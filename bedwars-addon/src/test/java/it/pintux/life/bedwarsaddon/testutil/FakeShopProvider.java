package it.pintux.life.bedwarsaddon.testutil;

import it.pintux.life.bedwarsaddon.api.ShopProvider;
import it.pintux.life.bedwarsaddon.model.PurchaseResult;
import it.pintux.life.bedwarsaddon.model.ShopCategory;
import it.pintux.life.bedwarsaddon.model.ShopContent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-memory ShopProvider for unit tests. No Bukkit calls. */
public final class FakeShopProvider implements ShopProvider {
    public boolean ready = true;
    public final List<ShopCategory> categories = new ArrayList<>();
    public final Map<String, List<ShopContent>> contents = new LinkedHashMap<>();
    public final List<String> purchased = new ArrayList<>();
    public PurchaseResult nextResult = PurchaseResult.ok();

    @Override public String getProviderId() { return "Fake"; }
    @Override public boolean isReady() { return ready; }
    @Override public List<ShopCategory> getCategories(Player player) { return categories; }
    @Override public List<ShopContent> getCategoryContents(Player player, String categoryId) {
        return contents.getOrDefault(categoryId, List.of());
    }
    @Override public PurchaseResult purchase(Player player, String contentId) {
        purchased.add(contentId);
        return nextResult;
    }
    @Override public boolean ownsInventory(Inventory inventory) { return false; }
}
