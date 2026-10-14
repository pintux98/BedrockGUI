package it.pintux.life.essentialsaddon.provider;

import fr.nocsy.mcpets.api.MCPetsAPI;
import fr.nocsy.mcpets.data.Pet;
import fr.nocsy.mcpets.data.PetDespawnReason;
import fr.nocsy.mcpets.data.config.Language;
import fr.nocsy.mcpets.data.livingpets.PetStats;
import it.pintux.life.essentialsaddon.api.PetProvider;
import it.pintux.life.essentialsaddon.model.PetBuyResult;
import it.pintux.life.essentialsaddon.model.PetView;
import it.pintux.life.essentialsaddon.model.ShopPetView;
import it.pintux.life.essentialsaddon.model.SkilltreeView;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class McPetsProvider implements PetProvider {

    private final JavaPlugin plugin;
    private final Logger logger;

    public McPetsProvider(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public static boolean isAvailable(JavaPlugin plugin) {
        if (Bukkit.getPluginManager().getPlugin("MCPets") == null) {
            return false;
        }
        try {
            Class.forName("fr.nocsy.mcpets.api.MCPetsAPI", false, plugin.getClass().getClassLoader());
            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("MCPets detected but its API is unavailable: " + t.getClass().getSimpleName());
            return false;
        }
    }

    @Override
    public String getProviderId() {
        return "mcpets";
    }

    @Override
    public boolean isReady() {
        try {
            return MCPetsAPI.getPluginInstance() != null;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void listOwnedPets(Player player, Consumer<List<PetView>> callback) {
        runMain(() -> {
            List<PetView> views = new ArrayList<>();
            try {
                List<Pet> active = MCPetsAPI.getActivePetsForPlayer(player.getUniqueId());
                for (Pet pet : MCPetsAPI.getAvailablePets(player)) {
                    views.add(toView(pet, findActive(active, pet.getId())));
                }
            } catch (Throwable t) {
                logger.warning("MCPets listOwnedPets failed: " + t.getMessage());
            }
            callback.accept(views);
        });
    }

    private PetView toView(Pet pet, Pet active) {
        Pet source = active != null ? active : pet;
        int level = -1;
        double health = -1;
        double maxHealth = -1;
        PetStats stats = source.getPetStats();
        if (stats != null) {
            level = stats.getCurrentLevelIndex();
            health = stats.getCurrentHealth();
            if (stats.getCurrentLevel() != null) {
                maxHealth = stats.getCurrentLevel().getMaxHealth();
            }
        }
        return new PetView(pet.getId(), displayName(source), pet.getId(), level, health, maxHealth, -1, null, active != null);
    }

    private static Pet findActive(List<Pet> active, String petId) {
        if (active == null) {
            return null;
        }
        for (Pet pet : active) {
            if (pet.getId().equals(petId) && pet.isStillHere()) {
                return pet;
            }
        }
        return null;
    }

    private static String displayName(Pet pet) {
        String custom = pet.getCurrentName();
        if (custom != null && !custom.isBlank()
                && !ChatColor.stripColor(custom).equalsIgnoreCase(ChatColor.stripColor(Language.TAG_TO_REMOVE_NAME.getMessage()))) {
            return custom;
        }
        ItemStack icon = pet.getIcon();
        ItemMeta meta = icon == null ? null : icon.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        return pet.getId();
    }

    @Override
    public void call(Player player, String petId, Consumer<Boolean> callback) {
        runMain(() -> {
            boolean spawned = false;
            try {
                for (Pet pet : MCPetsAPI.getAvailablePets(player)) {
                    if (pet.getId().equals(petId)) {
                        int result = pet.copy().spawn(player, player.getLocation());
                        spawned = result == Pet.MOB_SPAWN || result == Pet.DESPAWNED_PREVIOUS;
                        break;
                    }
                }
            } catch (Throwable t) {
                logger.warning("MCPets spawn failed: " + t.getMessage());
            }
            callback.accept(spawned);
        });
    }

    @Override
    public boolean putAway(Player player, String petId) {
        try {
            Pet active = findActive(MCPetsAPI.getActivePetsForPlayer(player.getUniqueId()), petId);
            return active != null && active.despawn(PetDespawnReason.REVOKE);
        } catch (Throwable t) {
            logger.warning("MCPets despawn failed: " + t.getMessage());
            return false;
        }
    }

    @Override
    public String activePetName(Player player) {
        List<Pet> active = MCPetsAPI.getActivePetsForPlayer(player.getUniqueId());
        return active == null || active.isEmpty() ? null : displayName(active.get(0));
    }

    @Override
    public List<SkilltreeView> listSkilltrees(Player player) {
        return List.of();
    }

    @Override
    public boolean setSkilltree(Player player, String skilltreeName) {
        return false;
    }

    @Override
    public List<ShopPetView> listShopEntries(Player player) {
        return List.of();
    }

    @Override
    public PetBuyResult buyShopEntry(Player player, String shopId, String petId) {
        return PetBuyResult.fail("MCPets has no pet shop");
    }

    private void runMain(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }
}
