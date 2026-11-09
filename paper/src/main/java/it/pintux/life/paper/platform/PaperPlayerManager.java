package it.pintux.life.paper.platform;

import it.pintux.life.common.platform.PlatformPlayerManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.paper.utils.PaperPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;



public class PaperPlayerManager implements PlatformPlayerManager {

    private final org.bukkit.plugin.java.JavaPlugin plugin;

    public PaperPlayerManager(org.bukkit.plugin.java.JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void sendMessage(Object player, String message) {
        if (player instanceof FormPlayer formPlayer) {
            formPlayer.sendMessage(message);
        }
    }

    @Override
    public void sendMessage(String playerName, String message) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player != null) {
            player.sendMessage(message);
        }
    }

    @Override
    public boolean broadcast(FormPlayer source, String audience, String target, String message) {
        Player origin = source instanceof PaperPlayer paperPlayer ? paperPlayer.getBukkitPlayer() : null;
        java.util.function.Predicate<Player> filter;
        switch (audience) {
            case "permission" -> filter = p -> p.hasPermission(target);
            case "world" -> filter = p -> p.getWorld().getName().equalsIgnoreCase(target);
            case "radius" -> {
                double radius;
                try {
                    radius = Double.parseDouble(target);
                } catch (NumberFormatException e) {
                    return false;
                }
                if (origin == null) {
                    return false;
                }
                double squared = radius * radius;
                filter = p -> p.getWorld().equals(origin.getWorld()) && p.getLocation().distanceSquared(origin.getLocation()) <= squared;
            }
            default -> filter = p -> true;
        }
        Bukkit.getOnlinePlayers().stream().filter(filter).forEach(p -> p.sendMessage(message));
        return true;
    }

    @Override
    public java.util.List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }

    @Override
    public Object getPlayer(String playerName) {
        return Bukkit.getPlayerExact(playerName);
    }

    @Override
    public boolean sendByteArray(FormPlayer player, String channel, byte[] data) {
        if (player instanceof PaperPlayer) {
            Player bukkitPlayer = ((PaperPlayer) player).getBukkitPlayer();
            if (bukkitPlayer != null && bukkitPlayer.isOnline()) {
                Messenger messenger = Bukkit.getMessenger();
                if (!messenger.isOutgoingChannelRegistered(plugin, channel)) {
                    messenger.registerOutgoingPluginChannel(plugin, channel);
                }
                bukkitPlayer.sendPluginMessage(plugin, channel, data);
                return true;
            }
        }
        return false;
    }
}
