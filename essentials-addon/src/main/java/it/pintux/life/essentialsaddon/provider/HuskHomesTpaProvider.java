package it.pintux.life.essentialsaddon.provider;

import it.pintux.life.essentialsaddon.api.TpaProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;

public final class HuskHomesTpaProvider implements TpaProvider {
    private final Object api;
    private final Method adaptUser, teleportBuilder;

    public HuskHomesTpaProvider() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("HuskHomes");
        if (plugin == null) throw new IllegalStateException("HuskHomes not found");
        try {
            ClassLoader cl = plugin.getClass().getClassLoader();
            Class<?> apiClass = cl.loadClass("net.william278.huskhomes.api.HuskHomesAPI");
            this.api = apiClass.getMethod("getInstance").invoke(null);
            this.adaptUser = apiClass.getMethod("adaptUser", Player.class);
            Class<?> onlineUserClass = cl.loadClass("net.william278.huskhomes.user.OnlineUser");
            this.teleportBuilder = apiClass.getMethod("teleportBuilder", onlineUserClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize HuskHomesTpaProvider", e);
        }
    }

    @Override
    public String getProviderId() { return "HuskHomes"; }
    @Override
    public boolean isReady() { return api != null; }

    @Override
    public boolean sendTpaRequest(Player sender, Player target) {
        try {
            Object senderUser = adaptUser.invoke(api, sender);
            ClassLoader cl = api.getClass().getClassLoader();
            Class<?> targetClass = cl.loadClass("net.william278.huskhomes.teleport.Target");
            Method targetUsername = targetClass.getMethod("username", String.class);
            Object builder = teleportBuilder.invoke(api, senderUser);
            builder.getClass().getMethod("target", targetClass).invoke(builder, targetUsername.invoke(null, target.getName()));
            builder.getClass().getMethod("toTimedTeleport").invoke(builder);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean sendTpahereRequest(Player sender, Player target) {
        try {
            Object targetUser = adaptUser.invoke(api, target);
            ClassLoader cl = api.getClass().getClassLoader();
            Class<?> targetClass = cl.loadClass("net.william278.huskhomes.teleport.Target");
            Method targetUsername = targetClass.getMethod("username", String.class);
            Object builder = teleportBuilder.invoke(api, targetUser);
            builder.getClass().getMethod("target", targetClass).invoke(builder, targetUsername.invoke(null, sender.getName()));
            builder.getClass().getMethod("toTimedTeleport").invoke(builder);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean acceptTpa(Player target) { return false; }
    @Override
    public boolean denyTpa(Player target) { return false; }
    @Override
    public boolean cancelTpa(Player sender) { return false; }
    @Override
    public List<String> getPendingRequests(Player player) { return List.of(); }
    @Override
    public boolean hasPendingRequest(Player player) { return false; }
    @Override
    public String getPendingRequestSender(Player player) { return null; }
}
