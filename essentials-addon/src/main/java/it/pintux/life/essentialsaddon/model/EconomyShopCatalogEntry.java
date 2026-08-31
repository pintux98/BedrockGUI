package it.pintux.life.essentialsaddon.model;

import me.gypopo.economyshopgui.objects.ShopItem;
import me.gypopo.economyshopgui.objects.shops.ShopSection;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

public final class EconomyShopCatalogEntry {
    private final ShopSection section;
    private final String id;
    private final String displayName;
    private final String title;
    private final int menuOrder;
    private final NavigableMap<Integer, List<ShopItemView>> itemsByPage;
    private final Map<String, ShopItemView> itemsById;
    private final Map<String, ShopItem> liveItemsById;

    public EconomyShopCatalogEntry(ShopSection section, String id, String displayName, String title, int menuOrder,
                                   NavigableMap<Integer, List<ShopItemView>> itemsByPage,
                                   Map<String, ShopItemView> itemsById,
                                   Map<String, ShopItem> liveItemsById) {
        this.section = section;
        this.id = id;
        this.displayName = displayName;
        this.title = title;
        this.menuOrder = menuOrder;
        this.itemsByPage = itemsByPage;
        this.itemsById = itemsById;
        this.liveItemsById = liveItemsById;
    }

    public ShopSection getSection() {
        return section;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTitle() {
        return title;
    }

    public int getMenuOrder() {
        return menuOrder;
    }

    public NavigableMap<Integer, List<ShopItemView>> getItemsByPage() {
        return itemsByPage;
    }

    public Map<String, ShopItemView> getItemsById() {
        return itemsById;
    }

    public Map<String, ShopItem> getLiveItemsById() {
        return liveItemsById;
    }
}
