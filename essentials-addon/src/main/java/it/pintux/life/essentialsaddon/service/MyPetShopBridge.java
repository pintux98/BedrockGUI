package it.pintux.life.essentialsaddon.service;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.types.ShopService;
import it.pintux.life.essentialsaddon.model.PetBuyResult;
import it.pintux.life.essentialsaddon.model.ShopPetView;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Bridges MyPet's pet shop. Listing is read directly from pet-shops.yml (stable,
 * documented schema). Buying reflects into MyPet's plugin-internal shop classes
 * (de.Keyle.MyPet.util.shop.*), which are NOT on the API classpath, so the method
 * names are resolved reflectively and the buy FAILS CLOSED if they cannot be found.
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

    // ---------------- Buy (reflective, fails closed) ----------------

    static PetBuyResult buy(Player player, String shopId, String petId, Logger logger) {
        Optional<Object> managerOpt = shopManager();
        if (managerOpt.isEmpty()) {
            return PetBuyResult.fail("shop service unavailable");
        }
        Object manager = managerOpt.get();
        Object shop = (shopId == null || shopId.isBlank())
                ? tryInvoke(manager, "getDefaultShop", new Object[]{})
                : tryInvoke(manager, "getShop", new Object[]{shopId});
        if (shop == null) {
            return PetBuyResult.fail("shop not found");
        }
        Object pet = tryInvoke(shop, "getPet", new Object[]{petId});
        if (pet == null) {
            return PetBuyResult.fail("pet not found");
        }
        Object result = tryInvoke(shop, "buy", new Object[]{player, pet});
        if (result == null) {
            result = tryInvoke(pet, "buy", new Object[]{player});
        }
        if (result instanceof Boolean bool) {
            return bool ? PetBuyResult.ok() : PetBuyResult.fail("purchase declined");
        }
        return PetBuyResult.fail("purchase API unavailable");
    }

    private static Optional<Object> shopManager() {
        try {
            // First try the API-level ShopService (de.Keyle.MyPet.api.util.service.types.ShopService).
            // getService(Class<? extends T>) needs T extends ServiceContainer — ShopService satisfies that.
            Optional<ShopService> svc = MyPetApi.getServiceManager().getService(ShopService.class);
            if (svc.isPresent()) {
                return Optional.of(svc.get());
            }
            // Fall back: try the internal ShopManager by service name via getService(String).
            // de.Keyle.MyPet.util.shop.ShopManager may register itself under its simple name.
            Optional<ServiceContainer> named =
                    MyPetApi.getServiceManager().getService("ShopManager");
            return named.map(s -> (Object) s);
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    private static Object tryInvoke(Object target, String method, Object[] args) {
        if (target == null) {
            return null;
        }
        try {
            Method m = findMethod(target.getClass(), method, args.length);
            if (m == null) {
                return null;
            }
            m.setAccessible(true);
            return m.invoke(target, args);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Method findMethod(Class<?> type, String name, int paramCount) {
        for (Method m : type.getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == paramCount) {
                return m;
            }
        }
        return null;
    }
}
