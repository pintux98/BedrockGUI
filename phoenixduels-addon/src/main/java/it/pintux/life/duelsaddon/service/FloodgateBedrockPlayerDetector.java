package it.pintux.life.duelsaddon.service;

import it.pintux.life.duelsaddon.api.BedrockPlayerDetector;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

public final class FloodgateBedrockPlayerDetector implements BedrockPlayerDetector {
    @Override
    public boolean isBedrockPlayer(Player player) {
        try {
            return player != null && FloodgateApi.getInstance() != null
                    && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        } catch (Exception ignored) {
            return false;
        }
    }
}
