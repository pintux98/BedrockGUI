package it.pintux.life.bungee.platform;

import it.pintux.life.common.platform.PlatformPlayerChecker;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

public class BungeePlayerChecker implements PlatformPlayerChecker {
    @Override
    public boolean isBedrockPlayer(UUID playerUuid) {
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(playerUuid);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isFloodgateAvailable() {
        try {
            return FloodgateApi.getInstance() != null;
        } catch (Exception e) {
            return false;
        }
    }
}