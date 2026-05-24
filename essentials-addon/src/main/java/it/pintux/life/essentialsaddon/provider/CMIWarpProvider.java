package it.pintux.life.essentialsaddon.provider;

import it.pintux.life.essentialsaddon.api.WarpProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.*;

public final class CMIWarpProvider implements WarpProvider {

    private Object cmiInstance;
    private Method getWarpManagerMethod;

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
            getWarpManagerMethod = cmiInstance.getClass().getMethod("getWarpManager");
            Object warpManager = getWarpManagerMethod.invoke(cmiInstance);
            return warpManager != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Collection<String> getWarpNames() {
        if (!isReady()) {
            return List.of();
        }
        try {
            Object warpManager = getWarpManagerMethod.invoke(cmiInstance);
            Method getWarps = warpManager.getClass().getMethod("getWarps");
            Object warpsMap = getWarps.invoke(warpManager);
            if (warpsMap instanceof Map) {
                return new ArrayList<>(((Map<?, ?>) warpsMap).keySet().stream()
                        .map(Object::toString)
                        .toList());
            }
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public Location getWarpLocation(String warpName) {
        if (!isReady()) {
            return null;
        }
        try {
            Object warpManager = getWarpManagerMethod.invoke(cmiInstance);
            Method getWarp = warpManager.getClass().getMethod("getWarp", String.class);
            Object warp = getWarp.invoke(warpManager, warpName);
            if (warp == null) {
                return null;
            }
            Method getLocation = warp.getClass().getMethod("getLocation");
            return (Location) getLocation.invoke(warp);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean hasAccess(Player player, String warpName) {
        if (!isReady()) {
            return false;
        }
        if (player.hasPermission("cmi.command.warp.*") || player.hasPermission("cmi.command.warp." + warpName)) {
            return true;
        }
        try {
            Object warpManager = getWarpManagerMethod.invoke(cmiInstance);
            Method getWarp = warpManager.getClass().getMethod("getWarp", String.class);
            Object warp = getWarp.invoke(warpManager, warpName);
            if (warp != null) {
                Method isReqPermission = warp.getClass().getMethod("isReqPermission");
                if (Boolean.TRUE.equals(isReqPermission.invoke(warp))) {
                    return player.hasPermission("cmi.command.warp." + warpName);
                }
            }
        } catch (Exception ignored) {
        }
        return true;
    }

    @Override
    public boolean teleport(Player player, String warpName) {
        if (!isReady()) {
            return false;
        }
        Location location = getWarpLocation(warpName);
        if (location == null) {
            return false;
        }
        try {
            player.teleport(location);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getDisplayName(String warpName) {
        if (!isReady()) {
            return warpName;
        }
        try {
            Object warpManager = getWarpManagerMethod.invoke(cmiInstance);
            Method getWarp = warpManager.getClass().getMethod("getWarp", String.class);
            Object warp = getWarp.invoke(warpManager, warpName);
            if (warp != null) {
                Method getDisplayName = warp.getClass().getMethod("getDisplayName");
                String name = (String) getDisplayName.invoke(warp);
                if (name != null && !name.isEmpty()) {
                    return name;
                }
            }
        } catch (Exception ignored) {
        }
        return warpName;
    }
}
