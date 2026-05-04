package it.pintux.life.shopguiaddon.service;

import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

public final class FloodgateBedrockPlayerDetector implements BedrockPlayerDetector {
    @Override
    public boolean isBedrockPlayer(Player player) {
        try {
            return player != null && FloodgateApi.getInstance() != null && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        } catch (Exception ignored) {
            return false;
        }
    }
}
