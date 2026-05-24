package it.pintux.life.essentialsaddon.provider;

import it.pintux.life.essentialsaddon.api.KitProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.*;

@SuppressWarnings("unchecked")
public final class CMIKitProvider implements KitProvider {

    private Object cmiInstance;
    private Method getKitsManagerMethod;

    @Override
    public String getProviderId() {
        return "cmi";
    }

    @Override
    public boolean isReady() {
        if (cmiInstance != null) {
            return true;
        }
        Plugin plugin = Bukkit.getPluginManager().getPlugin("CMI");
        if (plugin == null || !plugin.isEnabled()) {
            return false;
        }
        try {
            cmiInstance = plugin;
            getKitsManagerMethod = cmiInstance.getClass().getMethod("getKitsManager");
            Object kitsManager = getKitsManagerMethod.invoke(cmiInstance);
            return kitsManager != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Collection<String> getKitNames() {
        if (!isReady()) {
            return List.of();
        }
        try {
            Object kitsManager = getKitsManagerMethod.invoke(cmiInstance);
            Method getKits = kitsManager.getClass().getMethod("getKits");
            Object kitsList = getKits.invoke(kitsManager);
            if (kitsList instanceof Collection) {
                List<String> names = new ArrayList<>();
                for (Object kit : (Collection<?>) kitsList) {
                    Method getName = kit.getClass().getMethod("getName");
                    names.add((String) getName.invoke(kit));
                }
                return names;
            }
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public boolean hasAccess(Player player, String kitName) {
        if (!isReady()) {
            return false;
        }
        if (player.hasPermission("cmi.kit.*") || player.hasPermission("cmi.kit." + kitName)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean claimKit(Player player, String kitName) {
        if (!isReady()) {
            return false;
        }
        try {
            Object kitsManager = getKitsManagerMethod.invoke(cmiInstance);
            Method getKitByName = kitsManager.getClass().getMethod("getKitByName", String.class);
            Object kit = getKitByName.invoke(kitsManager, kitName);
            if (kit == null) {
                return false;
            }
            Method giveKit = kitsManager.getClass().getMethod("giveKit", Player.class, kit.getClass());
            return Boolean.TRUE.equals(giveKit.invoke(kitsManager, player, kit));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getDisplayName(String kitName) {
        if (!isReady()) {
            return kitName;
        }
        try {
            Object kitsManager = getKitsManagerMethod.invoke(cmiInstance);
            Method getKitByName = kitsManager.getClass().getMethod("getKitByName", String.class);
            Object kit = getKitByName.invoke(kitsManager, kitName);
            if (kit != null) {
                Method getDisplayName = kit.getClass().getMethod("getDisplayName");
                String name = (String) getDisplayName.invoke(kit);
                if (name != null && !name.isEmpty()) {
                    return name;
                }
            }
        } catch (Exception ignored) {
        }
        return kitName;
    }

    @Override
    public int getItemCount(String kitName) {
        if (!isReady()) {
            return -1;
        }
        try {
            Object kitsManager = getKitsManagerMethod.invoke(cmiInstance);
            Method getKitByName = kitsManager.getClass().getMethod("getKitByName", String.class);
            Object kit = getKitByName.invoke(kitsManager, kitName);
            if (kit == null) {
                return -1;
            }
            Method getInventory = kit.getClass().getMethod("getInventory");
            Object inventory = getInventory.invoke(kit);
            Method getContents = inventory.getClass().getMethod("getContents");
            Object[] contents = (Object[]) getContents.invoke(inventory);
            int count = 0;
            for (Object item : contents) {
                if (item != null) count++;
            }
            return count;
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public long getCooldownSeconds(Player player, String kitName) {
        if (!isReady()) {
            return 0;
        }
        try {
            Object kitsManager = getKitsManagerMethod.invoke(cmiInstance);
            Method getKitByName = kitsManager.getClass().getMethod("getKitByName", String.class);
            Object kit = getKitByName.invoke(kitsManager, kitName);
            if (kit == null) {
                return 0;
            }
            Method getCooldown = kitsManager.getClass().getMethod("getCooldown", Player.class, kit.getClass());
            Object result = getCooldown.invoke(kitsManager, player, kit);
            if (result instanceof Number) {
                return Math.max(0, ((Number) result).longValue());
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean isAvailable(Player player, String kitName) {
        return getCooldownSeconds(player, kitName) <= 0;
    }
}
