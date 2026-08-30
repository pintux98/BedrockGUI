package it.pintux.life.bungee.placeholders;

import it.pintux.life.common.utils.PlaceholderRegistry;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.stream.Collectors;

public final class CorePlaceholders {

    public static final String IDENTIFIER = "bgui";

    private CorePlaceholders() {
    }

    public static void register(PlaceholderRegistry registry, ProxyServer proxy) {
        registry.replace(IDENTIFIER, (player, params) -> switch (params.toLowerCase()) {
            case "online_players_list" -> onlinePlayersList(proxy);
            case "online_players_size" -> String.valueOf(proxy.getPlayers().size());
            case "player" -> player == null ? null : player.getName();
            case "uuid" -> player == null ? null : player.getUniqueId().toString();
            case "server" -> serverOf(proxy, player == null ? null : player.getName());
            default -> null;
        });
    }

    private static String onlinePlayersList(ProxyServer proxy) {
        return proxy.getPlayers().stream()
                .map(p -> p.getName() + ":" + p.getUniqueId().toString())
                .collect(Collectors.joining(","));
    }

    private static String serverOf(ProxyServer proxy, String name) {
        if (name == null) {
            return null;
        }
        ProxiedPlayer proxied = proxy.getPlayer(name);
        return proxied == null || proxied.getServer() == null
                ? null
                : proxied.getServer().getInfo().getName();
    }
}
