package it.pintux.life.paper.placeholders;

import it.pintux.life.common.utils.PlaceholderRegistry;
import org.bukkit.Bukkit;

import java.util.stream.Collectors;

public final class CorePlaceholders {

    public static final String IDENTIFIER = "bgui";

    private CorePlaceholders() {
    }

    public static void register(PlaceholderRegistry registry) {
        registry.replace(IDENTIFIER, (player, params) -> switch (params.toLowerCase()) {
            case "online_players_list" -> onlinePlayersList();
            case "online_players_size" -> String.valueOf(Bukkit.getOnlinePlayers().size());
            case "player" -> player == null ? null : player.getName();
            case "uuid" -> player == null ? null : player.getUniqueId().toString();
            default -> null;
        });
    }

    private static String onlinePlayersList() {
        return Bukkit.getOnlinePlayers().stream()
                .map(p -> p.getName() + ":" + p.getUniqueId().toString())
                .collect(Collectors.joining(","));
    }
}
