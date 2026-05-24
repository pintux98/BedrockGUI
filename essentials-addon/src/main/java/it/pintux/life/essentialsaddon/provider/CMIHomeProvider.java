package it.pintux.life.essentialsaddon.provider;

import it.pintux.life.essentialsaddon.api.HomeProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.*;

public final class CMIHomeProvider implements HomeProvider {

    private Object cmiInstance;
    private Method getPlayerManagerMethod;

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
            getPlayerManagerMethod = cmiInstance.getClass().getMethod("getPlayerManager");
            return getPlayerManagerMethod.invoke(cmiInstance) != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<String> getHomeNames(Player player) {
        if (!isReady()) return List.of();
        try {
            Object playerManager = getPlayerManagerMethod.invoke(cmiInstance);
            Method getUser = playerManager.getClass().getMethod("getUser", Player.class);
            Object user = getUser.invoke(playerManager, player);
            if (user == null) return List.of();
            Method getHomes = user.getClass().getMethod("getHomes");
            Object homes = getHomes.invoke(user);
            if (homes instanceof Map) {
                return new ArrayList<>(((Map<?, ?>) homes).keySet().stream()
                        .map(Object::toString)
                        .toList());
            }
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public Location getHomeLocation(Player player, String homeName) {
        if (!isReady()) return null;
        try {
            Object playerManager = getPlayerManagerMethod.invoke(cmiInstance);
            Method getUser = playerManager.getClass().getMethod("getUser", Player.class);
            Object user = getUser.invoke(playerManager, player);
            if (user == null) return null;
            Method getHome = user.getClass().getMethod("getHome", String.class);
            Object home = getHome.invoke(user, homeName);
            if (home == null) return null;
            Method getLocation = home.getClass().getMethod("getLocation");
            return (Location) getLocation.invoke(home);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean teleportHome(Player player, String homeName) {
        if (!isReady()) return false;
        Location loc = getHomeLocation(player, homeName);
        if (loc == null) return false;
        try {
            player.teleport(loc);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean setHome(Player player, String homeName) {
        if (!isReady()) return false;
        try {
            Object playerManager = getPlayerManagerMethod.invoke(cmiInstance);
            Method getUser = playerManager.getClass().getMethod("getUser", Player.class);
            Object user = getUser.invoke(playerManager, player);
            if (user == null) return false;
            Method addHome = user.getClass().getMethod("addHome", String.class, Location.class);
            addHome.invoke(user, homeName, player.getLocation());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean deleteHome(Player player, String homeName) {
        if (!isReady()) return false;
        try {
            Object playerManager = getPlayerManagerMethod.invoke(cmiInstance);
            Method getUser = playerManager.getClass().getMethod("getUser", Player.class);
            Object user = getUser.invoke(playerManager, player);
            if (user == null) return false;
            Method removeHome = user.getClass().getMethod("removeHome", String.class);
            removeHome.invoke(user, homeName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int getMaxHomes(Player player) {
        if (!isReady()) return 0;
        try {
            Object playerManager = getPlayerManagerMethod.invoke(cmiInstance);
            Method getUser = playerManager.getClass().getMethod("getUser", Player.class);
            Object user = getUser.invoke(playerManager, player);
            if (user == null) return 0;
            Method getMaxHomes = user.getClass().getMethod("getMaxHomes");
            Object result = getMaxHomes.invoke(user);
            if (result instanceof Number) {
                return ((Number) result).intValue();
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int getHomeCount(Player player) {
        return getHomeNames(player).size();
    }
}
