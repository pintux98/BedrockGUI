package it.pintux.life.duelsaddon.api;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface BedrockPlayerDetector {
    boolean isBedrockPlayer(Player player);
}
