package it.pintux.life.shopguiaddon.service;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.shopguiaddon.config.ShopGuiAddonConfiguration;
import it.pintux.life.shopguiaddon.model.ShopCatalogEntry;
import it.pintux.life.shopguiaddon.model.ShopItemView;
import it.pintux.life.shopguiaddon.util.BukkitFormPlayer;
import it.pintux.life.shopguiaddon.util.ShopGuiActionPayloads;
import net.brcdev.shopgui.shop.item.ShopItem;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public final class BedrockShopGuiService {
    private final Logger logger;
    private final ShopGuiAddonConfiguration configuration;
    private final ShopGuiCatalogService catalogService;
    private final BedrockPlayerDetector bedrockPlayerDetector;
    private final ShopGuiTransactionGateway transactionGateway;
    private final DecimalFormat priceFormat = new DecimalFormat("0.00");

    public BedrockShopGuiService(Logger logger, ShopGuiAddonConfiguration configuration, ShopGuiCatalogService catalogService,
                                 BedrockPlayerDetector bedrockPlayerDetector, ShopGuiTransactionGateway transactionGateway) {
        this.logger = logger;
        this.configuration = configuration;
        this.catalogService = catalogService;
        this.bedrockPlayerDetector = bedrockPlayerDetector;
        this.transactionGateway = transactionGateway;
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

        long startedAt = System.nanoTime();
        Collection<ShopCatalogEntry> shops = catalogService.getAccessibleShops(player);
        if (shops.isEmpty()) {
            player.sendMessage(configuration.emptyShopMessage());
            return;
        }

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(configuration.mainTitle());
        form.content(configuration.mainContent());
        for (ShopCatalogEntry entry : shops) {
            String buttonText = entry.getDisplayName() + "\n" + ChatColor.GRAY + entry.getId();
            form.button(buttonText, formPlayer -> api.executeActionString(
                    formPlayer,
                    "shopgui_shop:" + ShopGuiActionPayloads.encodeShop(entry.getId(), 1),
                    context("shopgui-main", Map.of("shopId", entry.getId()))
            ));
        }
        form.send(new BukkitFormPlayer(player));
        warnIfSlow("openMainMenu", startedAt);
    }

    public void openShop(Player player, String shopId, int requestedPage) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureCatalog(player)) {
            return;
        }
        Optional<ShopCatalogEntry> optionalEntry = catalogService.getShop(shopId);
        if (optionalEntry.isEmpty()) {
            player.sendMessage(configuration.noShopAccess());
            return;
        }
        ShopCatalogEntry entry = optionalEntry.get();
        if (!catalogService.hasShopAccess(player, entry)) {
            player.sendMessage(configuration.noShopAccess());
            return;
        }

        long startedAt = System.nanoTime();
        List<Integer> pages = catalogService.getAccessiblePages(player, shopId);
        int page = normalizePage(requestedPage, pages);
        List<ShopItemView> items = catalogService.getAccessibleItems(player, shopId, page);

        Map<String, String> replacements = new HashMap<>();
        replacements.put("shop_name", entry.getShop().getName(page));
        replacements.put("page", Integer.toString(page));
        replacements.put("max_page", Integer.toString(pages.get(pages.size() - 1)));
        replacements.put("economy", entry.getShop().getEconomyType().name());

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(configuration.render(configuration.shopTitle(), replacements));
        form.content(configuration.render(configuration.shopContent(), replacements));

        if (items.isEmpty()) {
            form.button(configuration.emptyPageMessage(), ignored -> { });
        } else {
            for (ShopItemView itemView : items) {
                String buttonText = itemView.getDisplayName() + "\n" + ChatColor.GREEN + priceLine(player, shopId, itemView.getId());
                form.button(buttonText, formPlayer -> api.executeActionString(
                        formPlayer,
                        "shopgui_item:" + ShopGuiActionPayloads.encodeItem(shopId, itemView.getId(), page),
                        context("shopgui-shop", Map.of("shopId", shopId, "itemId", itemView.getId()))
                ));
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
        warnIfSlow("openShop", startedAt);
    }

    public void openItemMenu(Player player, String shopId, String itemId, int page) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureCatalog(player)) {
            return;
        }

        Optional<ShopItemView> optionalItemView = catalogService.getItemView(shopId, itemId);
        Optional<ShopItem> optionalLiveItem = catalogService.getLiveItem(shopId, itemId);
        if (optionalItemView.isEmpty() || optionalLiveItem.isEmpty()) {
            player.sendMessage(configuration.unavailableItemMessage());
            return;
        }

        ShopItemView itemView = optionalItemView.get();
        ShopItem liveItem = optionalLiveItem.get();
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(configuration.render(configuration.itemTitle(), Map.of("item_name", itemView.getDisplayName())));
        form.content(configuration.render(configuration.itemContent(), Map.of(
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
            form.button(configuration.openLinkedButton(), formPlayer -> api.executeActionString(
                    formPlayer,
                    "shopgui_shop:" + ShopGuiActionPayloads.encodeShop(itemView.getLinkedShopId(), 1),
                    context("shopgui-link", Map.of("shopId", itemView.getLinkedShopId()))
            ));
        }

        if (liveItem.getBuyPriceForAmount(player, 1) >= 0) {
            hasAction = true;
            String label = usesTradeLabel(itemView) ? configuration.tradeLabel() : configuration.buyLabel();
            for (Integer amount : configuration.amountPresets()) {
                double price = liveItem.getBuyPriceForAmount(player, amount);
                if (price < 0) {
                    continue;
                }
                form.button(label + " x" + amount + "\n" + ChatColor.GREEN + formatPrice(price), formPlayer -> api.executeActionString(
                        formPlayer,
                        "shopgui_transaction:" + ShopGuiActionPayloads.encodeTransaction(
                                usesTradeLabel(itemView) ? BedrockShopAction.TRADE : BedrockShopAction.BUY,
                                shopId,
                                itemId,
                                amount,
                                page
                        ),
                        context("shopgui-buy", Map.of("shopId", shopId, "itemId", itemId))
                ));
            }
        }

        if (liveItem.getSellPriceForAmount(player, 1) >= 0) {
            hasAction = true;
            for (Integer amount : configuration.amountPresets()) {
                double price = liveItem.getSellPriceForAmount(player, amount);
                if (price < 0) {
                    continue;
                }
                form.button(configuration.sellLabel() + " x" + amount + "\n" + ChatColor.RED + formatPrice(price), formPlayer -> api.executeActionString(
                        formPlayer,
                        "shopgui_transaction:" + ShopGuiActionPayloads.encodeTransaction(BedrockShopAction.SELL, shopId, itemId, amount, page),
                        context("shopgui-sell", Map.of("shopId", shopId, "itemId", itemId))
                ));
            }
        }

        if (!hasAction) {
            form.button(configuration.decorationLabel(), ignored -> player.sendMessage(configuration.unavailableItemMessage()));
        }
        form.button(configuration.backButton(), ignored -> openShop(player, shopId, page));
        form.send(new BukkitFormPlayer(player));
    }

    public ShopGuiTransactionGateway.TransactionExecutionResult executeTransaction(Player player, BedrockShopAction action,
                                                                                   String shopId, String itemId, int amount, int page) {
        if (!ensureCatalog(player)) {
            return ShopGuiTransactionGateway.TransactionExecutionResult.failure(configuration.shopsNotReady());
        }
        Optional<ShopItem> optionalLiveItem = catalogService.getLiveItem(shopId, itemId);
        if (optionalLiveItem.isEmpty()) {
            player.sendMessage(configuration.unavailableItemMessage());
            return ShopGuiTransactionGateway.TransactionExecutionResult.failure("Missing ShopGUI+ item");
        }

        long startedAt = System.nanoTime();
        ShopGuiTransactionGateway.TransactionExecutionResult result = transactionGateway.execute(player, optionalLiveItem.get(), action, Math.max(1, amount));
        if (result.success()) {
            player.sendMessage(configuration.transactionSuccess());
            openShop(player, shopId, page);
        } else {
            String reason = result.message() == null || result.message().isBlank() ? configuration.unsupportedTransaction() : result.message();
            player.sendMessage(configuration.transactionFailed().replace("%reason%", reason));
        }
        warnIfSlow("executeTransaction", startedAt);
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
            player.sendMessage(configuration.shopsNotReady());
            return false;
        }
        return true;
    }

    private String priceLine(Player player, String shopId, String itemId) {
        Optional<ShopItem> optionalLiveItem = catalogService.getLiveItem(shopId, itemId);
        if (optionalLiveItem.isEmpty()) {
            return configuration.unavailableItemMessage();
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

    private void warnIfSlow(String operation, long startedAtNanos) {
        long elapsedMs = (System.nanoTime() - startedAtNanos) / 1_000_000L;
        if (elapsedMs > configuration.warnThresholdMs()) {
            logger.warning(operation + " exceeded " + configuration.warnThresholdMs() + "ms: " + elapsedMs + "ms");
        }
    }
}
