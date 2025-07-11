package it.pintux.life.bungee.platform;

import it.pintux.life.common.platform.PlatformCommandExecutor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Bungeecord implementation of PlatformCommandExecutor using BungeeCord API.
 */
public class BungeeCommandExecutor implements PlatformCommandExecutor {
    
    @Override
    public boolean executeAsConsole(String command) {
        try {
            ProxyServer.getInstance().getPluginManager().dispatchCommand(ProxyServer.getInstance().getConsole(), command);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean executeAsPlayer(String playerName, String command) {
        try {
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(playerName);
            if (player != null && player.isConnected()) {
                ProxyServer.getInstance().getPluginManager().dispatchCommand(player, command);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean commandExists(String command) {
        try {
            String[] parts = command.split(" ");
            String baseCommand = parts[0];
            // BungeeCord doesn't provide a direct way to check command existence
            // We'll return true as a fallback since commands are registered dynamically
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}