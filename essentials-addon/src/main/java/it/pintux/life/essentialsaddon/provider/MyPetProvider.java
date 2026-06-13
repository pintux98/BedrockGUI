package it.pintux.life.essentialsaddon.provider;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.event.MyPetSelectSkilltreeEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.Colorizer;
import de.Keyle.MyPet.api.util.locale.Translation;
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
        boolean isActivePet = activeUuid != null && activeUuid.equals(pet.getUUID());
        int level = -1;
        double maxHealth = -1;
        boolean spawned = false;
        if (isActivePet && active != null) {
            try {
                level = active.getExperience().getLevel();
                maxHealth = active.getMaxHealth();
            } catch (Throwable ignored) {
            }
            try {
                // "Here" = currently spawned in the world. A put-away (despawned) pet is still
                // the active pet, so drive the Call/Put-Away button off spawn state, not activeness.
                spawned = active.getStatus() == MyPet.PetState.Here;
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
                spawned
        );
    }

    @Override
    public void call(Player player, UUID petUuid, Consumer<Boolean> callback) {
        MyPetPlayer owner = myPetPlayer(player);
        if (owner == null) {
            runMain(() -> callback.accept(false));
            return;
        }
        // Already the active pet but despawned (put away) → just re-spawn it, like /petcall.
        MyPet active = activePet(player);
        if (active != null && active.getUUID().equals(petUuid)) {
            runMain(() -> {
                try {
                    active.createEntity();
                    callback.accept(true);
                } catch (Throwable t) {
                    logger.warning("MyPet createEntity failed: " + t.getMessage());
                    callback.accept(false);
                }
            });
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
    public boolean putAway(Player player, java.util.UUID petUuid) {
        MyPet active = activePet(player);
        if (active == null) {
            return false;
        }
        if (petUuid != null && !petUuid.equals(active.getUUID())) {
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
    public String activePetName(Player player) {
        MyPet active = activePet(player);
        return active == null ? null : active.getPetName();
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

    /**
     * Switches the active pet's skilltree, mirroring MyPet's own /petchooseskilltree command:
     * level gate, the switch fee, and MyPet's localized feedback messages (sent directly to the
     * player). Returns true only on a successful switch so the caller can pick a sound.
     */
    @Override
    public boolean setSkilltree(Player player, String skilltreeName) {
        MyPet active = activePet(player);
        if (active == null) {
            player.sendMessage(Translation.getString("Message.No.HasPet", player));
            return false;
        }
        try {
            if (!MyPetApi.getSkilltreeManager().hasSkilltree(skilltreeName)) {
                player.sendMessage(Util.formatText(
                        Translation.getString("Message.Command.Skilltree.CantFindSkilltree", player), skilltreeName));
                return false;
            }
            Skilltree tree = MyPetApi.getSkilltreeManager().getSkilltree(skilltreeName);
            if (tree == null || !tree.getMobTypes().contains(active.getPetType()) || !tree.checkRequirements(active)) {
                player.sendMessage(Util.formatText(
                        Translation.getString("Message.Command.Skilltree.CantFindSkilltree", player), skilltreeName));
                return false;
            }

            MyPetPlayer owner = active.getOwner();
            int requiredLevel = tree.getRequiredLevel();
            int maxLevel = tree.getMaxLevel();
            int level = active.getExperience().getLevel();
            if (requiredLevel > 1 && level < requiredLevel) {
                owner.sendMessage(Util.formatText(
                        Translation.getString("Message.Skilltree.RequiresLevel.Message", player),
                        active.getPetName(), requiredLevel));
                return false;
            }
            if (level > maxLevel) {
                owner.sendMessage(Util.formatText(
                        Translation.getString("Message.Skilltree.MaxLevel.Message", player),
                        active.getPetName(), maxLevel));
                return false;
            }

            if (!active.setSkilltree(tree, MyPetSelectSkilltreeEvent.Source.PlayerCommand)) {
                owner.sendMessage(Translation.getString("Message.Skilltree.NotSwitched", player));
                return false;
            }

            owner.sendMessage(Util.formatText(
                    Translation.getString("Message.Skilltree.SwitchedTo", player),
                    Colorizer.setColors(tree.getDisplayName())));
            applySwitchFee(active, owner, requiredLevel);
            return true;
        } catch (Throwable t) {
            logger.warning("MyPet setSkilltree failed: " + t.getMessage());
            return false;
        }
    }

    /** Deducts the configured skilltree switch fee (mirrors MyPet's /petchooseskilltree). */
    private void applySwitchFee(MyPet active, MyPetPlayer owner, int requiredLevel) {
        if (owner.isMyPetAdmin() && !Configuration.Skilltree.SWITCH_FEE_ADMIN) {
            return;
        }
        double penalty = Configuration.Skilltree.SWITCH_FEE_FIXED
                + active.getExperience().getExp() * Configuration.Skilltree.SWITCH_FEE_PERCENT / 100.;
        if (requiredLevel > 1) {
            double minExp = active.getExperience().getExpByLevel(requiredLevel);
            penalty = active.getExp() - penalty < minExp ? active.getExp() - minExp : penalty;
        }
        if (Configuration.LevelSystem.Experience.ALLOW_LEVEL_DOWNGRADE) {
            active.getExperience().removeExp(penalty);
        } else {
            active.getExperience().removeCurrentExp(penalty);
        }
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
