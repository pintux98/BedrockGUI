package it.pintux.life.essentialsaddon.api;

import it.pintux.life.essentialsaddon.model.PetBuyResult;
import it.pintux.life.essentialsaddon.model.PetView;
import it.pintux.life.essentialsaddon.model.ShopPetView;
import it.pintux.life.essentialsaddon.model.SkilltreeView;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Contract for pet providers. Only MyPet is implemented today; the interface
 * exists for consistency with the other addon modules and to keep the MyPet
 * API out of the service/form layer.
 */
public interface PetProvider {

    String getProviderId();

    boolean isReady();

    /** Async: a player's full pet list (active + stored). Callback runs on the main thread. */
    void listOwnedPets(Player player, Consumer<List<PetView>> callback);

    /** Async: activate + spawn the stored pet with this UUID. Callback runs on the main thread. */
    void call(Player player, UUID petUuid, Consumer<Boolean> callback);

    /** Despawn the player's active pet (keeps it active, just not in the world). */
    boolean putAway(Player player);

    /** Skilltrees selectable for the player's active pet (current one flagged). */
    List<SkilltreeView> listSkilltrees(Player player);

    /** Switch the active pet's skilltree. */
    boolean setSkilltree(Player player, String skilltreeName);

    /** Buyable shop pets for this player ({@code owned} = already owns that pet type). */
    List<ShopPetView> listShopEntries(Player player);

    /** Charge the player and grant the shop pet. Runs on the main thread. */
    PetBuyResult buyShopEntry(Player player, String shopId, String petId);

    /** True if the player already owns at least one pet of the given MyPet type. */
    boolean ownsType(Player player, String petType);
}
