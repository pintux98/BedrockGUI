package it.pintux.life.essentialsaddon.provider;

import it.pintux.life.essentialsaddon.api.HomeProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class HuskHomesHomeProvider implements HomeProvider {
    private static final long FUTURE_TIMEOUT_SECONDS = 5L;

    private final Logger logger;
    private final Object api;
    private final ClassLoader huskLoader;
    private final Method adaptUser;
    private final Method getUserHomes;
    private final Method getHome;
    private final Method createHome;
    private final Method deleteHome;
    private final Method teleportBuilder;
    private final Method getMaxHomeSlots;

    private volatile String lastFailure;

    public HuskHomesHomeProvider(Logger logger) {
        this.logger = logger;
        Plugin plugin = Bukkit.getPluginManager().getPlugin("HuskHomes");
        if (plugin == null) throw new IllegalStateException("HuskHomes not found");
        try {
            this.huskLoader = plugin.getClass().getClassLoader();
            Class<?> apiClass = huskLoader.loadClass("net.william278.huskhomes.api.HuskHomesAPI");
            this.api = apiClass.getMethod("getInstance").invoke(null);
            Class<?> userClass = huskLoader.loadClass("net.william278.huskhomes.user.User");
            Class<?> onlineUserClass = huskLoader.loadClass("net.william278.huskhomes.user.OnlineUser");
            Class<?> posClass = huskLoader.loadClass("net.william278.huskhomes.position.Position");

            this.adaptUser = require(apiClass, "adaptUser", new Class<?>[]{Player.class});
            this.getUserHomes = require(apiClass, "getUserHomes", new Class<?>[]{userClass});
            // Renamed across HuskHomes releases, and none of these are needed to list homes, so a
            // missing one only disables its own operation instead of the whole provider.
            this.getHome = optional(apiClass, new String[]{"getUserHome", "getHome"},
                    new Class<?>[]{userClass, String.class});
            this.createHome = optional(apiClass, new String[]{"createHome"},
                    new Class<?>[]{userClass, String.class, posClass});
            this.deleteHome = optional(apiClass, new String[]{"deleteHome"},
                    new Class<?>[]{userClass, String.class});
            this.teleportBuilder = optional(apiClass, new String[]{"teleportBuilder"},
                    new Class<?>[]{onlineUserClass});
            this.getMaxHomeSlots = optional(apiClass, new String[]{"getMaxHomeSlots", "getMaxHomes"},
                    new Class<?>[]{onlineUserClass});
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize HuskHomesHomeProvider", e);
        }
    }

    @Override
    public String getProviderId() { return "HuskHomes"; }

    @Override
    public boolean isReady() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("HuskHomes");
        return api != null && plugin != null && plugin.isEnabled();
    }

    @Override
    public List<String> getHomeNames(Player player) {
        try {
            Object user = adaptUser.invoke(api, player);
            Object resolved = HuskHomesValues.await(getUserHomes.invoke(api, user), "getUserHomes", FUTURE_TIMEOUT_SECONDS);
            List<String> names = new ArrayList<>();
            for (Object home : HuskHomesValues.asList(resolved)) {
                String name = HuskHomesValues.homeName(home);
                if (name != null) {
                    names.add(name);
                }
            }
            if (names.isEmpty() && !HuskHomesValues.asList(resolved).isEmpty()) {
                report("getUserHomes returned " + HuskHomesValues.asList(resolved).size()
                        + " home(s) but none exposed a readable name", null);
            }
            return names;
        } catch (Throwable failure) {
            report("Could not read homes for " + player.getName(), failure);
            return List.of();
        }
    }

    @Override
    public Location getHomeLocation(Player player, String homeName) {
        Object home = findHome(player, homeName);
        if (home == null) return null;
        try {
            return toBukkitLocation(positionOf(home));
        } catch (Throwable failure) {
            report("Could not read the location of home '" + homeName + "'", failure);
            return null;
        }
    }

    @Override
    public boolean teleportHome(Player player, String homeName) {
        if (teleportBuilder == null) {
            report("HuskHomes exposes no teleportBuilder; cannot teleport to '" + homeName + "'", null);
            return false;
        }
        Object home = findHome(player, homeName);
        if (home == null) return false;
        try {
            Object user = adaptUser.invoke(api, player);
            Class<?> targetClass = huskLoader.loadClass("net.william278.huskhomes.teleport.Target");
            Method targetPos = targetClass.getMethod("position",
                    huskLoader.loadClass("net.william278.huskhomes.position.Position"));
            Object builder = teleportBuilder.invoke(api, user);
            builder.getClass().getMethod("target", targetClass)
                    .invoke(builder, targetPos.invoke(null, positionOf(home)));
            builder.getClass().getMethod("buildAndComplete", boolean.class).invoke(builder, false);
            return true;
        } catch (Throwable failure) {
            report("Could not teleport " + player.getName() + " to home '" + homeName + "'", failure);
            return false;
        }
    }

    @Override
    public boolean setHome(Player player, String homeName) {
        if (createHome == null) {
            report("HuskHomes exposes no createHome; cannot set '" + homeName + "'", null);
            return false;
        }
        try {
            Object user = adaptUser.invoke(api, player);
            Object position = user.getClass().getMethod("getPosition").invoke(user);
            HuskHomesValues.await(createHome.invoke(api, user, homeName, position), "createHome", FUTURE_TIMEOUT_SECONDS);
            return true;
        } catch (Throwable failure) {
            report("Could not set home '" + homeName + "' for " + player.getName(), failure);
            return false;
        }
    }

    @Override
    public boolean deleteHome(Player player, String homeName) {
        if (deleteHome == null) {
            report("HuskHomes exposes no deleteHome; cannot delete '" + homeName + "'", null);
            return false;
        }
        try {
            Object user = adaptUser.invoke(api, player);
            HuskHomesValues.await(deleteHome.invoke(api, user, homeName), "deleteHome", FUTURE_TIMEOUT_SECONDS);
            return true;
        } catch (Throwable failure) {
            report("Could not delete home '" + homeName + "' for " + player.getName(), failure);
            return false;
        }
    }

    @Override
    public int getMaxHomes(Player player) {
        if (getMaxHomeSlots == null) return 0;
        try {
            Object user = adaptUser.invoke(api, player);
            Object value = HuskHomesValues.await(getMaxHomeSlots.invoke(api, user), "getMaxHomeSlots", FUTURE_TIMEOUT_SECONDS);
            return value instanceof Number number ? number.intValue() : 0;
        } catch (Throwable failure) {
            report("Could not read the home limit for " + player.getName(), failure);
            return 0;
        }
    }

    @Override
    public int getHomeCount(Player player) { return getHomeNames(player).size(); }

    private Object findHome(Player player, String homeName) {
        try {
            Object user = adaptUser.invoke(api, player);
            if (getHome != null) {
                Object resolved = HuskHomesValues.unwrapOptional(HuskHomesValues.await(getHome.invoke(api, user, homeName), "getHome", FUTURE_TIMEOUT_SECONDS));
                if (resolved != null) {
                    return resolved;
                }
            }
            // Fall back to the list, which is the one call this provider always has.
            for (Object home : HuskHomesValues.asList(HuskHomesValues.await(getUserHomes.invoke(api, user), "getUserHomes", FUTURE_TIMEOUT_SECONDS))) {
                String name = HuskHomesValues.homeName(home);
                if (name != null && name.equalsIgnoreCase(homeName)) {
                    return home;
                }
            }
            return null;
        } catch (Throwable failure) {
            report("Could not look up home '" + homeName + "' for " + player.getName(), failure);
            return null;
        }
    }

    private Object positionOf(Object savedPosition) {
        Object position = HuskHomesValues.call(savedPosition, "getPosition");
        // A Home already is a Position in HuskHomes 4.x, so it is its own position.
        return position != null ? position : savedPosition;
    }

    private Location toBukkitLocation(Object position) {
        Object world = HuskHomesValues.call(position, "getWorld");
        if (world == null) {
            return null;
        }
        org.bukkit.World bukkitWorld = null;
        Object worldName = HuskHomesValues.call(world, "getName");
        if (worldName instanceof String name) {
            bukkitWorld = Bukkit.getWorld(name);
        }
        if (bukkitWorld == null) {
            Object uuid = HuskHomesValues.call(world, "getUuid", "getUniqueId");
            if (uuid instanceof UUID id) {
                bukkitWorld = Bukkit.getWorld(id);
            }
        }
        if (bukkitWorld == null) {
            return null;
        }
        Object x = HuskHomesValues.call(position, "getX");
        Object y = HuskHomesValues.call(position, "getY");
        Object z = HuskHomesValues.call(position, "getZ");
        if (!(x instanceof Number) || !(y instanceof Number) || !(z instanceof Number)) {
            return null;
        }
        Object yaw = HuskHomesValues.call(position, "getYaw");
        Object pitch = HuskHomesValues.call(position, "getPitch");
        return new Location(bukkitWorld,
                ((Number) x).doubleValue(), ((Number) y).doubleValue(), ((Number) z).doubleValue(),
                yaw instanceof Number number ? number.floatValue() : 0f,
                pitch instanceof Number number ? number.floatValue() : 0f);
    }

    private Method require(Class<?> apiClass, String name, Class<?>[] parameters) throws Exception {
        Method method = optional(apiClass, new String[]{name}, parameters);
        if (method == null) {
            throw new NoSuchMethodException("HuskHomesAPI." + name);
        }
        return method;
    }

    private Method optional(Class<?> apiClass, String[] names, Class<?>[] parameters) {
        for (String name : names) {
            try {
                Method method = apiClass.getMethod(name, parameters);
                method.setAccessible(true);
                return method;
            } catch (Exception | LinkageError ignored) {
            }
        }
        for (Method method : apiClass.getMethods()) {
            for (String name : names) {
                if (method.getName().equals(name) && method.getParameterCount() == parameters.length) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        logger.warning("HuskHomes exposes none of " + String.join("/", names)
                + " with " + parameters.length + " parameter(s); that operation is unavailable.");
        return null;
    }

    /** Logs each distinct failure once, so a broken call is visible without spamming console. */
    private void report(String message, Throwable failure) {
        String detail = failure == null ? message
                : message + ": " + failure.getClass().getSimpleName()
                        + (failure.getMessage() == null ? "" : " - " + failure.getMessage());
        String key = detail.toLowerCase(Locale.ROOT);
        if (key.equals(lastFailure)) {
            return;
        }
        lastFailure = key;
        if (failure == null) {
            logger.warning("HuskHomes home provider: " + detail);
        } else {
            logger.log(Level.WARNING, "HuskHomes home provider: " + detail, failure);
        }
    }
}
