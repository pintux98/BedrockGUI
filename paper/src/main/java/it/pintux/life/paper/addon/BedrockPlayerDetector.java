package it.pintux.life.paper.addon;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface BedrockPlayerDetector {
    boolean isBedrockPlayer(Player player);
}
