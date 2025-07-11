package org.pintux.life.velocity.platform;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.Player;
import it.pintux.life.common.platform.PlatformCommandExecutor;

/**
 * Velocity implementation of PlatformCommandExecutor using Velocity API.
 */
public class VelocityCommandExecutor implements PlatformCommandExecutor {
    
    private final ProxyServer proxyServer;
    
    public VelocityCommandExecutor(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }
    
    @Override
    public boolean executeAsConsole(String command) {
        try {
            proxyServer.getCommandManager().executeAsync(proxyServer.getConsoleCommandSource(), command);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean executeAsPlayer(String playerName, String command) {
        try {
            Player player = proxyServer.getPlayer(playerName).orElse(null);
            if (player != null && player.isActive()) {
                proxyServer.getCommandManager().executeAsync(player, command);
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
            return proxyServer.getCommandManager().hasCommand(baseCommand);
        } catch (Exception e) {
            return false;
        }
    }
}