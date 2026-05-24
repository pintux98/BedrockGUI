package it.pintux.life.essentialsaddon.api;

import org.bukkit.entity.Player;

/**
 * Detects whether a player is on Bedrock Edition.
 */
@FunctionalInterface
public interface BedrockPlayerDetector {
    boolean isBedrockPlayer(Player player);
}
