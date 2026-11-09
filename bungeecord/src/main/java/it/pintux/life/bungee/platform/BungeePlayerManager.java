package it.pintux.life.bungee.platform;

import it.pintux.life.common.platform.PlatformPlayerManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.bungee.utils.BungeePlayer;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;


public class BungeePlayerManager implements PlatformPlayerManager {
    private final ProxyServer proxy;

    public BungeePlayerManager(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public boolean broadcast(FormPlayer source, String audience, String target, String message) {
        java.util.function.Predicate<ProxiedPlayer> filter;
        switch (audience) {
            case "permission" -> filter = p -> p.hasPermission(target);
            case "world" -> filter = p -> p.getServer() != null && p.getServer().getInfo().getName().equalsIgnoreCase(target);
            case "radius" -> {
                return false;
            }
            default -> filter = p -> true;
        }
        proxy.getPlayers().stream().filter(filter).forEach(p -> p.sendMessage(BungeePlayer.text(message)));
        return true;
    }

    @Override
    public java.util.List<String> getOnlinePlayerNames() {
        return proxy.getPlayers().stream().map(ProxiedPlayer::getName).toList();
    }

    @Override
    public Object getPlayer(String playerName) {
        return proxy.getPlayer(playerName);
    }

    @Override
    public void sendMessage(String playerName, String message) {
        ProxiedPlayer p = proxy.getPlayer(playerName);
        if (p != null) p.sendMessage(BungeePlayer.text(message));
    }

    @Override
    public void sendMessage(Object player, String message) {
        if (player instanceof FormPlayer formPlayer) {
            formPlayer.sendMessage(message);
        } else if (player instanceof ProxiedPlayer proxiedPlayer) {
            proxiedPlayer.sendMessage(BungeePlayer.text(message));
        }
    }

    @Override
    public boolean sendByteArray(FormPlayer player, String channel, byte[] data) {
        if (!(player instanceof BungeePlayer bungeePlayer)) {
            return false;
        }
        ProxiedPlayer p = bungeePlayer.getPlayer();
        if ("BungeeCord".equals(channel)) {
            return handleBungeeMessage(p, com.google.common.io.ByteStreams.newDataInput(data));
        }
        if (p.getServer() == null) {
            return false;
        }
        p.getServer().sendData(channel, data);
        return true;
    }

    private boolean handleBungeeMessage(ProxiedPlayer player, com.google.common.io.ByteArrayDataInput in) {
        switch (in.readUTF()) {
            case "Connect" -> {
                return connect(player, in.readUTF());
            }
            case "ConnectOther" -> {
                ProxiedPlayer other = proxy.getPlayer(in.readUTF());
                return other != null && connect(other, in.readUTF());
            }
            case "Message" -> {
                String target = in.readUTF();
                String message = in.readUTF();
                if (target.equals("ALL")) {
                    proxy.getPlayers().forEach(p -> p.sendMessage(BungeePlayer.text(message)));
                    return true;
                }
                ProxiedPlayer recipient = proxy.getPlayer(target);
                if (recipient != null) {
                    recipient.sendMessage(BungeePlayer.text(message));
                }
                return recipient != null;
            }
            case "KickPlayer" -> {
                ProxiedPlayer target = proxy.getPlayer(in.readUTF());
                String reason = in.readUTF();
                if (target != null) {
                    target.disconnect(BungeePlayer.text(reason));
                }
                return target != null;
            }
            default -> {
                return false;
            }
        }
    }

    private boolean connect(ProxiedPlayer player, String serverName) {
        net.md_5.bungee.api.config.ServerInfo target = proxy.getServerInfo(serverName);
        if (target != null) {
            player.connect(target);
        }
        return target != null;
    }
}