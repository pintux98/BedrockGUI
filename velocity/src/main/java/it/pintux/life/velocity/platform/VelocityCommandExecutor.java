package it.pintux.life.velocity.platform;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import it.pintux.life.common.platform.PlatformCommandExecutor;


public class VelocityCommandExecutor implements PlatformCommandExecutor {

    private final ProxyServer proxyServer;

    public VelocityCommandExecutor(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public boolean executeAsConsole(String command) {
        CommandSource console = proxyServer.getConsoleCommandSource();
        return proxyServer.getCommandManager().executeAsync(console, command).join();
    }

    @Override
    public boolean executeAsPlayer(String playerName, String command) {
        Player player = proxyServer.getPlayer(playerName).orElse(null);
        if (player == null) return false;
        String alias = command.trim().split("\\s+", 2)[0];
        if (proxyServer.getCommandManager().hasCommand(alias, player)) {
            return proxyServer.getCommandManager().executeAsync(player, command).join();
        }
        return new it.pintux.life.velocity.utils.VelocityPlayer(player).executeAction(command);
    }
}