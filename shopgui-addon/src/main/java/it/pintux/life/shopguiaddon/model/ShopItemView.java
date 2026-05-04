package it.pintux.life.shopguiaddon.model;

public final class ShopItemView {
    private final String id;
    private final String displayName;
    private final String description;
    private final String type;
    private final int page;
    private final int slot;
    private final String material;
    private final String linkedShopId;
    private final double baseBuyPrice;
    private final double baseSellPrice;

    public ShopItemView(String id, String displayName, String description, String type, int page, int slot,
                        String material, String linkedShopId, double baseBuyPrice, double baseSellPrice) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.type = type;
        this.page = page;
        this.slot = slot;
        this.material = material;
        this.linkedShopId = linkedShopId;
        this.baseBuyPrice = baseBuyPrice;
        this.baseSellPrice = baseSellPrice;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public int getPage() { return page; }
    public int getSlot() { return slot; }
    public String getMaterial() { return material; }
    public String getLinkedShopId() { return linkedShopId; }
    public double getBaseBuyPrice() { return baseBuyPrice; }
    public double getBaseSellPrice() { return baseSellPrice; }
}
