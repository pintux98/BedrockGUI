package it.pintux.life.velocity.utils;

import com.velocitypowered.api.proxy.Player;
import it.pintux.life.common.utils.FormPlayer;
import net.kyori.adventure.text.Component;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

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
        try {
            String cmd = action == null ? "" : action.trim();
            if (cmd.isEmpty()) return false;
            player.getCurrentServer().ifPresent(serverConnection -> {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF(player.getUniqueId().toString());
                out.writeUTF(cmd.startsWith("/") ? cmd.substring(1) : cmd);
                serverConnection.sendPluginMessage(
                        new com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier("bedrockgui:cmd"),
                        out.toByteArray()
                );
            });
            return true;
        } catch (Exception e) {
            return false;
        }
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
