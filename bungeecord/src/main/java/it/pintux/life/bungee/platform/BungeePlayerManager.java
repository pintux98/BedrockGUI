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

    public BungeePlayerManager(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public Object getPlayer(String playerName) {
        return proxy.getPlayer(playerName);
    }

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
    public void sendByteArray(FormPlayer player, String channel, byte[] data) {
        if (player instanceof BungeePlayer) {
            ProxiedPlayer p = ((BungeePlayer) player).getPlayer();
            if (p.getServer() != null) {
                p.getServer().sendData(channel, data);
            }
        }
    }
}