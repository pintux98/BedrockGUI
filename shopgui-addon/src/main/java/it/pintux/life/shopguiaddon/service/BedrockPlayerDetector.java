package it.pintux.life.shopguiaddon.service;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface BedrockPlayerDetector {
    boolean isBedrockPlayer(Player player);
}
