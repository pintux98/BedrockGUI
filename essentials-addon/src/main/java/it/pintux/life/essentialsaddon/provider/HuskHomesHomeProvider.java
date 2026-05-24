package it.pintux.life.essentialsaddon.provider;

import it.pintux.life.essentialsaddon.api.HomeProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class HuskHomesHomeProvider implements HomeProvider {
    private final Object api;
    private final Method adaptUser, getUserHomes, getHome, createHome, deleteHome,
            teleportBuilder, getMaxHomeSlots;

    public HuskHomesHomeProvider() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("HuskHomes");
        if (plugin == null) throw new IllegalStateException("HuskHomes not found");
        try {
            ClassLoader cl = plugin.getClass().getClassLoader();
            Class<?> apiClass = cl.loadClass("net.william278.huskhomes.api.HuskHomesAPI");
            this.api = apiClass.getMethod("getInstance").invoke(null);
            this.adaptUser = apiClass.getMethod("adaptUser", Player.class);
            Class<?> userClass = cl.loadClass("net.william278.huskhomes.user.User");
            Class<?> onlineUserClass = cl.loadClass("net.william278.huskhomes.user.OnlineUser");
            Class<?> posClass = cl.loadClass("net.william278.huskhomes.position.Position");
            this.getUserHomes = apiClass.getMethod("getUserHomes", userClass);
            this.getHome = apiClass.getMethod("getHome", userClass, String.class);
            this.createHome = apiClass.getMethod("createHome", userClass, String.class, posClass);
            this.deleteHome = apiClass.getMethod("deleteHome", userClass, String.class);
            this.teleportBuilder = apiClass.getMethod("teleportBuilder", onlineUserClass);
            this.getMaxHomeSlots = apiClass.getMethod("getMaxHomeSlots", onlineUserClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize HuskHomesHomeProvider", e);
        }
    }

    @Override
    public String getProviderId() { return "HuskHomes"; }
    @Override
    public boolean isReady() { return api != null; }

    @Override
    public List<String> getHomeNames(Player player) {
        try {
            Object user = adaptUser.invoke(api, player);
            List<?> homes = (List<?>) ((CompletableFuture<?>) getUserHomes.invoke(api, user)).get(5, TimeUnit.SECONDS);
            List<String> names = new ArrayList<>();
            for (Object home : homes) {
                names.add((String) home.getClass().getMethod("getName").invoke(home));
            }
            return names;
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public Location getHomeLocation(Player player, String homeName) {
        try {
            Object user = adaptUser.invoke(api, player);
            Optional<?> opt = (Optional<?>) ((CompletableFuture<?>) getHome.invoke(api, user, homeName)).get(5, TimeUnit.SECONDS);
            if (opt.isEmpty()) return null;
            return toBukkitLocation(getPosition(opt.get()));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean teleportHome(Player player, String homeName) {
        try {
            Object user = adaptUser.invoke(api, player);
            Optional<?> opt = (Optional<?>) ((CompletableFuture<?>) getHome.invoke(api, user, homeName)).get(5, TimeUnit.SECONDS);
            if (opt.isEmpty()) return false;
            Object position = getPosition(opt.get());
            ClassLoader cl = api.getClass().getClassLoader();
            Class<?> targetClass = cl.loadClass("net.william278.huskhomes.teleport.Target");
            Method targetPos = targetClass.getMethod("position", cl.loadClass("net.william278.huskhomes.position.Position"));
            Object builder = teleportBuilder.invoke(api, user);
            builder.getClass().getMethod("target", targetClass).invoke(builder, targetPos.invoke(null, position));
            builder.getClass().getMethod("buildAndComplete", boolean.class).invoke(builder, false);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean setHome(Player player, String homeName) {
        try {
            Object user = adaptUser.invoke(api, player);
            Object position = user.getClass().getMethod("getPosition").invoke(user);
            createHome.invoke(api, user, homeName, position);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean deleteHome(Player player, String homeName) {
        try {
            Object user = adaptUser.invoke(api, player);
            deleteHome.invoke(api, user, homeName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int getMaxHomes(Player player) {
        try {
            Object user = adaptUser.invoke(api, player);
            return (int) getMaxHomeSlots.invoke(api, user);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int getHomeCount(Player player) { return getHomeNames(player).size(); }

    private Object getPosition(Object savedPosition) throws Exception {
        return savedPosition.getClass().getMethod("getPosition").invoke(savedPosition);
    }

    private Location toBukkitLocation(Object position) throws Exception {
        Class<?> posClass = position.getClass();
        Object world = posClass.getMethod("getWorld").invoke(position);
        Class<?> worldClass = world.getClass();
        String worldName = (String) worldClass.getMethod("getName").invoke(world);
        org.bukkit.World bukkitWorld = Bukkit.getWorld(worldName);
        if (bukkitWorld == null) {
            UUID uuid = (UUID) worldClass.getMethod("getUuid").invoke(world);
            if (uuid != null) bukkitWorld = Bukkit.getWorld(uuid);
        }
        if (bukkitWorld == null) return null;
        return new Location(bukkitWorld,
                (double) posClass.getMethod("getX").invoke(position),
                (double) posClass.getMethod("getY").invoke(position),
                (double) posClass.getMethod("getZ").invoke(position),
                (float) posClass.getMethod("getYaw").invoke(position),
                (float) posClass.getMethod("getPitch").invoke(position));
    }
}
