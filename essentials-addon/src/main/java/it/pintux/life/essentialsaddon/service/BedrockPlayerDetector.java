package it.pintux.life.essentialsaddon.service;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface BedrockPlayerDetector {
    boolean isBedrockPlayer(Player player);
}
