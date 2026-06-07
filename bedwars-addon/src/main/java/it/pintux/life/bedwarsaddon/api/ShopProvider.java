package it.pintux.life.bedwarsaddon.api;

import it.pintux.life.bedwarsaddon.model.PurchaseResult;
import it.pintux.life.bedwarsaddon.model.ShopCategory;
import it.pintux.life.bedwarsaddon.model.ShopContent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

/**
 * Contract for a Bedwars shop backend (BedWars2023, etc.).
 * Implementations are the ONLY classes allowed to touch the underlying plugin's types.
 */
public interface ShopProvider {
    String getProviderId();

    boolean isReady();

    /** Static category structure for the player's shop index. */
    List<ShopCategory> getCategories(Player player);

    /** Live contents of one category for this player (cost/affordable computed now). */
    List<ShopContent> getCategoryContents(Player player, String categoryId);

    /** Execute the purchase of a content id. */
    PurchaseResult purchase(Player player, String contentId);

    /** Interception seam: does this inventory belong to this provider's shop GUI? */
    boolean ownsInventory(Inventory inventory);
}
