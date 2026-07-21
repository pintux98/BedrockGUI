package it.pintux.life.homesteadaddon.api;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface BedrockPlayerDetector {
    boolean isBedrockPlayer(Player player);
}
