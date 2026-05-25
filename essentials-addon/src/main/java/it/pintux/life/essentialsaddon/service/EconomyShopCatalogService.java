package it.pintux.life.essentialsaddon.service;

import it.pintux.life.essentialsaddon.model.EconomyShopCatalogEntry;
import it.pintux.life.essentialsaddon.model.ShopItemView;
import it.pintux.life.essentialsaddon.util.ShopGuiReflectionSupport;
import me.gypopo.economyshopgui.api.EconomyShopGUIHook;
import me.gypopo.economyshopgui.api.objects.BuyPrice;
import me.gypopo.economyshopgui.api.objects.SellPrice;
import me.gypopo.economyshopgui.objects.ShopItem;
import me.gypopo.economyshopgui.objects.ShopPageItems;
import me.gypopo.economyshopgui.objects.shops.ShopSection;
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

public final class EconomyShopCatalogService {
    private final Logger logger;
    private volatile Map<String, EconomyShopCatalogEntry> catalog = Map.of();

    public EconomyShopCatalogService(Logger logger) {
        this.logger = logger;
    }

    public synchronized void refreshCatalog() {
        Map<String, EconomyShopCatalogEntry> refreshed = new ConcurrentHashMap<>();
        try {
            List<String> sections = EconomyShopGUIHook.getShopSections();
            if (sections == null || sections.isEmpty()) {
                return;
            }
            for (String sectionId : sections) {
                ShopSection section = EconomyShopGUIHook.getShopSection(sectionId);
                if (section == null) {
                    continue;
                }
                EconomyShopCatalogEntry entry = snapshot(section);
                refreshed.put(entry.getId(), entry);
            }
        } catch (Exception exception) {
            logger.warning("Unable to refresh EconomyShopGUI catalog: " + exception.getMessage());
            return;
        }
        this.catalog = Map.copyOf(refreshed);
    }

    public boolean isReady() {
        return !catalog.isEmpty();
    }

    public boolean isAvailable() {
        try {
            return EconomyShopGUIHook.getShopSections() != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public Collection<EconomyShopCatalogEntry> getAccessibleShops(Player player) {
        List<EconomyShopCatalogEntry> result = new ArrayList<>();
        for (EconomyShopCatalogEntry entry : catalog.values()) {
            if (!entry.getSection().isHidden() && !entry.getSection().isSubSection() && hasShopAccess(player, entry)) {
                result.add(entry);
            }
        }
        result.sort(Comparator.comparing(EconomyShopCatalogEntry::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public Optional<EconomyShopCatalogEntry> getShop(String sectionId) {
        return Optional.ofNullable(catalog.get(sectionId.toLowerCase(Locale.ROOT)));
    }

    public Optional<ShopItemView> getItemView(String sectionId, String itemId) {
        return getShop(sectionId).map(entry -> entry.getItemsById().get(itemId));
    }

    public Optional<ShopItem> getLiveItem(String sectionId, String itemId) {
        return getShop(sectionId).map(entry -> entry.getLiveItemsById().get(itemId));
    }

    public List<Integer> getAccessiblePages(Player player, String sectionId) {
        Optional<EconomyShopCatalogEntry> optionalEntry = getShop(sectionId);
        if (optionalEntry.isEmpty()) {
            return List.of();
        }
        List<Integer> pages = new ArrayList<>();
        for (Map.Entry<Integer, List<ShopItemView>> entry : optionalEntry.get().getItemsByPage().entrySet()) {
            if (!getAccessibleItems(player, sectionId, entry.getKey()).isEmpty()) {
                pages.add(entry.getKey());
            }
        }
        if (pages.isEmpty()) {
            pages.add(1);
        }
        return pages;
    }

    public List<ShopItemView> getAccessibleItems(Player player, String sectionId, int page) {
        Optional<EconomyShopCatalogEntry> optionalEntry = getShop(sectionId);
        if (optionalEntry.isEmpty()) {
            return List.of();
        }
        List<ShopItemView> source = optionalEntry.get().getItemsByPage().getOrDefault(page, List.of());
        logger.fine("[EShop] getAccessibleItems section=" + sectionId + " page=" + page + " source=" + source.size());
        List<ShopItemView> accessible = new ArrayList<>();
        for (ShopItemView view : source) {
            ShopItem liveItem = optionalEntry.get().getLiveItemsById().get(view.getId());
            boolean ok = liveItem != null && isAccessible(player, liveItem, sectionId);
            logger.fine("[EShop]   item='" + view.getId() + "' accessible=" + ok);
            if (ok) {
                accessible.add(view);
            }
        }
        return accessible;
    }

    public boolean hasShopAccess(Player player, EconomyShopCatalogEntry entry) {
        if (player == null || entry == null) {
            return false;
        }
        if (player.hasPermission("economyshopgui.shop.*") || player.hasPermission("EconomyShopGUI.shop.*")) {
            return true;
        }
        String permission = "EconomyShopGUI.shop." + entry.getId();
        return player.hasPermission(permission) || player.hasPermission(permission.toLowerCase(Locale.ROOT));
    }

    public Optional<BuyPrice> resolveBuyPrice(Player player, ShopItem shopItem, int amount) {
        if (player == null || shopItem == null || shopItem.isLinked() || shopItem.hasItemError() || shopItem.isHidden()) {
            return Optional.empty();
        }
        ItemStack stack = baseItem(shopItem);
        if (stack == null) {
            return Optional.empty();
        }
        stack.setAmount(Math.max(1, amount));
        Optional<BuyPrice> price = EconomyShopGUIHook.getBuyPrice(player, stack);
        if (price.isEmpty() || !matchesShopItem(shopItem, price.get().getShopItem())) {
            return Optional.empty();
        }
        return price;
    }

    public Optional<SellPrice> resolveSellPrice(Player player, ShopItem shopItem, int amount) {
        if (player == null || shopItem == null || shopItem.isLinked() || shopItem.hasItemError() || shopItem.isHidden()) {
            return Optional.empty();
        }
        ItemStack stack = baseItem(shopItem);
        if (stack == null) {
            return Optional.empty();
        }
        stack.setAmount(Math.max(1, amount));
        Optional<SellPrice> price = EconomyShopGUIHook.getSellPrice(player, stack);
        if (price.isEmpty() || !matchesShopItem(shopItem, price.get().getShopItem())) {
            return Optional.empty();
        }
        return price;
    }

    public Optional<String> resolveSectionByInventoryTitle(String rawTitle) {
        String normalized = normalizeTitle(rawTitle);
        for (EconomyShopCatalogEntry entry : catalog.values()) {
            if (normalizeTitle(entry.getDisplayName()).equals(normalized) || entry.getId().equalsIgnoreCase(normalized)) {
                return Optional.of(entry.getId());
            }
        }
        return Optional.empty();
    }

    private boolean isAccessible(Player player, ShopItem shopItem, String sectionId) {
        if (shopItem.hasItemError() || shopItem.isHidden()) {
            logger.fine("[EShop] isAccessible: item has error or hidden, itemPath=" + shopItem.getItemPath());
            return false;
        }
        if (shopItem.isLinked()) {
            boolean hasSub = shopItem.getSubSection() != null && getShop(shopItem.getSubSection()).isPresent();
            logger.fine("[EShop] isAccessible: linked item, subSection=" + shopItem.getSubSection() + " hasSub=" + hasSub);
            return hasSub;
        }
        if (!hasShopAccess(player, getShop(sectionId).orElse(null))) {
            logger.fine("[EShop] isAccessible: no shop access for player");
            return false;
        }
        Optional<BuyPrice> buy = resolveBuyPrice(player, shopItem, 1);
        Optional<SellPrice> sell = resolveSellPrice(player, shopItem, 1);
        logger.fine("[EShop] isAccessible: buy=" + buy.isPresent() + " sell=" + sell.isPresent() + " for item=" + shopItem.getItemPath());
        return buy.isPresent() || sell.isPresent();
    }

    private EconomyShopCatalogEntry snapshot(ShopSection section) {
        NavigableMap<Integer, List<ShopItemView>> itemsByPage = new TreeMap<>();
        Map<String, ShopItemView> itemsById = new HashMap<>();
        Map<String, ShopItem> liveItemsById = new HashMap<>();
        Map<String, ShopItem> byPath = new HashMap<>();
        String sectionName = section.getSection();
        for (ShopItem shopItem : section.getShopItems()) {
            if (shopItem != null && shopItem.getItemPath() != null && !shopItem.getItemPath().isEmpty()) {
                byPath.put(shopItem.getItemPath(), shopItem);
            }
        }

        int pages = Math.max(1, section.getPages());
        for (int page = 1; page <= pages; page++) {
            ShopPageItems pageItems = section.getShopPageItems(page);
            if (pageItems == null) {
                continue;
            }
            List<ShopItemView> views = new ArrayList<>();
            for (Integer slot : pageItems.getItems().keySet()) {
                String itemLoc = pageItems.getItem(slot);
                String fullPath = sectionName + "." + itemLoc;
                ShopItem shopItem = byPath.get(fullPath);
                if (shopItem == null) {
                    continue;
                }
                ShopItemView view = toView(shopItem, page, slot);
                views.add(view);
                itemsById.put(view.getId(), view);
                liveItemsById.put(view.getId(), shopItem);
            }
            views.sort(Comparator.comparingInt(ShopItemView::getSlot).thenComparing(ShopItemView::getId));
            if (!views.isEmpty()) {
                itemsByPage.put(page, views);
            }
        }

        if (itemsByPage.isEmpty()) {
            itemsByPage.put(1, List.of());
        }

        String id = section.getSection().toLowerCase(Locale.ROOT);
        return new EconomyShopCatalogEntry(section, id, prettyName(section.getSection()), itemsByPage, itemsById, liveItemsById);
    }

    private ShopItemView toView(ShopItem shopItem, int page, int slot) {
        ItemStack displayItem = shopItem.getShopItem() != null ? shopItem.getShopItem() : shopItem.getItemToGive();
        double buyPrice = 0D;
        double sellPrice = 0D;
        if (displayItem != null) {
            buyPrice = Optional.ofNullable(EconomyShopGUIHook.getItemBuyPrice(shopItem, 1)).orElse(-1D);
            sellPrice = Optional.ofNullable(EconomyShopGUIHook.getItemSellPrice(shopItem, displayItem.clone(), 1, 0)).orElse(-1D);
        }
        return new ShopItemView(
                shopItem.getItemPath(),
                ShopGuiReflectionSupport.displayName(displayItem),
                ShopGuiReflectionSupport.description(displayItem),
                shopItem.isLinked() ? "LINKED" : "ITEM",
                page,
                slot,
                ShopGuiReflectionSupport.material(displayItem),
                shopItem.isLinked() ? shopItem.getSubSection() : null,
                buyPrice,
                sellPrice
        );
    }

    private ItemStack baseItem(ShopItem shopItem) {
        ItemStack base = shopItem.getItemToGive() != null ? shopItem.getItemToGive().clone() :
                (shopItem.getShopItem() != null ? shopItem.getShopItem().clone() : null);
        if (base == null) {
            return null;
        }
        base.setAmount(1);
        return base;
    }

    private boolean matchesShopItem(ShopItem expected, ShopItem actual) {
        return actual != null && expected != null && expected.getItemPath() != null
                && expected.getItemPath().equalsIgnoreCase(actual.getItemPath());
    }

    private String prettyName(String id) {
        String lowered = id == null ? "" : id.replace('_', ' ').replace('-', ' ').trim();
        if (lowered.isEmpty()) {
            return "Shop";
        }
        String[] parts = lowered.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return ChatColor.translateAlternateColorCodes('&', builder.toString());
    }

    private String normalizeTitle(String title) {
        return ChatColor.stripColor(title == null ? "" : title).trim().toLowerCase(Locale.ROOT);
    }
}
