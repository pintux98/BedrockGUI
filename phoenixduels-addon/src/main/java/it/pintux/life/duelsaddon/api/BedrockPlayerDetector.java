package it.pintux.life.duelsaddon.api;

import org.bukkit.entity.Player;

/**
 * Decides whether a player is on Bedrock and should therefore get forms instead of chest menus.
 *
 * <p>An interface rather than a direct Floodgate call so the services can be reasoned about
 * without a Floodgate runtime.</p>
 */
@FunctionalInterface
public interface BedrockPlayerDetector {
    boolean isBedrockPlayer(Player player);
}
