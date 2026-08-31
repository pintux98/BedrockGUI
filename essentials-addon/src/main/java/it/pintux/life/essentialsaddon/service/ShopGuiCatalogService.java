package it.pintux.life.essentialsaddon.service;

import it.pintux.life.common.utils.LegacyColors;
import it.pintux.life.essentialsaddon.config.EssentialsAddonConfiguration;
import it.pintux.life.essentialsaddon.config.ShopCategoryOrder;
import it.pintux.life.essentialsaddon.model.ShopCatalogEntry;
import it.pintux.life.essentialsaddon.model.ShopItemView;
import it.pintux.life.essentialsaddon.util.ShopGuiNames;
import it.pintux.life.essentialsaddon.util.ShopGuiReflectionSupport;
import net.brcdev.shopgui.ShopGuiPlusApi;
import net.brcdev.shopgui.shop.Shop;
import net.brcdev.shopgui.shop.item.ShopItem;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class ShopGuiCatalogService {
    private final Logger logger;
    private final EssentialsAddonConfiguration configuration;
    private volatile Map<String, ShopCatalogEntry> catalog = Map.of();

    public ShopGuiCatalogService(Logger logger, EssentialsAddonConfiguration configuration) {
        this.logger = logger;
        this.configuration = configuration;
    }

    public synchronized void refreshCatalog() {
        if (ShopGuiPlusApi.getPlugin() == null || !ShopGuiPlusApi.getPlugin().getShopManager().areShopsLoaded()) {
            return;
        }
        Map<String, ShopCatalogEntry> refreshed = new ConcurrentHashMap<>();
        Collection<Shop> shops;
        try {
            shops = ShopGuiPlusApi.getPlugin().getShopManager().getShops();
        } catch (Exception | LinkageError failure) {
            logger.warning("Unable to refresh ShopGUI+ catalog: " + failure);
            return;
        }
        int menuOrder = 0;
        for (Shop shop : shops) {
            try {
                refreshed.put(shop.getId().toLowerCase(Locale.ROOT), snapshot(shop, menuOrder++));
            } catch (Exception | LinkageError failure) {
                // one shop built against an incompatible ShopGUI+ signature must not void the whole catalog
                logger.warning("Skipping ShopGUI+ shop that could not be read: " + failure);
            }
        }
        this.catalog = Map.copyOf(refreshed);
    }

    public boolean isReady() {
        return ShopGuiPlusApi.getPlugin() != null && ShopGuiPlusApi.getPlugin().getShopManager().areShopsLoaded() && !catalog.isEmpty();
    }

    public Collection<ShopCatalogEntry> getAccessibleShops(Player player) {
        List<ShopCatalogEntry> result = new ArrayList<>();
        for (ShopCatalogEntry entry : catalog.values()) {
            if (hasShopAccess(player, entry)) {
                result.add(entry);
            }
        }
        result.sort(order());
        return result;
    }

    private Comparator<ShopCatalogEntry> order() {
        Comparator<ShopCatalogEntry> byName =
                Comparator.comparing(entry -> normalizeTitle(entry.getDisplayName()));
        return configuration != null && configuration.shopCategoryOrder() == ShopCategoryOrder.NAME
                ? byName
                : Comparator.comparingInt(ShopCatalogEntry::getMenuOrder).thenComparing(byName);
    }

    public Optional<ShopCatalogEntry> getShop(String shopId) {
        return Optional.ofNullable(catalog.get(shopId.toLowerCase(Locale.ROOT)));
    }

    public Optional<ShopItemView> getItemView(String shopId, String itemId) {
        return getShop(shopId).map(entry -> entry.getItemsById().get(itemId));
    }

    public Optional<ShopItem> getLiveItem(String shopId, String itemId) {
        return getShop(shopId).map(entry -> entry.getLiveItemsById().get(itemId));
    }

    public List<Integer> getAccessiblePages(Player player, String shopId) {
        Optional<ShopCatalogEntry> optionalEntry = getShop(shopId);
        if (optionalEntry.isEmpty()) {
            return List.of();
        }
        List<Integer> pages = new ArrayList<>();
        for (Map.Entry<Integer, List<ShopItemView>> entry : optionalEntry.get().getItemsByPage().entrySet()) {
            if (!getAccessibleItems(player, shopId, entry.getKey()).isEmpty()) {
                pages.add(entry.getKey());
            }
        }
        if (pages.isEmpty()) {
            pages.add(1);
        }
        return pages;
    }

    public List<ShopItemView> getAccessibleItems(Player player, String shopId, int page) {
        Optional<ShopCatalogEntry> optionalEntry = getShop(shopId);
        if (optionalEntry.isEmpty()) {
            return List.of();
        }
        ShopCatalogEntry entry = optionalEntry.get();
        List<ShopItemView> source = entry.getItemsByPage().getOrDefault(page, List.of());
        List<ShopItemView> accessible = new ArrayList<>();
        for (ShopItemView view : source) {
            ShopItem liveItem = entry.getLiveItemsById().get(view.getId());
            if (liveItem != null && hasItemAccess(player, entry, liveItem, view)) {
                accessible.add(view);
            }
        }
        return accessible;
    }

    private boolean hasItemAccess(Player player, ShopCatalogEntry entry, ShopItem liveItem, ShopItemView view) {
        Boolean shopVerdict = ShopGuiReflectionSupport.invokeAccessCheck(entry.getShop(), player, liveItem);
        if (shopVerdict != null) {
            return shopVerdict;
        }
        // ShopGUI+ build without a usable item-gate overload: mirror its per-item permission nodes ourselves
        if (!ShopGuiReflectionSupport.booleanFlag(entry.getShop(), false, "isEnablePerItemPermissions")) {
            return true;
        }
        return player.hasPermission("shopguiplus.item." + entry.getId() + ".*")
                || player.hasPermission("shopguiplus.item." + entry.getId() + "." + view.getId());
    }

    public Optional<ResolvedTitle> resolveByInventoryTitle(String rawTitle) {
        String normalized = normalizeTitle(rawTitle);
        for (ShopCatalogEntry entry : catalog.values()) {
            for (Map.Entry<Integer, String> pageTitle : entry.getPageTitles().entrySet()) {
                if (normalizeTitle(pageTitle.getValue()).equals(normalized)) {
                    return Optional.of(new ResolvedTitle(entry.getId(), pageTitle.getKey()));
                }
            }
            if (normalizeTitle(entry.getDisplayName()).equals(normalized)) {
                return Optional.of(new ResolvedTitle(entry.getId(), 1));
            }
        }
        return Optional.empty();
    }

    public boolean hasShopAccess(Player player, ShopCatalogEntry entry) {
        if (player == null || entry == null) {
            return false;
        }
        if (player.hasPermission("shopguiplus.shops.*") || player.hasPermission("shopguiplus.shops." + entry.getId())) {
            return true;
        }
        return !ShopGuiReflectionSupport.booleanFlag(entry.getShop(), false, "isDenyDirectAccess");
    }

    private ShopCatalogEntry snapshot(Shop shop, int menuOrder) {
        NavigableMap<Integer, String> pageTitles = new TreeMap<>();
        Map<Integer, List<ShopItemView>> itemsByPage = new TreeMap<>();
        Map<String, ShopItemView> itemsById = new HashMap<>();
        Map<String, ShopItem> liveItemsById = new HashMap<>();

        for (ShopItem shopItem : shop.getShopItems()) {
            if (shopItem == null || shopItem.getId() == null) {
                continue;
            }
            if (isNonInteractive(shopItem)) {
                continue;
            }
            int page = Math.max(1, shopItem.getPage());
            pageTitles.putIfAbsent(page, ShopGuiNames.resolvePageName(shop.getName(page), page));
            ShopItemView view = toView(shopItem);
            itemsByPage.computeIfAbsent(page, ignored -> new ArrayList<>()).add(view);
            itemsById.put(view.getId(), view);
            liveItemsById.put(view.getId(), shopItem);
        }

        for (List<ShopItemView> values : itemsByPage.values()) {
            values.sort(Comparator.comparingInt(ShopItemView::getSlot).thenComparing(ShopItemView::getId));
        }

        if (pageTitles.isEmpty()) {
            pageTitles.put(1, ShopGuiNames.resolvePageName(shop.getName(1), 1));
        }

        // ShopGUI+ shows the first page's name in its own shop selection menu, so mirror that for the category list
        String displayName = ShopGuiNames.resolvePageName(shop.getName(1), 1);
        return new ShopCatalogEntry(shop, shop.getId(), displayName, menuOrder, pageTitles, itemsByPage, itemsById, liveItemsById);
    }

    /**
     * ShopGUI+ mixes real stock with decoration: the information icon and the filler/navigation
     * items configured under its {@code settings} block are shop items of type {@code DUMMY} with
     * no price at all. Java players just look at them; on Bedrock they used to render as a button
     * that opened a dead-end form, so keep them out of the catalog entirely.
     */
    private boolean isNonInteractive(ShopItem shopItem) {
        try {
            // compare by name: older ShopGUI+ builds may not ship the DUMMY constant
            if (shopItem.getType() != null && "DUMMY".equals(shopItem.getType().name())) {
                return true;
            }
            String linkedShopId = ShopGuiReflectionSupport.resolveLinkedShopId(shopItem);
            if (linkedShopId != null && !linkedShopId.isBlank()) {
                return false;
            }
            return shopItem.getBuyPrice() < 0 && shopItem.getSellPrice() < 0;
        } catch (Exception | LinkageError failure) {
            return false;
        }
    }

    private ShopItemView toView(ShopItem shopItem) {
        ItemStack displayItem = shopItem.getPlaceholder() != null ? shopItem.getPlaceholder() : shopItem.getItem();
        String type = shopItem.getType() == null ? "UNKNOWN" : shopItem.getType().name();
        return new ShopItemView(
                shopItem.getId(),
                ShopGuiReflectionSupport.displayName(displayItem),
                ShopGuiReflectionSupport.description(displayItem),
                type,
                Math.max(1, shopItem.getPage()),
                Math.max(0, shopItem.getSlot()),
                ShopGuiReflectionSupport.material(displayItem),
                ShopGuiReflectionSupport.resolveLinkedShopId(shopItem),
                shopItem.getBuyPrice(),
                shopItem.getSellPrice()
        );
    }

    private String normalizeTitle(String title) {
        return ChatColor.stripColor(LegacyColors.translate(title == null ? "" : title)).trim().toLowerCase(Locale.ROOT);
    }

    public record ResolvedTitle(String shopId, int page) { }
}
