package it.pintux.life.essentialsaddon.service;

import it.pintux.life.essentialsaddon.api.PetProvider;
import it.pintux.life.essentialsaddon.model.PetBuyResult;
import it.pintux.life.essentialsaddon.model.PetView;
import it.pintux.life.essentialsaddon.model.ShopPetView;
import it.pintux.life.essentialsaddon.model.SkilltreeView;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Facade over {@link PetProvider} plus the MyPet pet-shop bridge. Pet lists are
 * live (async via the provider); the shop listing reads MyPet's pet-shops.yml and
 * flags entries whose pet type the player already owns.
 */
public final class PetCatalogService {

    private final JavaPlugin plugin;
    private final Logger logger;
    private PetProvider provider;

    public PetCatalogService(JavaPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    public void setProvider(PetProvider provider) {
        this.provider = provider;
    }

    public PetProvider getProvider() {
        return provider;
    }

    public boolean isReady() {
        return provider != null && provider.isReady();
    }

    public void listOwnedPets(Player player, Consumer<List<PetView>> callback) {
        if (provider == null) {
            callback.accept(List.of());
            return;
        }
        provider.listOwnedPets(player, callback);
    }

    public void call(Player player, UUID petUuid, Consumer<Boolean> callback) {
        if (provider == null) {
            callback.accept(false);
            return;
        }
        provider.call(player, petUuid, callback);
    }

    public boolean putAway(Player player, java.util.UUID petUuid) {
        return provider != null && provider.putAway(player, petUuid);
    }

    public List<SkilltreeView> listSkilltrees(Player player) {
        return provider == null ? List.of() : provider.listSkilltrees(player);
    }

    public boolean setSkilltree(Player player, String skilltreeName) {
        return provider != null && provider.setSkilltree(player, skilltreeName);
    }

    /**
     * Read MyPet's shop listing from pet-shops.yml and flag entries the player already
     * owns (owns a pet of that type). Owned-set comes from the async pet list, so this is async.
     */
    public void listShopEntries(Player player, String shopId, Consumer<List<ShopPetView>> callback) {
        listOwnedPets(player, owned -> {
            Set<String> ownedTypes = new HashSet<>();
            for (PetView pet : owned) {
                ownedTypes.add(pet.petType().toUpperCase(Locale.ROOT));
            }
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                List<ShopPetView> entries = new ArrayList<>();
                try {
                    entries.addAll(MyPetShopBridge.readShop(shopId, ownedTypes, logger));
                } catch (Throwable t) {
                    logger.warning("MyPet shop read failed: " + t.getMessage());
                }
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(entries));
            });
        });
    }

    public PetBuyResult buyShopEntry(Player player, String shopId, String petId) {
        try {
            return MyPetShopBridge.buy(player, shopId, petId, logger);
        } catch (Throwable t) {
            logger.warning("MyPet shop buy failed: " + t.getMessage());
            return PetBuyResult.fail("internal error");
        }
    }
}
