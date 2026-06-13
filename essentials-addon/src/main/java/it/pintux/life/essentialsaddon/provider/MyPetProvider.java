package it.pintux.life.essentialsaddon.provider;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.event.MyPetSelectSkilltreeEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import it.pintux.life.essentialsaddon.api.PetProvider;
import it.pintux.life.essentialsaddon.model.PetBuyResult;
import it.pintux.life.essentialsaddon.model.PetView;
import it.pintux.life.essentialsaddon.model.ShopPetView;
import it.pintux.life.essentialsaddon.model.SkilltreeView;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class MyPetProvider implements PetProvider {

    private final JavaPlugin plugin;
    private final Logger logger;

    public MyPetProvider(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /** True only if MyPet is present AND its API classes are loadable. */
    public static boolean isAvailable(JavaPlugin plugin) {
        Plugin myPet = Bukkit.getPluginManager().getPlugin("MyPet");
        if (myPet == null) {
            return false;
        }
        try {
            Class.forName("de.Keyle.MyPet.MyPetApi", false, plugin.getClass().getClassLoader());
            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("MyPet detected but its API is unavailable: " + t.getClass().getSimpleName());
            return false;
        }
    }

    @Override
    public String getProviderId() {
        return "mypet";
    }

    @Override
    public boolean isReady() {
        try {
            return MyPetApi.getPlugin() != null;
        } catch (Throwable t) {
            return false;
        }
    }

    private MyPetPlayer myPetPlayer(Player player) {
        try {
            if (!MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
                return MyPetApi.getPlayerManager().registerMyPetPlayer(player);
            }
            return MyPetApi.getPlayerManager().getMyPetPlayer(player);
        } catch (Throwable t) {
            logger.warning("MyPet getMyPetPlayer failed: " + t.getMessage());
            return null;
        }
    }

    private MyPet activePet(Player player) {
        try {
            if (MyPetApi.getMyPetManager().hasActiveMyPet(player)) {
                return MyPetApi.getMyPetManager().getMyPet(player);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Override
    public void listOwnedPets(Player player, Consumer<List<PetView>> callback) {
        MyPetPlayer owner = myPetPlayer(player);
        if (owner == null) {
            runMain(() -> callback.accept(List.of()));
            return;
        }
        MyPet active = activePet(player);
        UUID activeUuid = active != null ? active.getUUID() : null;
        try {
            MyPetApi.getRepository().getMyPets(owner, new RepositoryCallback<List<StoredMyPet>>() {
                @Override
                public void callback(List<StoredMyPet> pets) {
                    List<PetView> views = new ArrayList<>();
                    if (pets != null) {
                        for (StoredMyPet pet : pets) {
                            views.add(toView(pet, activeUuid, active));
                        }
                    }
                    runMain(() -> callback.accept(views));
                }
            });
        } catch (Throwable t) {
            logger.warning("MyPet getMyPets failed: " + t.getMessage());
            runMain(() -> callback.accept(List.of()));
        }
    }

    private PetView toView(StoredMyPet pet, UUID activeUuid, MyPet active) {
        boolean isActive = activeUuid != null && activeUuid.equals(pet.getUUID());
        int level = -1;
        double maxHealth = -1;
        if (isActive && active != null) {
            try {
                level = active.getExperience().getLevel();
                maxHealth = active.getMaxHealth();
            } catch (Throwable ignored) {
            }
        }
        String skilltree = pet.getSkilltree() != null ? pet.getSkilltree().getName() : "None";
        return new PetView(
                pet.getUUID(),
                pet.getPetName(),
                pet.getPetType() != null ? pet.getPetType().name() : "Unknown",
                level,
                pet.getHealth(),
                maxHealth,
                pet.getSaturation(),
                skilltree,
                isActive
        );
    }

    @Override
    public void call(Player player, UUID petUuid, Consumer<Boolean> callback) {
        MyPetPlayer owner = myPetPlayer(player);
        if (owner == null) {
            runMain(() -> callback.accept(false));
            return;
        }
        try {
            MyPetApi.getRepository().getMyPet(petUuid, new RepositoryCallback<StoredMyPet>() {
                @Override
                public void callback(StoredMyPet stored) {
                    runMain(() -> callback.accept(activateAndSpawn(owner, stored)));
                }
            });
        } catch (Throwable t) {
            logger.warning("MyPet getMyPet failed: " + t.getMessage());
            runMain(() -> callback.accept(false));
        }
    }

    /** Main-thread: activate the stored pet, mark it active for its world group, spawn it. */
    private boolean activateAndSpawn(MyPetPlayer owner, StoredMyPet stored) {
        if (stored == null) {
            return false;
        }
        try {
            Optional<MyPet> activated = MyPetApi.getMyPetManager().activateMyPet(stored);
            if (activated.isEmpty()) {
                return false;
            }
            MyPet pet = activated.get();
            owner.setMyPetForWorldGroup(stored.getWorldGroup(), pet.getUUID());
            pet.createEntity();
            return true;
        } catch (Throwable t) {
            logger.warning("MyPet activate/spawn failed: " + t.getMessage());
            return false;
        }
    }

    @Override
    public boolean putAway(Player player) {
        MyPet active = activePet(player);
        if (active == null) {
            return false;
        }
        try {
            active.removePet(false);
            return true;
        } catch (Throwable t) {
            logger.warning("MyPet removePet failed: " + t.getMessage());
            return false;
        }
    }

    @Override
    public List<SkilltreeView> listSkilltrees(Player player) {
        MyPet active = activePet(player);
        if (active == null) {
            return List.of();
        }
        List<SkilltreeView> result = new ArrayList<>();
        try {
            Skilltree current = active.getSkilltree();
            String currentName = current != null ? current.getName() : null;
            for (Skilltree tree : MyPetApi.getSkilltreeManager().getOrderedSkilltrees()) {
                if (!tree.getMobTypes().contains(active.getPetType())) {
                    continue;
                }
                if (!tree.checkRequirements(active)) {
                    continue;
                }
                result.add(new SkilltreeView(
                        tree.getName(),
                        tree.getDisplayName() != null ? tree.getDisplayName() : tree.getName(),
                        tree.getName().equals(currentName)
                ));
            }
        } catch (Throwable t) {
            logger.warning("MyPet listSkilltrees failed: " + t.getMessage());
            return List.of();
        }
        return result;
    }

    @Override
    public boolean setSkilltree(Player player, String skilltreeName) {
        MyPet active = activePet(player);
        if (active == null) {
            return false;
        }
        try {
            if (!MyPetApi.getSkilltreeManager().hasSkilltree(skilltreeName)) {
                return false;
            }
            Skilltree tree = MyPetApi.getSkilltreeManager().getSkilltree(skilltreeName);
            if (tree == null || !tree.getMobTypes().contains(active.getPetType()) || !tree.checkRequirements(active)) {
                return false;
            }
            return active.setSkilltree(tree, MyPetSelectSkilltreeEvent.Source.PlayerCommand);
        } catch (Throwable t) {
            logger.warning("MyPet setSkilltree failed: " + t.getMessage());
            return false;
        }
    }

    @Override
    public boolean ownsType(Player player, String petType) {
        MyPet active = activePet(player);
        if (active != null && active.getPetType() != null && active.getPetType().name().equalsIgnoreCase(petType)) {
            return true;
        }
        // Stored pets need an async lookup; BedrockPetService computes the full owned-set
        // from the async pet list when building the shop form. This sync check covers the active pet.
        return false;
    }

    @Override
    public List<ShopPetView> listShopEntries(Player player) {
        // Shop listing/purchase is handled by PetCatalogService (reads MyPet's pet-shops.yml). See Task B5.
        return List.of();
    }

    @Override
    public PetBuyResult buyShopEntry(Player player, String shopId, String petId) {
        return PetBuyResult.fail("handled by catalog bridge");
    }

    private void runMain(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }
}
