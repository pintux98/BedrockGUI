package it.pintux.life.paper.platform;

import it.pintux.life.common.platform.PlatformPlayerManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.paper.utils.PaperPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;


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
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            player.sendMessage(message);
        }
    }

    @Override
    public Object getPlayer(String playerName) {
        return Bukkit.getPlayer(playerName);
    }

    @Override
    public void sendByteArray(FormPlayer player, String channel, byte[] data) {
        if (player instanceof PaperPlayer) {
            Player bukkitPlayer = ((PaperPlayer) player).getBukkitPlayer();
            if (bukkitPlayer != null && bukkitPlayer.isOnline()) {
                bukkitPlayer.sendPluginMessage(plugin, channel, data);
            }
        }
    }
}
