package it.pintux.life.velocity.platform;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import it.pintux.life.common.platform.PlatformCommandExecutor;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.velocity.utils.VelocityPlayer;

import java.nio.charset.StandardCharsets;
import java.util.List;

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
        proxyServer.getCommandManager().executeAsync(player, command).join();
        return true;
    }
}