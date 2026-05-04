package it.pintux.life.shopguiaddon.model;

import net.brcdev.shopgui.shop.Shop;
import net.brcdev.shopgui.shop.item.ShopItem;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class ShopCatalogEntry {
    private final Shop shop;
    private final String id;
    private final String displayName;
    private final NavigableMap<Integer, String> pageTitles;
    private final NavigableMap<Integer, List<ShopItemView>> itemsByPage;
    private final Map<String, ShopItemView> itemsById;
    private final Map<String, ShopItem> liveItemsById;

    public ShopCatalogEntry(Shop shop, String id, String displayName, Map<Integer, String> pageTitles,
                            Map<Integer, List<ShopItemView>> itemsByPage, Map<String, ShopItemView> itemsById,
                            Map<String, ShopItem> liveItemsById) {
        this.shop = shop;
        this.id = id;
        this.displayName = displayName;
        this.pageTitles = Collections.unmodifiableNavigableMap(new TreeMap<>(pageTitles));
        this.itemsByPage = Collections.unmodifiableNavigableMap(new TreeMap<>(itemsByPage));
        this.itemsById = Collections.unmodifiableMap(itemsById);
        this.liveItemsById = Collections.unmodifiableMap(liveItemsById);
    }

    public Shop getShop() { return shop; }
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public NavigableMap<Integer, String> getPageTitles() { return pageTitles; }
    public NavigableMap<Integer, List<ShopItemView>> getItemsByPage() { return itemsByPage; }
    public Map<String, ShopItemView> getItemsById() { return itemsById; }
    public Map<String, ShopItem> getLiveItemsById() { return liveItemsById; }
}
