package it.pintux.life.essentialsaddon.service;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.util.hooks.types.EconomyHook;
import de.Keyle.MyPet.api.util.locale.Translation;
import it.pintux.life.essentialsaddon.model.PetBuyResult;
import it.pintux.life.essentialsaddon.model.ShopPetView;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Bridges MyPet's pet shop. Listing is read straight from pet-shops.yml (stable, documented
 * schema). Buying is done fully in-process using MyPet's public API: the pet template
 * ({@code ShopMyPet}, which implements the public {@code StoredMyPet}) is built reflectively
 * from the same config section MyPet uses, then cloned, charged for and stored/activated via
 * {@code MyPetManager}/{@code Repository}/{@code EconomyHook} — no native GUI involved. All
 * player-facing text uses MyPet's own localized {@code Translation} strings.
 */
final class MyPetShopBridge {

    private static final String SHOP_PET_CLASS = "de.Keyle.MyPet.util.shop.ShopMyPet";

    private MyPetShopBridge() {
    }

    // ---------------- Listing (pet-shops.yml) ----------------

    static List<ShopPetView> readShop(String shopId, Set<String> ownedTypes, Logger logger) {
        ConfigurationSection shops = shopsSection(logger);
        if (shops == null) {
            return List.of();
        }
        String chosen = resolveShopId(shops, shopId);
        if (chosen == null) {
            return List.of();
        }
        ConfigurationSection shop = shops.getConfigurationSection(chosen);
        ConfigurationSection pets = shop == null ? null : shop.getConfigurationSection("Pets");
        if (pets == null) {
            return List.of();
        }
        List<ShopPetView> views = new ArrayList<>();
        for (String petId : pets.getKeys(false)) {
            ConfigurationSection pet = pets.getConfigurationSection(petId);
            if (pet == null) {
                continue;
            }
            double price = pet.getDouble("Price", 0D);
            String petType = pet.getString("PetType", "Unknown");
            String name = pet.getString("Name", petType);
            boolean owned = ownedTypes.contains(petType.toUpperCase(Locale.ROOT));
            views.add(new ShopPetView(chosen, petId, name, petType, price, owned));
        }
        return views;
    }

    // ---------------- Buy (fully integrated, MyPet public API) ----------------

    /**
     * Runs on the main thread. Replicates MyPet's own pet-shop purchase: validate storage
     * limit, charge via the economy hook, clone the configured pet and store/activate it.
     * Sends MyPet's localized messages to the player. Returns whether the purchase started
     * (so the caller can pick a sound); the final "success"/"stored" message is sent from the
     * async repository callback, exactly as MyPet does.
     */
    static PetBuyResult buy(Player player, String shopId, String petId, Logger logger) {
        ConfigurationSection shops = shopsSection(logger);
        if (shops == null) {
            return PetBuyResult.fail("no shop config");
        }
        String chosen = resolveShopId(shops, shopId);
        ConfigurationSection shop = chosen == null ? null : shops.getConfigurationSection(chosen);
        ConfigurationSection pets = shop == null ? null : shop.getConfigurationSection("Pets");
        ConfigurationSection petSection = pets == null ? null : pets.getConfigurationSection(petId);
        if (petSection == null) {
            return PetBuyResult.fail("pet not found");
        }

        StoredMyPet template = buildTemplate(petId, petSection, logger);
        if (template == null) {
            return PetBuyResult.fail("could not build pet template");
        }

        final MyPetPlayer owner = MyPetApi.getPlayerManager().isMyPetPlayer(player)
                ? MyPetApi.getPlayerManager().getMyPetPlayer(player)
                : MyPetApi.getPlayerManager().registerMyPetPlayer(player);
        if (owner == null) {
            return PetBuyResult.fail("no owner");
        }

        // A player who already has an active pet needs the storage permission to buy another
        // (mirrors MyPet's own check, done before charging).
        if (owner.hasMyPet() && !player.hasPermission("MyPet.shop.storage")) {
            player.sendMessage(Translation.getString("Message.Command.Trade.Receiver.HasPet", player));
            return PetBuyResult.fail("storage not allowed");
        }

        // Build the pet to grant BEFORE charging, so a failure here never takes the player's money.
        // getInactiveMyPetFromMyPet does `new InactiveMyPet(template.getOwner())`, so the template
        // MUST have an owner set first (this is what MyPet's own shop does).
        template.setOwner(owner);
        final StoredMyPet clonedPet;
        try {
            clonedPet = MyPetApi.getMyPetManager().getInactiveMyPetFromMyPet(template);
        } catch (Throwable t) {
            logger.warning("MyPet failed to clone shop pet '" + petId + "': " + t.getMessage());
            return PetBuyResult.fail("could not create pet");
        }
        if (clonedPet == null) {
            return PetBuyResult.fail("could not create pet");
        }
        clonedPet.setOwner(owner);
        clonedPet.setWorldGroup(WorldGroup.getGroupByWorld(player.getWorld()).getName());
        clonedPet.setUUID(null);

        double price = petSection.getDouble("Price", 0D);
        if (price > 0) {
            EconomyHook economy = MyPetApi.getHookHelper().getEconomy();
            if (economy == null || !MyPetApi.getHookHelper().isEconomyEnabled()) {
                player.sendMessage(Translation.getString("Message.No.Economy", player));
                return PetBuyResult.fail("no economy");
            }
            if (!economy.canPay(player.getUniqueId(), price)) {
                player.sendMessage(Translation.getString("Message.Shop.NoMoney", player));
                return PetBuyResult.fail("cannot afford");
            }
            if (!economy.pay(player.getUniqueId(), price)) {
                player.sendMessage(Translation.getString("Message.No.Money", player));
                return PetBuyResult.fail("payment failed");
            }
        }

        final double paidPrice = price;
        MyPetApi.getRepository().addMyPet(clonedPet, new RepositoryCallback<Boolean>() {
            @Override
            public void callback(Boolean value) {
                player.sendMessage(Util.formatText(Translation.getString("Message.Shop.Success", player),
                        clonedPet.getPetName(), formatPrice(paidPrice)));
                if (owner.hasMyPet()) {
                    player.sendMessage(Util.formatText(Translation.getString("Message.Shop.SuccessStorage", player),
                            clonedPet.getPetName()));
                } else {
                    owner.setMyPetForWorldGroup(WorldGroup.getGroupByWorld(player.getWorld()), clonedPet.getUUID());
                    MyPetApi.getRepository().updateMyPetPlayer(owner, null);
                    MyPetApi.getMyPetManager().activateMyPet(clonedPet).ifPresent(MyPet::createEntity);
                }
            }
        });
        return PetBuyResult.ok();
    }

    private static StoredMyPet buildTemplate(String petId, ConfigurationSection petSection, Logger logger) {
        try {
            Class<?> shopPetClass = Class.forName(SHOP_PET_CLASS);
            Constructor<?> constructor = shopPetClass.getConstructor(String.class);
            Object shopPet = constructor.newInstance(petId);
            Method load = shopPetClass.getMethod("load", ConfigurationSection.class);
            load.invoke(shopPet, petSection);
            if (shopPet instanceof StoredMyPet stored) {
                return stored;
            }
            logger.warning("MyPet ShopMyPet does not implement StoredMyPet; cannot buy.");
            return null;
        } catch (Throwable t) {
            logger.warning("Failed to build MyPet shop template for '" + petId + "': " + t.getMessage());
            return null;
        }
    }

    private static String formatPrice(double price) {
        try {
            EconomyHook economy = MyPetApi.getHookHelper().getEconomy();
            if (economy != null) {
                return economy.format(price);
            }
        } catch (Throwable ignored) {
        }
        return String.valueOf(price);
    }

    // ---------------- shared ----------------

    private static ConfigurationSection shopsSection(Logger logger) {
        Plugin myPet = Bukkit.getPluginManager().getPlugin("MyPet");
        if (myPet == null) {
            return null;
        }
        File file = new File(myPet.getDataFolder(), "pet-shops.yml");
        if (!file.exists()) {
            logger.warning("MyPet pet-shops.yml not found at " + file.getPath());
            return null;
        }
        return YamlConfiguration.loadConfiguration(file).getConfigurationSection("Shops");
    }

    private static String resolveShopId(ConfigurationSection shops, String shopId) {
        if (shopId != null && !shopId.isBlank() && shops.isConfigurationSection(shopId)) {
            return shopId;
        }
        for (String key : shops.getKeys(false)) {
            ConfigurationSection s = shops.getConfigurationSection(key);
            if (s != null && s.getBoolean("Default", false)) {
                return key;
            }
        }
        for (String key : shops.getKeys(false)) {
            return key;
        }
        return null;
    }
}
