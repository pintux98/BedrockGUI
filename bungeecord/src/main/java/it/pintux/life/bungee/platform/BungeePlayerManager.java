package it.pintux.life.bungee.platform;

import it.pintux.life.common.platform.PlatformPlayerManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.bungee.utils.BungeePlayer;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;

public class BungeePlayerManager implements PlatformPlayerManager {
    private final ProxyServer proxy;
    public BungeePlayerManager(ProxyServer proxy) { this.proxy = proxy; }

    @Override
    public Object getPlayer(String playerName) { return proxy.getPlayer(playerName); }

    @Override
    public Object getOfflinePlayer(String playerName) { return getPlayer(playerName); }

    @Override
    public boolean isPlayerOnline(String playerName) { return proxy.getPlayer(playerName) != null; }

    @Override
    public void sendMessage(String playerName, String message) {
        ProxiedPlayer p = proxy.getPlayer(playerName);
        if (p != null) p.sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
    }

    @Override
    public void sendMessage(Object player, String message) {
        if (player instanceof ProxiedPlayer) {
            ((ProxiedPlayer) player).sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
        }
    }

    @Override
    public String getPlayerName(Object player) { return player instanceof ProxiedPlayer ? ((ProxiedPlayer) player).getName() : ""; }

    @Override
    public Object getPlayerWorld(Object player) { return null; }

    @Override
    public Object getPlayerLocation(Object player) { return null; }

    @Override
    public FormPlayer toFormPlayer(Object player) { return player instanceof ProxiedPlayer ? new BungeePlayer((ProxiedPlayer) player) : null; }

    @Override
    public Object fromFormPlayer(FormPlayer formPlayer) { return formPlayer instanceof BungeePlayer ? ((BungeePlayer) formPlayer).getPlayer() : null; }

    @Override
    public String getWorldName(Object world) { return ""; }

    @Override
    public void sendByteArray(FormPlayer player, String channel, byte[] data) {
        if (player instanceof BungeePlayer) {
            ProxiedPlayer p = ((BungeePlayer) player).getPlayer();
            if (p.getServer() != null) {
                p.getServer().sendData(channel, data);
            }
        }
    }
}