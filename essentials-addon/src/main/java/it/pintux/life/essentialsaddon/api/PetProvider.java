package it.pintux.life.essentialsaddon.api;

import it.pintux.life.essentialsaddon.model.PetBuyResult;
import it.pintux.life.essentialsaddon.model.PetView;
import it.pintux.life.essentialsaddon.model.ShopPetView;
import it.pintux.life.essentialsaddon.model.SkilltreeView;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

/**
 * Contract for pet providers (MyPet, MCPets). Keeps each plugin's API out of the
 * service/form layer.
 */
public interface PetProvider {

    String getProviderId();

    boolean isReady();

    default boolean supportsSkilltrees() {
        return false;
    }

    default boolean supportsShop() {
        return false;
    }

    default List<String> petOptions(Player player, String petId) {
        return List.of();
    }

    default boolean runPetOption(Player player, String petId, String option) {
        return false;
    }

    /** Async: a player's full pet list (active + stored). Callback runs on the main thread. */
    void listOwnedPets(Player player, Consumer<List<PetView>> callback);

    /** Async: activate + spawn the pet with this id. Callback runs on the main thread. */
    void call(Player player, String petId, Consumer<Boolean> callback);

    /** Despawn the player's active pet (keeps it active, just not in the world). */
    boolean putAway(Player player, String petId);

    /** Display name of the player's active pet, or {@code null} if there is none. */
    String activePetName(Player player);

    /** Skilltrees selectable for the player's active pet (current one flagged). */
    List<SkilltreeView> listSkilltrees(Player player);

    /** Switch the active pet's skilltree. */
    boolean setSkilltree(Player player, String skilltreeName);

    /** Buyable shop pets for this player ({@code owned} = already owns that pet type). */
    List<ShopPetView> listShopEntries(Player player);

    /** Charge the player and grant the shop pet. Runs on the main thread. */
    PetBuyResult buyShopEntry(Player player, String shopId, String petId);
}
