package it.pintux.life.essentialsaddon.service;
import it.pintux.life.essentialsaddon.api.BedrockPlayerDetector;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.config.EssentialsAddonConfiguration;
import it.pintux.life.essentialsaddon.model.ShopCatalogEntry;
import it.pintux.life.essentialsaddon.model.ShopItemView;
import it.pintux.life.essentialsaddon.util.BedrockIconResolver;
import it.pintux.life.essentialsaddon.util.BedrockSoundFeedback;
import it.pintux.life.essentialsaddon.util.BukkitFormPlayer;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import it.pintux.life.essentialsaddon.util.ShopGuiActionPayloads;
import net.brcdev.shopgui.shop.item.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public final class BedrockShopGuiService {
    private final Logger logger;
    private final EssentialsAddonConfiguration configuration;
    private final ShopGuiCatalogService catalogService;
    private final BedrockPlayerDetector bedrockPlayerDetector;
    private final ShopGuiTransactionGateway transactionGateway;
    private final BedrockSoundFeedback soundFeedback;
    private final DecimalFormat priceFormat = new DecimalFormat("0.00");

    public BedrockShopGuiService(Logger logger, EssentialsAddonConfiguration configuration, ShopGuiCatalogService catalogService,
                                  BedrockPlayerDetector bedrockPlayerDetector, ShopGuiTransactionGateway transactionGateway,
                                  BedrockSoundFeedback soundFeedback) {
        this.logger = logger;
        this.configuration = configuration;
        this.catalogService = catalogService;
        this.bedrockPlayerDetector = bedrockPlayerDetector;
        this.transactionGateway = transactionGateway;
        this.soundFeedback = soundFeedback;
    }

    public boolean shouldHandle(Player player) {
        return player != null && bedrockPlayerDetector.isBedrockPlayer(player);
    }

    public boolean looksLikeShopGuiInventory(String title, Object holder) {
        String holderName = holder == null ? "" : holder.getClass().getName();
        return holderName.startsWith("net.brcdev.shopgui") || catalogService.resolveByInventoryTitle(title).isPresent();
    }

    public void openFromInventoryTitle(Player player, String title) {
        Optional<ShopGuiCatalogService.ResolvedTitle> resolvedTitle = catalogService.resolveByInventoryTitle(title);
        if (resolvedTitle.isPresent()) {
            openShop(player, resolvedTitle.get().shopId(), resolvedTitle.get().page());
        } else {
            openMainMenu(player);
        }
    }

    public void openMainMenu(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureCatalog(player)) {
            return;
        }

        Collection<ShopCatalogEntry> shops = catalogService.getAccessibleShops(player);
        if (shops.isEmpty()) {
            player.sendMessage(configuration.shopEmptyShopMessage());
            return;
        }

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(configuration.shopMainTitle());
        form.content(configuration.shopMainContent());
        for (ShopCatalogEntry entry : shops) {
            String buttonText = entry.getDisplayName() + "\n" + ChatColor.GRAY + entry.getId();
            form.button(buttonText, formPlayer -> api.executeActionString(
                    formPlayer,
                    "shopgui_shop:" + ShopGuiActionPayloads.encodeShop(entry.getId(), 1),
                    context("shopgui-main", Map.of("shopId", entry.getId()))
            ));
        }
        form.send(new BukkitFormPlayer(player));
        soundFeedback.playFormOpen(player);
    }

    public void openShop(Player player, String shopId, int requestedPage) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureCatalog(player)) {
            return;
        }
        Optional<ShopCatalogEntry> optionalEntry = catalogService.getShop(shopId);
        if (optionalEntry.isEmpty()) {
            player.sendMessage(configuration.shopNoShopAccess());
            return;
        }
        ShopCatalogEntry entry = optionalEntry.get();
        if (!catalogService.hasShopAccess(player, entry)) {
            player.sendMessage(configuration.shopNoShopAccess());
            return;
        }

        List<Integer> pages = catalogService.getAccessiblePages(player, shopId);
        int page = normalizePage(requestedPage, pages);
        List<ShopItemView> items = catalogService.getAccessibleItems(player, shopId, page);

        Map<String, String> replacements = new HashMap<>();
        replacements.put("shop_name", entry.getShop().getName(page));
        replacements.put("page", Integer.toString(page));
        replacements.put("max_page", Integer.toString(pages.get(pages.size() - 1)));
        replacements.put("economy", entry.getShop().getEconomyType().name());

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(configuration.render(configuration.shopShopTitle(), replacements));
        form.content(configuration.render(configuration.shopShopContent(), replacements));

        if (items.isEmpty()) {
            form.button(configuration.shopEmptyPageMessage(), ignored -> { });
        } else {
            for (ShopItemView itemView : items) {
                String buttonText = itemView.getDisplayName() + "\n" + ChatColor.GREEN + priceLine(player, shopId, itemView.getId());
                String icon = BedrockIconResolver.resolveTexturePath(itemView.getMaterial());
                if (icon != null) {
                    form.button(buttonText, icon, formPlayer -> api.executeActionString(
                            formPlayer,
                            "shopgui_item:" + ShopGuiActionPayloads.encodeItem(shopId, itemView.getId(), page),
                            context("shopgui-shop", Map.of("shopId", shopId, "itemId", itemView.getId()))
                    ));
                } else {
                    form.button(buttonText, formPlayer -> api.executeActionString(
                            formPlayer,
                            "shopgui_item:" + ShopGuiActionPayloads.encodeItem(shopId, itemView.getId(), page),
                            context("shopgui-shop", Map.of("shopId", shopId, "itemId", itemView.getId()))
                    ));
                }
            }
        }

        Integer previousPage = previousPage(page, pages);
        Integer nextPage = nextPage(page, pages);
        if (previousPage != null) {
            form.button(configuration.previousButton(), formPlayer -> api.executeActionString(
                    formPlayer,
                    "shopgui_shop:" + ShopGuiActionPayloads.encodeShop(shopId, previousPage),
                    context("shopgui-shop-prev", Map.of("shopId", shopId))
            ));
        }
        form.button(configuration.mainButton(), ignored -> openMainMenu(player));
        if (nextPage != null) {
            form.button(configuration.nextButton(), formPlayer -> api.executeActionString(
                    formPlayer,
                    "shopgui_shop:" + ShopGuiActionPayloads.encodeShop(shopId, nextPage),
                    context("shopgui-shop-next", Map.of("shopId", shopId))
            ));
        }

        form.send(new BukkitFormPlayer(player));
        soundFeedback.playFormOpen(player);
    }

    public void openItemMenu(Player player, String shopId, String itemId, int page) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureCatalog(player)) {
            return;
        }

        Optional<ShopItemView> optionalItemView = catalogService.getItemView(shopId, itemId);
        Optional<ShopItem> optionalLiveItem = catalogService.getLiveItem(shopId, itemId);
        if (optionalItemView.isEmpty() || optionalLiveItem.isEmpty()) {
            player.sendMessage(configuration.shopUnavailableItemMessage());
            return;
        }

        ShopItemView itemView = optionalItemView.get();
        ShopItem liveItem = optionalLiveItem.get();
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(configuration.render(configuration.shopItemTitle(), Map.of("item_name", itemView.getDisplayName())));
        form.content(configuration.render(configuration.shopItemContent(), Map.of(
                "item_name", itemView.getDisplayName(),
                "item_type", itemView.getType(),
                "material", itemView.getMaterial(),
                "buy_price", formatPrice(liveItem.getBuyPriceForAmount(player, 1)),
                "sell_price", formatPrice(liveItem.getSellPriceForAmount(player, 1)),
                "description", itemView.getDescription()
        )));

        boolean hasAction = false;
        if (itemView.getLinkedShopId() != null && !itemView.getLinkedShopId().isBlank()) {
            hasAction = true;
            form.button(configuration.shopOpenLinkedButton(), formPlayer -> api.executeActionString(
                    formPlayer,
                    "shopgui_shop:" + ShopGuiActionPayloads.encodeShop(itemView.getLinkedShopId(), 1),
                    context("shopgui-link", Map.of("shopId", itemView.getLinkedShopId()))
            ));
        }

        if (liveItem.getBuyPriceForAmount(player, 1) >= 0) {
            hasAction = true;
            String label = usesTradeLabel(itemView) ? configuration.shopTradeLabel() : configuration.shopBuyLabel();
            for (Integer amount : configuration.shopAmountPresets()) {
                double price = liveItem.getBuyPriceForAmount(player, amount);
                if (price < 0) {
                    continue;
                }
                BedrockShopAction buyAction = usesTradeLabel(itemView) ? BedrockShopAction.TRADE : BedrockShopAction.BUY;
                form.button(label + " x" + amount + "\n" + ChatColor.GREEN + formatPrice(price),
                        formPlayer -> handleTransactionClick(formPlayer, shopId, itemId, amount, page, buyAction, label, price));
            }
        }

        if (liveItem.getSellPriceForAmount(player, 1) >= 0) {
            hasAction = true;
            for (Integer amount : configuration.shopAmountPresets()) {
                double price = liveItem.getSellPriceForAmount(player, amount);
                if (price < 0) {
                    continue;
                }
                form.button(configuration.shopSellLabel() + " x" + amount + "\n" + ChatColor.RED + formatPrice(price),
                        formPlayer -> handleTransactionClick(formPlayer, shopId, itemId, amount, page,
                                BedrockShopAction.SELL, configuration.shopSellLabel(), price));
            }
        }

        if (!hasAction) {
            form.button(configuration.shopDecorationLabel(), ignored -> player.sendMessage(configuration.shopUnavailableItemMessage()));
        }
        form.button(configuration.backButton(), ignored -> openShop(player, shopId, page));
        form.send(new BukkitFormPlayer(player));
        soundFeedback.playFormOpen(player);
    }

    public ShopGuiTransactionGateway.TransactionExecutionResult executeTransaction(Player player, BedrockShopAction action,
                                                                                   String shopId, String itemId, int amount, int page) {
        if (!ensureCatalog(player)) {
            return ShopGuiTransactionGateway.TransactionExecutionResult.failure(configuration.shopShopsNotReady());
        }
        Optional<ShopItem> optionalLiveItem = catalogService.getLiveItem(shopId, itemId);
        if (optionalLiveItem.isEmpty()) {
            player.sendMessage(configuration.shopUnavailableItemMessage());
            return ShopGuiTransactionGateway.TransactionExecutionResult.failure("Missing ShopGUI+ item");
        }

        ShopItem liveItem = optionalLiveItem.get();
        if (action == BedrockShopAction.BUY || action == BedrockShopAction.TRADE) {
            double price = liveItem.getBuyPriceForAmount(player, Math.max(1, amount));
            if (price > 0 && !hasBalance(player, price)) {
                player.sendMessage(configuration.shopTransactionFailed().replace("%reason%", "Insufficient balance"));
                soundFeedback.playPurchaseFailed(player);
                return ShopGuiTransactionGateway.TransactionExecutionResult.failure("Insufficient balance");
            }
        }
        ShopGuiTransactionGateway.TransactionExecutionResult result = transactionGateway.execute(player, liveItem, action, Math.max(1, amount));
        if (result.success()) {
            player.sendMessage(configuration.shopTransactionSuccess());
            soundFeedback.playPurchaseSuccess(player);
            openShop(player, shopId, page);
        } else {
            String reason = result.message() == null || result.message().isBlank() ? configuration.shopUnsupportedTransaction() : result.message();
            player.sendMessage(configuration.shopTransactionFailed().replace("%reason%", reason));
            soundFeedback.playPurchaseFailed(player);
        }
        return result;
    }

    private boolean usesTradeLabel(ShopItemView itemView) {
        return !"ITEM".equalsIgnoreCase(itemView.getType());
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

    private String priceLine(Player player, String shopId, String itemId) {
        Optional<ShopItem> optionalLiveItem = catalogService.getLiveItem(shopId, itemId);
        if (optionalLiveItem.isEmpty()) {
            return configuration.shopUnavailableItemMessage();
        }
        ShopItem item = optionalLiveItem.get();
        double buy = item.getBuyPriceForAmount(player, 1);
        double sell = item.getSellPriceForAmount(player, 1);
        return "Buy " + formatPrice(buy) + " / Sell " + formatPrice(sell);
    }

    private String formatPrice(double price) {
        if (price < 0) {
            return "N/A";
        }
        return priceFormat.format(price);
    }

    private ActionSystem.ActionContext context(String source, Map<String, Object> metadata) {
        ActionSystem.ActionContext.Builder builder = ActionSystem.ActionContext.builder().menuName(source).formType("bedrock-shopgui");
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

    private boolean hasBalance(Player player, double required) {
        try {
            Class<?> vaultClass = Class.forName("net.milkbowl.vault.economy.Economy");
            Object vault = Bukkit.getServicesManager().getRegistration(vaultClass).getProvider();
            Object economy = vault;
            java.lang.reflect.Method getBalance = economy.getClass().getMethod("getBalance", org.bukkit.OfflinePlayer.class);
            double balance = ((Number) getBalance.invoke(economy, player)).doubleValue();
            return balance >= required;
        } catch (Exception e) {
            return true;
        }
    }

    private void handleTransactionClick(FormPlayer formPlayer, String shopId, String itemId, int amount, int page,
                                         BedrockShopAction action, String actionLabel, double price) {
        if (configuration.shopRequirePurchaseConfirmation()) {
            showTransactionConfirmation(formPlayer, shopId, itemId, amount, page, action, actionLabel, price);
        } else {
            Player bukkitPlayer = FormPlayerResolver.resolve(formPlayer);
            if (bukkitPlayer == null) return;
            executeTransaction(bukkitPlayer, action, shopId, itemId, amount, page);
        }
    }

    private void showTransactionConfirmation(FormPlayer formPlayer, String shopId, String itemId, int amount, int page,
                                              BedrockShopAction action, String actionLabel, double price) {
        BedrockGUIApi api = BedrockGUIApi.getInstance();
        if (api == null) return;
        Player bukkitPlayer = FormPlayerResolver.resolve(formPlayer);
        if (bukkitPlayer == null) return;

        String confirmTitle = configuration.render(configuration.shopItemTitle(), Map.of("item_name", actionLabel + " x" + amount));
        String confirmContent = ChatColor.YELLOW + "Confirm " + actionLabel + " x" + amount + "\n" +
                ChatColor.GREEN + "Price: " + formatPrice(price) + "\n\n" +
                ChatColor.GRAY + "Select Confirm to proceed or Cancel to return.";

        api.createModalForm(confirmTitle)
                .button1(ChatColor.GREEN + "Confirm", confirmedPlayer ->
                        api.executeActionString(confirmedPlayer,
                                "shopgui_transaction:" + ShopGuiActionPayloads.encodeTransaction(action, shopId, itemId, amount, page),
                                context("shopgui-confirm", Map.of("shopId", shopId, "itemId", itemId))))
                .button2(ChatColor.RED + "Cancel", cancelledPlayer -> openItemMenu(bukkitPlayer, shopId, itemId, page))
                .content(confirmContent)
                .send(formPlayer);
    }
}
