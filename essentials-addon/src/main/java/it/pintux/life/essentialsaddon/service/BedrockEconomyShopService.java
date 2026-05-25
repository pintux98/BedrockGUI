package it.pintux.life.essentialsaddon.service;
import it.pintux.life.common.utils.IconResolver;
import it.pintux.life.essentialsaddon.api.BedrockPlayerDetector;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.config.EssentialsAddonConfiguration;
import it.pintux.life.essentialsaddon.model.EconomyShopCatalogEntry;
import it.pintux.life.essentialsaddon.model.ShopItemView;
import it.pintux.life.essentialsaddon.util.BedrockSoundFeedback;
import it.pintux.life.essentialsaddon.util.BukkitFormPlayer;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import it.pintux.life.essentialsaddon.util.ShopGuiActionPayloads;
import me.gypopo.economyshopgui.api.EconomyShopGUIHook;
import me.gypopo.economyshopgui.api.events.PostTransactionEvent;
import me.gypopo.economyshopgui.api.events.PreTransactionEvent;
import me.gypopo.economyshopgui.api.objects.BuyPrice;
import me.gypopo.economyshopgui.api.objects.SellPrice;
import me.gypopo.economyshopgui.objects.ShopItem;
import me.gypopo.economyshopgui.providers.EconomyProvider;
import me.gypopo.economyshopgui.util.EcoType;
import me.gypopo.economyshopgui.util.Transaction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class BedrockEconomyShopService {
    private final Logger logger;
    private final EssentialsAddonConfiguration configuration;
    private final EconomyShopCatalogService catalogService;
    private final BedrockPlayerDetector bedrockPlayerDetector;
    private final BedrockSoundFeedback soundFeedback;
    private final Map<UUID, NavigationState> navigationState = new ConcurrentHashMap<>();

    public BedrockEconomyShopService(Logger logger, EssentialsAddonConfiguration configuration,
                                      EconomyShopCatalogService catalogService,
                                      BedrockPlayerDetector bedrockPlayerDetector,
                                      BedrockSoundFeedback soundFeedback) {
        this.logger = logger;
        this.configuration = configuration;
        this.catalogService = catalogService;
        this.bedrockPlayerDetector = bedrockPlayerDetector;
        this.soundFeedback = soundFeedback;
    }

    public boolean shouldHandle(Player player) {
        return player != null && bedrockPlayerDetector.isBedrockPlayer(player) && catalogService.isAvailable();
    }

    public boolean isAvailable() {
        return catalogService.isAvailable();
    }

    public boolean looksLikeInventory(String title, Object holder) {
        String holderName = holder == null ? "" : holder.getClass().getName().toLowerCase(Locale.ROOT);
        return holderName.contains("economyshopgui") || catalogService.resolveSectionByInventoryTitle(title).isPresent();
    }

    public void openFromInventoryTitle(Player player, String title) {
        Optional<String> resolvedSection = catalogService.resolveSectionByInventoryTitle(title);
        if (resolvedSection.isPresent()) {
            openShop(player, resolvedSection.get(), 1);
            return;
        }
        NavigationState state = navigationState.get(player.getUniqueId());
        if (state != null) {
            openShop(player, state.sectionId(), state.page());
            return;
        }
        openMainMenu(player);
    }

    public void openMainMenu(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureCatalog(player)) {
            return;
        }

        Collection<EconomyShopCatalogEntry> shops = catalogService.getAccessibleShops(player);
        if (shops.isEmpty()) {
            player.sendMessage(configuration.shopEmptyShopMessage());
            return;
        }

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(configuration.shopMainTitle());
        form.content(configuration.shopMainContent());
        for (EconomyShopCatalogEntry entry : shops) {
            form.button(entry.getDisplayName() + "\n" + ChatColor.GRAY + entry.getId(), formPlayer ->
                    api.executeActionString(
                            formPlayer,
                            "economyshop_shop:" + ShopGuiActionPayloads.encodeShop(entry.getId(), 1),
                            context("economyshop-main", Map.of("shopId", entry.getId()))
                    ));
        }
        form.send(new BukkitFormPlayer(player));
        soundFeedback.playFormOpen(player);
    }

    public void openShop(Player player, String sectionId, int requestedPage) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureCatalog(player)) {
            return;
        }
        Optional<EconomyShopCatalogEntry> optionalEntry = catalogService.getShop(sectionId);
        if (optionalEntry.isEmpty()) {
            player.sendMessage(configuration.shopNoShopAccess());
            return;
        }
        EconomyShopCatalogEntry entry = optionalEntry.get();
        if (!catalogService.hasShopAccess(player, entry)) {
            player.sendMessage(configuration.shopNoShopAccess());
            return;
        }

        List<Integer> pages = catalogService.getAccessiblePages(player, sectionId);
        int page = normalizePage(requestedPage, pages);
        List<ShopItemView> items = catalogService.getAccessibleItems(player, sectionId, page);
        navigationState.put(player.getUniqueId(), new NavigationState(sectionId, page));

        Map<String, String> replacements = new HashMap<>();
        replacements.put("shop_name", entry.getDisplayName());
        replacements.put("page", Integer.toString(page));
        replacements.put("max_page", Integer.toString(pages.get(pages.size() - 1)));
        replacements.put("economy", "EconomyShopGUI");

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(configuration.render(configuration.shopShopTitle(), replacements));
        form.content(configuration.render(configuration.shopShopContent(), replacements));

        if (items.isEmpty()) {
            form.button(configuration.shopEmptyPageMessage(), ignored -> { });
        } else {
            for (ShopItemView itemView : items) {
                String buttonText = itemView.getDisplayName() + "\n" + priceLine(player, sectionId, itemView.getId());
                form.button(buttonText, itemView.getMaterial(), formPlayer ->
                        api.executeActionString(
                                formPlayer,
                                "economyshop_item:" + ShopGuiActionPayloads.encodeItem(sectionId, itemView.getId(), page),
                                context("economyshop-shop", Map.of("shopId", sectionId, "itemId", itemView.getId()))
                        ));
            }
        }

        Integer previousPage = previousPage(page, pages);
        Integer nextPage = nextPage(page, pages);
        if (previousPage != null) {
            form.button(configuration.previousButton(), formPlayer ->
                    api.executeActionString(
                            formPlayer,
                            "economyshop_shop:" + ShopGuiActionPayloads.encodeShop(sectionId, previousPage),
                            context("economyshop-prev", Map.of("shopId", sectionId))
                    ));
        }
        form.button(configuration.mainButton(), ignored -> openMainMenu(player));
        if (nextPage != null) {
            form.button(configuration.nextButton(), formPlayer ->
                    api.executeActionString(
                            formPlayer,
                            "economyshop_shop:" + ShopGuiActionPayloads.encodeShop(sectionId, nextPage),
                            context("economyshop-next", Map.of("shopId", sectionId))
                    ));
        }
        form.send(new BukkitFormPlayer(player));
        soundFeedback.playFormOpen(player);
    }

    public void openItemMenu(Player player, String sectionId, String itemId, int page) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureCatalog(player)) {
            return;
        }
        Optional<ShopItemView> optionalItemView = catalogService.getItemView(sectionId, itemId);
        Optional<ShopItem> optionalLiveItem = catalogService.getLiveItem(sectionId, itemId);
        if (optionalItemView.isEmpty() || optionalLiveItem.isEmpty()) {
            player.sendMessage(configuration.shopUnavailableItemMessage());
            return;
        }

        ShopItemView itemView = optionalItemView.get();
        ShopItem liveItem = optionalLiveItem.get();

        if (itemView.getLinkedShopId() != null && !itemView.getLinkedShopId().isBlank()) {
            openShop(player, itemView.getLinkedShopId(), 1);
            return;
        }

        Optional<BuyPrice> buyPrice = catalogService.resolveBuyPrice(player, liveItem, 1);
        Optional<SellPrice> sellPrice = catalogService.resolveSellPrice(player, liveItem, 1);

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(configuration.render(configuration.shopItemTitle(), Map.of("item_name", itemView.getDisplayName())));
        form.content(configuration.render(configuration.shopItemContent(), Map.of(
                "item_name", itemView.getDisplayName(),
                "item_type", itemView.getType(),
                "material", itemView.getMaterial(),
                "buy_price", formatPrices(buyPrice.map(BuyPrice::getPrices).orElse(Map.of())),
                "sell_price", formatPrices(sellPrice.map(SellPrice::getPrices).orElse(Map.of())),
                "description", itemView.getDescription()
        )));

        boolean hasAction = false;

        if (buyPrice.isPresent()) {
            hasAction = true;
            for (Integer amount : configuration.shopAmountPresets()) {
                Optional<BuyPrice> amountPrice = catalogService.resolveBuyPrice(player, liveItem, amount);
                if (amountPrice.isEmpty()) {
                    continue;
                }
                String priceText = formatPrices(amountPrice.get().getPrices());
                form.button(configuration.shopBuyLabel() + " x" + amount + "\n" + ChatColor.GREEN + priceText,
                        formPlayer -> handleTransactionClick(formPlayer, sectionId, itemId, amount, page,
                                BedrockShopAction.BUY, configuration.shopBuyLabel(), priceText));
            }
        }

        if (sellPrice.isPresent()) {
            hasAction = true;
            for (Integer amount : configuration.shopAmountPresets()) {
                Optional<SellPrice> amountPrice = catalogService.resolveSellPrice(player, liveItem, amount);
                if (amountPrice.isEmpty()) {
                    continue;
                }
                String priceText = formatPrices(amountPrice.get().getPrices());
                form.button(configuration.shopSellLabel() + " x" + amount + "\n" + ChatColor.RED + priceText,
                        formPlayer -> handleTransactionClick(formPlayer, sectionId, itemId, amount, page,
                                BedrockShopAction.SELL, configuration.shopSellLabel(), priceText));
            }
        }

        if (!hasAction) {
            form.button(configuration.shopDecorationLabel(), ignored -> player.sendMessage(configuration.shopUnavailableItemMessage()));
        }
        form.button(configuration.backButton(), ignored -> openShop(player, sectionId, page));
        form.send(new BukkitFormPlayer(player));
        soundFeedback.playFormOpen(player);
    }

    public TransactionResult executeTransaction(Player player, BedrockShopAction action,
                                                String sectionId, String itemId, int amount, int page) {
        if (!ensureCatalog(player)) {
            return TransactionResult.failure(configuration.shopShopsNotReady());
        }
        Optional<ShopItem> optionalLiveItem = catalogService.getLiveItem(sectionId, itemId);
        if (optionalLiveItem.isEmpty()) {
            return TransactionResult.failure(configuration.shopUnavailableItemMessage());
        }

        TransactionResult result = switch (action) {
            case BUY, TRADE -> buy(player, optionalLiveItem.get(), Math.max(1, amount));
            case SELL -> sell(player, optionalLiveItem.get(), Math.max(1, amount));
            case LINK -> TransactionResult.failure("Linked shop actions are handled via the item menu");
        };

        if (result.success()) {
            player.sendMessage(configuration.shopTransactionSuccess());
            soundFeedback.playPurchaseSuccess(player);
            openShop(player, sectionId, page);
        } else {
            player.sendMessage(configuration.shopTransactionFailed().replace("%reason%", result.message()));
            soundFeedback.playPurchaseFailed(player);
        }
        return result;
    }

    private TransactionResult buy(Player player, ShopItem shopItem, int amount) {
        Optional<BuyPrice> optionalPrice = catalogService.resolveBuyPrice(player, shopItem, amount);
        if (optionalPrice.isEmpty()) {
            return TransactionResult.failure("Buy requirements are not met");
        }
        BuyPrice buyPrice = optionalPrice.get();
        ItemStack baseItem = baseItem(shopItem);
        if (baseItem == null) {
            return TransactionResult.failure("Missing target item");
        }

        PreTransactionEvent preEvent = createPreEvent(shopItem, player, amount, buyPrice.getPrices(), Transaction.Type.BUY_SCREEN);
        Bukkit.getPluginManager().callEvent(preEvent);
        if (preEvent.isCancelled()) {
            return TransactionResult.failure("Transaction cancelled");
        }

        Map<EcoType, Double> prices = effectivePrices(preEvent, buyPrice.getPrices());
        if (!hasFunds(player, prices)) {
            return TransactionResult.failure(Transaction.Result.INSUFFICIENT_FUNDS.name());
        }
        if (!canFit(player.getInventory(), baseItem, amount, shopItem.getStackSize())) {
            return TransactionResult.failure(Transaction.Result.NO_INVENTORY_SPACE.name());
        }

        for (Map.Entry<EcoType, Double> entry : prices.entrySet()) {
            EconomyProvider provider = EconomyShopGUIHook.getEcon(entry.getKey());
            if (provider == null) {
                return TransactionResult.failure("Unsupported economy: " + entry.getKey());
            }
            provider.withdrawBalance(player, entry.getValue());
        }

        player.getInventory().addItem(splitStacks(baseItem, amount, shopItem.getStackSize()));
        buyPrice.updateLimits();
        Bukkit.getPluginManager().callEvent(createPostEvent(shopItem, player, amount, prices, Transaction.Type.BUY_SCREEN, Transaction.Result.SUCCESS));
        return TransactionResult.success("Bought item");
    }

    private TransactionResult sell(Player player, ShopItem shopItem, int amount) {
        Optional<SellPrice> optionalPrice = catalogService.resolveSellPrice(player, shopItem, amount);
        if (optionalPrice.isEmpty()) {
            return TransactionResult.failure("Sell requirements are not met");
        }
        SellPrice sellPrice = optionalPrice.get();
        ItemStack baseItem = baseItem(shopItem);
        if (baseItem == null) {
            return TransactionResult.failure("Missing source item");
        }

        PreTransactionEvent preEvent = createPreEvent(shopItem, player, amount, sellPrice.getPrices(), Transaction.Type.SELL_SCREEN);
        Bukkit.getPluginManager().callEvent(preEvent);
        if (preEvent.isCancelled()) {
            return TransactionResult.failure("Transaction cancelled");
        }

        if (!containsAtLeast(player.getInventory(), baseItem, amount)) {
            return TransactionResult.failure(Transaction.Result.NOT_ENOUGH_ITEMS.name());
        }
        if (!removeItems(player.getInventory(), baseItem, amount)) {
            return TransactionResult.failure(Transaction.Result.NOT_ENOUGH_ITEMS.name());
        }

        Map<EcoType, Double> prices = effectivePrices(preEvent, sellPrice.getPrices());
        for (Map.Entry<EcoType, Double> entry : prices.entrySet()) {
            EconomyProvider provider = EconomyShopGUIHook.getEcon(entry.getKey());
            if (provider == null) {
                return TransactionResult.failure("Unsupported economy: " + entry.getKey());
            }
            provider.depositBalance(player, entry.getValue());
        }

        sellPrice.updateLimits();
        Bukkit.getPluginManager().callEvent(createPostEvent(shopItem, player, amount, prices, Transaction.Type.SELL_SCREEN, Transaction.Result.SUCCESS));
        return TransactionResult.success("Sold item");
    }

    private boolean hasFunds(Player player, Map<EcoType, Double> prices) {
        for (Map.Entry<EcoType, Double> entry : prices.entrySet()) {
            EconomyProvider provider = EconomyShopGUIHook.getEcon(entry.getKey());
            if (provider == null || provider.getBalance(player) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private boolean canFit(Inventory inventory, ItemStack baseItem, int amount, int configuredStackSize) {
        int maxStack = Math.max(1, Math.min(64, configuredStackSize > 0 ? configuredStackSize : baseItem.getMaxStackSize()));
        int remaining = amount;
        for (ItemStack content : inventory.getStorageContents()) {
            if (remaining <= 0) {
                return true;
            }
            if (content == null || content.getType().isAir()) {
                remaining -= maxStack;
                continue;
            }
            if (content.isSimilar(baseItem)) {
                remaining -= Math.max(0, maxStack - content.getAmount());
            }
        }
        return remaining <= 0;
    }

    private ItemStack[] splitStacks(ItemStack baseItem, int amount, int configuredStackSize) {
        List<ItemStack> stacks = new ArrayList<>();
        int maxStack = Math.max(1, Math.min(64, configuredStackSize > 0 ? configuredStackSize : baseItem.getMaxStackSize()));
        int remaining = amount;
        while (remaining > 0) {
            ItemStack clone = baseItem.clone();
            int giveAmount = Math.min(maxStack, remaining);
            clone.setAmount(giveAmount);
            stacks.add(clone);
            remaining -= giveAmount;
        }
        return stacks.toArray(ItemStack[]::new);
    }

    private boolean containsAtLeast(Inventory inventory, ItemStack baseItem, int amount) {
        int remaining = amount;
        for (ItemStack content : inventory.getStorageContents()) {
            if (content != null && content.isSimilar(baseItem)) {
                remaining -= content.getAmount();
                if (remaining <= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean removeItems(Inventory inventory, ItemStack baseItem, int amount) {
        int remaining = amount;
        ItemStack[] contents = inventory.getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack content = contents[i];
            if (content == null || !content.isSimilar(baseItem)) {
                continue;
            }
            int remove = Math.min(remaining, content.getAmount());
            content.setAmount(content.getAmount() - remove);
            if (content.getAmount() <= 0) {
                contents[i] = null;
            }
            remaining -= remove;
            if (remaining <= 0) {
                inventory.setStorageContents(contents);
                return true;
            }
        }
        inventory.setStorageContents(contents);
        return false;
    }

    private ItemStack baseItem(ShopItem shopItem) {
        ItemStack item = shopItem.getItemToGive() != null ? shopItem.getItemToGive().clone() :
                (shopItem.getShopItem() != null ? shopItem.getShopItem().clone() : null);
        if (item != null) {
            item.setAmount(1);
        }
        return item;
    }

    private String priceLine(Player player, String sectionId, String itemId) {
        Optional<ShopItem> optionalItem = catalogService.getLiveItem(sectionId, itemId);
        if (optionalItem.isEmpty()) {
            return configuration.shopUnavailableItemMessage();
        }
        String buy = formatPrices(catalogService.resolveBuyPrice(player, optionalItem.get(), 1).map(BuyPrice::getPrices).orElse(Map.of()));
        String sell = formatPrices(catalogService.resolveSellPrice(player, optionalItem.get(), 1).map(SellPrice::getPrices).orElse(Map.of()));
        return ChatColor.GREEN + "Buy " + buy + ChatColor.GRAY + " / " + ChatColor.RED + "Sell " + sell;
    }

    private String formatPrices(Map<EcoType, Double> prices) {
        if (prices == null || prices.isEmpty()) {
            return "N/A";
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<EcoType, Double> entry : prices.entrySet()) {
            EconomyProvider provider = EconomyShopGUIHook.getEcon(entry.getKey());
            String value = provider != null ? provider.formatPrice(entry.getValue()) : String.format(Locale.US, "%.2f", entry.getValue());
            parts.add(value);
        }
        return String.join(", ", parts);
    }

    private PreTransactionEvent createPreEvent(ShopItem shopItem, Player player, int amount, Map<EcoType, Double> prices, Transaction.Type type) {
        if (prices.size() == 1) {
            return new PreTransactionEvent(shopItem, player, amount, prices.values().iterator().next(), type);
        }
        return new PreTransactionEvent(Map.of(shopItem, amount), new HashMap<>(prices), player, amount, type);
    }

    private PostTransactionEvent createPostEvent(ShopItem shopItem, Player player, int amount, Map<EcoType, Double> prices,
                                                 Transaction.Type type, Transaction.Result result) {
        if (prices.size() == 1) {
            return new PostTransactionEvent(shopItem, player, amount, prices.values().iterator().next(), type, result);
        }
        return new PostTransactionEvent(Map.of(shopItem, amount), new HashMap<>(prices), player, amount, type, result);
    }

    private Map<EcoType, Double> effectivePrices(PreTransactionEvent event, Map<EcoType, Double> originalPrices) {
        if (originalPrices.size() == 1 && !event.getPrices().isEmpty()) {
            return new HashMap<>(event.getPrices());
        }
        if (originalPrices.size() == 1) {
            EcoType type = originalPrices.keySet().iterator().next();
            return Map.of(type, event.getPrice());
        }
        return new HashMap<>(event.getPrices());
    }

    private BedrockGUIApi requireApi(Player player) {
        BedrockGUIApi api = BedrockGUIApi.getInstance();
        if (api == null) {
            player.sendMessage(configuration.noBedrockGui());
        }
        return api;
    }

    private boolean ensureCatalog(Player player) {
        if (!catalogService.isReady()) {
            catalogService.refreshCatalog();
        }
        if (!catalogService.isReady()) {
            player.sendMessage(configuration.shopShopsNotReady());
            return false;
        }
        return true;
    }

    private ActionSystem.ActionContext context(String source, Map<String, Object> metadata) {
        ActionSystem.ActionContext.Builder builder = ActionSystem.ActionContext.builder().menuName(source).formType("bedrock-economyshop");
        metadata.forEach(builder::metadata);
        return builder.build();
    }

    private int normalizePage(int requestedPage, List<Integer> pages) {
        if (pages.isEmpty()) {
            return 1;
        }
        if (pages.contains(requestedPage)) {
            return requestedPage;
        }
        return pages.get(0);
    }

    private Integer previousPage(int page, List<Integer> pages) {
        Integer previous = null;
        for (Integer value : pages) {
            if (value >= page) {
                break;
            }
            previous = value;
        }
        return previous;
    }

    private Integer nextPage(int page, List<Integer> pages) {
        for (Integer value : pages) {
            if (value > page) {
                return value;
            }
        }
        return null;
    }

    public record TransactionResult(boolean success, String message) {
        public static TransactionResult success(String message) {
            return new TransactionResult(true, message);
        }

        public static TransactionResult failure(String message) {
            return new TransactionResult(false, message);
        }
    }

    private record NavigationState(String sectionId, int page) { }

    private void handleTransactionClick(FormPlayer formPlayer, String sectionId, String itemId, int amount, int page,
                                         BedrockShopAction action, String actionLabel, String priceText) {
        if (configuration.shopRequirePurchaseConfirmation()) {
            showTransactionConfirmation(formPlayer, sectionId, itemId, amount, page, action, actionLabel, priceText);
        } else {
            Player bukkitPlayer = FormPlayerResolver.resolve(formPlayer);
            if (bukkitPlayer == null) return;
            executeTransaction(bukkitPlayer, action, sectionId, itemId, amount, page);
        }
    }

    private void showTransactionConfirmation(FormPlayer formPlayer, String sectionId, String itemId, int amount, int page,
                                              BedrockShopAction action, String actionLabel, String priceText) {
        BedrockGUIApi api = BedrockGUIApi.getInstance();
        if (api == null) return;
        Player bukkitPlayer = FormPlayerResolver.resolve(formPlayer);
        if (bukkitPlayer == null) return;

        String confirmTitle = configuration.render(configuration.shopItemTitle(), Map.of("item_name", actionLabel + " x" + amount));
        String confirmContent = ChatColor.YELLOW + "Confirm " + actionLabel + " x" + amount + "\n" +
                ChatColor.GREEN + "Price: " + priceText + "\n\n" +
                ChatColor.GRAY + "Select Confirm to proceed or Cancel to return.";

        api.createModalForm(confirmTitle)
                .button1(ChatColor.GREEN + "Confirm", confirmedPlayer ->
                        api.executeActionString(confirmedPlayer,
                                "economyshop_transaction:" + ShopGuiActionPayloads.encodeTransaction(action, sectionId, itemId, amount, page),
                                context("economyshop-confirm", Map.of("shopId", sectionId, "itemId", itemId))))
                .button2(ChatColor.RED + "Cancel", cancelledPlayer -> openItemMenu(bukkitPlayer, sectionId, itemId, page))
                .content(confirmContent)
                .send(formPlayer);
    }
}
