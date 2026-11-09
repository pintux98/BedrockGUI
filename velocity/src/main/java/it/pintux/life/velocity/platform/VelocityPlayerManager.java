package it.pintux.life.velocity.platform;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import it.pintux.life.common.platform.PlatformPlayerManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.velocity.utils.VelocityPlayer;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.Component;

import java.util.Optional;

public class VelocityPlayerManager implements PlatformPlayerManager {

    private final ProxyServer server;

    public VelocityPlayerManager(ProxyServer server) {
        this.server = server;
    }

    @Override
    public boolean broadcast(FormPlayer source, String audience, String target, String message) {
        java.util.function.Predicate<Player> filter;
        switch (audience) {
            case "permission" -> filter = p -> p.hasPermission(target);
            case "world" -> filter = p -> p.getCurrentServer().map(s -> s.getServerInfo().getName().equalsIgnoreCase(target)).orElse(false);
            case "radius" -> {
                return false;
            }
            default -> filter = p -> true;
        }
        server.getAllPlayers().stream().filter(filter).forEach(p -> p.sendMessage(VelocityPlayer.text(message)));
        return true;
    }

    @Override
    public java.util.List<String> getOnlinePlayerNames() {
        return server.getAllPlayers().stream().map(Player::getUsername).toList();
    }

    @Override
    public Object getPlayer(String playerName) {
        Optional<Player> player = server.getPlayer(playerName);
        return player.orElse(null);
    }

    @Override
    public void sendMessage(String playerName, String message) {
        server.getPlayer(playerName).ifPresent(player ->
                player.sendMessage(VelocityPlayer.text(message))
        );
    }

    @Override
    public void sendMessage(Object player, String message) {
        if (player instanceof FormPlayer formPlayer) {
            formPlayer.sendMessage(message);
        } else if (player instanceof Player velocityPlayer) {
            velocityPlayer.sendMessage(VelocityPlayer.text(message));
        }
    }

    @Override
    public boolean sendByteArray(FormPlayer player, String channel, byte[] data) {
        if (!(player instanceof VelocityPlayer velocityPlayer)) {
            return false;
        }
        Player p = velocityPlayer.getPlayer();
        if ("BungeeCord".equals(channel)) {
            return handleBungeeMessage(p, ByteStreams.newDataInput(data));
        }
        return p.getCurrentServer()
                .map(connection -> connection.sendPluginMessage(MinecraftChannelIdentifier.from(channel), data))
                .orElse(false);
    }

    private boolean handleBungeeMessage(Player player, ByteArrayDataInput in) {
        switch (in.readUTF()) {
            case "Connect" -> {
                return connect(player, in.readUTF());
            }
            case "ConnectOther" -> {
                Optional<Player> other = server.getPlayer(in.readUTF());
                return other.isPresent() && connect(other.get(), in.readUTF());
            }
            case "Message" -> {
                String target = in.readUTF();
                Component message = VelocityPlayer.text(in.readUTF());
                if (target.equals("ALL")) {
                    server.getAllPlayers().forEach(p -> p.sendMessage(message));
                    return true;
                }
                Optional<Player> recipient = server.getPlayer(target);
                recipient.ifPresent(p -> p.sendMessage(message));
                return recipient.isPresent();
            }
            case "KickPlayer" -> {
                Optional<Player> target = server.getPlayer(in.readUTF());
                Component reason = VelocityPlayer.text(in.readUTF());
                target.ifPresent(p -> p.disconnect(reason));
                return target.isPresent();
            }
            default -> {
                return false;
            }
        }
    }

    private boolean connect(Player player, String serverName) {
        Optional<RegisteredServer> target = server.getServer(serverName);
        target.ifPresent(registered -> player.createConnectionRequest(registered).fireAndForget());
        return target.isPresent();
    }
}