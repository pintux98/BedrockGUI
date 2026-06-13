package it.pintux.life.essentialsaddon.service;

import it.pintux.life.essentialsaddon.model.ShopPetView;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Bridges MyPet's pet shop. Listing is read directly from pet-shops.yml (stable,
 * documented schema). Purchasing delegates to MyPet's native ShopService so Geyser
 * can translate the GUI for Bedrock players.
 */
final class MyPetShopBridge {

    private MyPetShopBridge() {
    }

    // ---------------- Listing (pet-shops.yml) ----------------

    static List<ShopPetView> readShop(String shopId, Set<String> ownedTypes, Logger logger) {
        Plugin myPet = Bukkit.getPluginManager().getPlugin("MyPet");
        if (myPet == null) {
            return List.of();
        }
        File file = new File(myPet.getDataFolder(), "pet-shops.yml");
        if (!file.exists()) {
            logger.warning("MyPet pet-shops.yml not found at " + file.getPath());
            return List.of();
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection shops = yml.getConfigurationSection("Shops");
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

    // ---------------- Open native shop (typed API; caller must be on main thread) ----------------

    static boolean openNativeShop(org.bukkit.entity.Player player, String shopId) {
        Optional<de.Keyle.MyPet.api.util.service.types.ShopService> svc =
                de.Keyle.MyPet.MyPetApi.getServiceManager().getService(de.Keyle.MyPet.api.util.service.types.ShopService.class);
        if (svc.isEmpty()) {
            return false;
        }
        de.Keyle.MyPet.api.util.service.types.ShopService shop = svc.get();
        if (shopId == null || shopId.isBlank()) {
            shop.open(player);
        } else {
            shop.open(shopId, player);
        }
        return true;
    }
}
