package org.pintux.life.velocity.utils;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import it.pintux.life.common.utils.FormPlayer;

import java.util.UUID;

public class VelocityPlayer implements FormPlayer {

    private final Player player;
    private final ProxyServer proxyServer;

    public VelocityPlayer(Player player, ProxyServer proxyServer) {
        this.player = player;
        this.proxyServer = proxyServer;
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
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        player.sendMessage(component);
    }

    @Override
    public void executeAction(String action) {

    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }

    public void performCommand(String command) {
        player.spoofChatInput("/" + command);
    }

    public String getServerName() {
        return player.getCurrentServer()
                .map(server -> server.getServerInfo().getName())
                .orElse("");
    }

    public void connectToServer(String serverName) {
        proxyServer.getServer(serverName).ifPresentOrElse(
            targetServer -> {
                player.createConnectionRequest(targetServer).connect()
                        .whenComplete((result, throwable) -> {
                            if (throwable != null) {
                                player.sendMessage(Component.text("Failed to connect to server: " + serverName));
                            }
                        });
            },
            () -> player.sendMessage(Component.text("Server not found: " + serverName))
        );
    }

    public Player getPlayer() {
        return player;
    }
}