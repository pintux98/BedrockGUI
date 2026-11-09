package it.pintux.life.bungee.platform;

import it.pintux.life.common.platform.PlatformCommandExecutor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class BungeeCommandExecutor implements PlatformCommandExecutor {
    @Override
    public boolean executeAsConsole(String command) {
        ProxyServer proxy = ProxyServer.getInstance();
        return proxy.getPluginManager().dispatchCommand(proxy.getConsole(), command);
    }

    @Override
    public boolean executeAsPlayer(String playerName, String command) {
        ProxyServer proxy = ProxyServer.getInstance();
        ProxiedPlayer player = proxy.getPlayer(playerName);
        return player != null && proxy.getPluginManager().dispatchCommand(player, command);
    }
}
