package it.pintux.life.velocity.platform;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import it.pintux.life.common.platform.PlatformPlayerManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.velocity.utils.VelocityPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class VelocityPlayerManager implements PlatformPlayerManager {

    private final ProxyServer server;

    public VelocityPlayerManager(ProxyServer server) {
        this.server = server;
    }

    @Override
    public Object getPlayer(String playerName) {
        Optional<Player> player = server.getPlayer(playerName);
        return player.orElse(null);
    }

    @Override
    public Object getOfflinePlayer(String playerName) {
        // Velocity doesn't have offline player storage like Bukkit
        // Return the online player if available, otherwise null
        return getPlayer(playerName);
    }

    @Override
    public boolean isPlayerOnline(String playerName) {
        return server.getPlayer(playerName).isPresent();
    }

    @Override
    public void sendMessage(String playerName, String message) {
        server.getPlayer(playerName).ifPresent(player ->
                player.sendMessage(net.kyori.adventure.text.Component.text(message))
        );
    }

    @Override
    public void sendMessage(Object player, String message) {
        if (player instanceof Player) {
            ((Player) player).sendMessage(net.kyori.adventure.text.Component.text(message));
        }
    }

    @Override
    public String getPlayerName(Object player) {
        if (player instanceof Player) {
            return ((Player) player).getUsername();
        }
        return "";
    }

    @Override
    public Object getPlayerWorld(Object player) {
        // Velocity doesn't have world concept - players are on servers
        if (player instanceof Player) {
            return ((Player) player).getCurrentServer().orElse(null);
        }
        return null;
    }

    @Override
    public Object getPlayerLocation(Object player) {
        // Velocity doesn't track player locations - this is backend-specific
        return null;
    }

    @Override
    public FormPlayer toFormPlayer(Object player) {
        if (player instanceof Player) {
            return new VelocityPlayer((Player) player);
        }
        return null;
    }

    @Override
    public Object fromFormPlayer(FormPlayer formPlayer) {
        if (formPlayer instanceof VelocityPlayer) {
            return ((VelocityPlayer) formPlayer).getPlayer();
        }
        return null;
    }

    @Override
    public String getWorldName(Object world) {
        // Velocity doesn't have world names - return server name if available
        if (world instanceof ServerConnection) {
            return ((ServerConnection) world).getServerInfo().getName();
        }
        return "";
    }
}