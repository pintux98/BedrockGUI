package it.pintux.life.bedwarsaddon.service;

import it.pintux.life.bedwarsaddon.api.ShopProvider;
import it.pintux.life.bedwarsaddon.model.PurchaseResult;
import it.pintux.life.bedwarsaddon.model.ShopCategory;
import it.pintux.life.bedwarsaddon.model.ShopContent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.logging.Logger;

public final class ShopCatalogService {
    private final Logger logger;
    private ShopProvider provider;

    public ShopCatalogService(Logger logger) {
        this.logger = logger;
    }

    public void setProvider(ShopProvider provider) {
        this.provider = provider;
    }

    public ShopProvider getProvider() {
        return provider;
    }

    public boolean isReady() {
        return provider != null && provider.isReady();
    }

    public List<ShopCategory> getCategories(Player player) {
        return isReady() ? provider.getCategories(player) : List.of();
    }

    public List<ShopContent> getCategoryContents(Player player, String categoryId) {
        return isReady() ? provider.getCategoryContents(player, categoryId) : List.of();
    }

    public PurchaseResult purchase(Player player, String contentId) {
        if (!isReady()) return PurchaseResult.fail("provider unavailable");
        return provider.purchase(player, contentId);
    }

    public boolean ownsInventory(Inventory inventory) {
        return isReady() && provider.ownsInventory(inventory);
    }
}
