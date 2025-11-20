package it.pintux.life.velocity.utils;

import com.velocitypowered.api.proxy.Player;
import it.pintux.life.common.utils.FormPlayer;
import net.kyori.adventure.text.Component;

import java.util.UUID;

public class VelocityPlayer implements FormPlayer {

    private final Player player;

    public VelocityPlayer(Player player) {
        this.player = player;
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public String getName() {
        return player.getUsername();
    }

    @Override
    public void sendMessage(String message) {
        player.sendMessage(Component.text(message));
    }

    @Override
    public boolean executeAction(String action) {
        // Actions would be executed on the backend server
        // This is a simplified implementation
        return false;
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }

    public boolean isOnline() {
        return player.isActive();
    }

    public Player getPlayer() {
        return player;
    }

    public String getServerName() {
        return player.getCurrentServer()
                .map(serverConnection -> serverConnection.getServerInfo().getName())
                .orElse("unknown");
    }
}