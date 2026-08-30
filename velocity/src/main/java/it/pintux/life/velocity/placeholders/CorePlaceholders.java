package it.pintux.life.velocity.placeholders;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import it.pintux.life.common.utils.PlaceholderRegistry;

import java.util.Optional;
import java.util.stream.Collectors;

public final class CorePlaceholders {

    public static final String IDENTIFIER = "bgui";

    private CorePlaceholders() {
    }

    public static void register(PlaceholderRegistry registry, ProxyServer proxy) {
        registry.replace(IDENTIFIER, (player, params) -> switch (params.toLowerCase()) {
            case "online_players_list" -> onlinePlayersList(proxy);
            case "online_players_size" -> String.valueOf(proxy.getPlayerCount());
            case "player" -> player == null ? null : player.getName();
            case "uuid" -> player == null ? null : player.getUniqueId().toString();
            case "server" -> serverOf(proxy, player == null ? null : player.getName());
            default -> null;
        });
    }

    private static String onlinePlayersList(ProxyServer proxy) {
        return proxy.getAllPlayers().stream()
                .map(p -> p.getUsername() + ":" + p.getUniqueId().toString())
                .collect(Collectors.joining(","));
    }

    private static String serverOf(ProxyServer proxy, String name) {
        if (name == null) {
            return null;
        }
        Optional<Player> connected = proxy.getPlayer(name);
        return connected
                .flatMap(p -> p.getCurrentServer().map(server -> server.getServerInfo().getName()))
                .orElse(null);
    }
}
