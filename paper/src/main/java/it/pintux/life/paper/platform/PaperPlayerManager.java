package it.pintux.life.paper.platform;

import it.pintux.life.common.platform.PlatformPlayerManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.paper.utils.PaperPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Paper implementation of PlatformPlayerManager using Bukkit API.
 */
public class PaperPlayerManager implements PlatformPlayerManager {

    public Object getPlayerByName(String name) {
        return Bukkit.getPlayer(name);
    }

    public Object getPlayerByUUID(String uuid) {
        try {
            UUID playerUUID = UUID.fromString(uuid);
            return Bukkit.getPlayer(playerUUID);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Object getOfflinePlayerByName(String name) {
        return Bukkit.getOfflinePlayer(name);
    }

    public Object getOfflinePlayerByUUID(String uuid) {
        try {
            UUID playerUUID = UUID.fromString(uuid);
            return Bukkit.getOfflinePlayer(playerUUID);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public boolean isPlayerOnline(String name) {
        Player player = Bukkit.getPlayer(name);
        return player != null && player.isOnline();
    }

    public boolean isPlayerOnlineByUUID(String uuid) {
        try {
            UUID playerUUID = UUID.fromString(uuid);
            Player player = Bukkit.getPlayer(playerUUID);
            return player != null && player.isOnline();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public void sendMessage(Object player, String message) {
        if (player instanceof Player) {
            ((Player) player).sendMessage(message);
        }
    }

    @Override
    public void sendMessage(String playerName, String message) {
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            player.sendMessage(message);
        }
    }

    public boolean hasPermission(Object player, String permission) {
        if (player instanceof Player) {
            return ((Player) player).hasPermission(permission);
        }
        return false;
    }

    public boolean hasPermission(String playerName, String permission) {
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            return player.hasPermission(permission);
        }
        return false;
    }

    @Override
    public FormPlayer toFormPlayer(Object player) {
        if (player instanceof Player) {
            return new PaperPlayer((Player) player);
        }
        return null;
    }

    @Override
    public Object fromFormPlayer(FormPlayer formPlayer) {
        if (formPlayer instanceof PaperPlayer) {
            return ((PaperPlayer) formPlayer).getBukkitPlayer();
        }
        return null;
    }

    @Override
    public Object getPlayer(String playerName) {
        return Bukkit.getPlayer(playerName);
    }

    @Override
    public Object getOfflinePlayer(String playerName) {
        return Bukkit.getOfflinePlayer(playerName);
    }

    @Override
    public String getPlayerName(Object player) {
        if (player instanceof Player) {
            return ((Player) player).getName();
        }
        return null;
    }

    @Override
    public Object getPlayerWorld(Object player) {
        if (player instanceof Player) {
            return ((Player) player).getWorld();
        }
        return null;
    }

    @Override
    public Object getPlayerLocation(Object player) {
        if (player instanceof Player) {
            return ((Player) player).getLocation();
        }
        return null;
    }

    @Override
    public String getWorldName(Object world) {
        if (world instanceof org.bukkit.World) {
            return ((org.bukkit.World) world).getName();
        }
        return null;
    }
}