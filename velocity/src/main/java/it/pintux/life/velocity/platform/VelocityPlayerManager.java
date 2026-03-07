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
    public void sendByteArray(FormPlayer player, String channel, byte[] data) {
        if (player instanceof VelocityPlayer) {
            ((VelocityPlayer) player).getPlayer().getCurrentServer().ifPresent(server -> {
                server.sendPluginMessage(new com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier(channel), data);
            });
        }
    }
}