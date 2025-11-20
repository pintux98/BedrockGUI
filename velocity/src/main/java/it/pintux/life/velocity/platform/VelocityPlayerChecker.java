package it.pintux.life.velocity.platform;

import com.velocitypowered.api.proxy.Player;
import it.pintux.life.common.platform.PlatformPlayerChecker;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

public class VelocityPlayerChecker implements PlatformPlayerChecker {

    @Override
    public boolean isBedrockPlayer(UUID playerUuid) {
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(playerUuid);
        } catch (Exception e) {
            // Floodgate not available or player not found
            return false;
        }
    }

    @Override
    public boolean isJavaPlayer(UUID playerUuid) {
        try {
            return !FloodgateApi.getInstance().isFloodgatePlayer(playerUuid);
        } catch (Exception e) {
            // If Floodgate is not available, assume Java player
            return true;
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

    @Override
    public String getPlayerPlatform(UUID playerUuid) {
        if (isBedrockPlayer(playerUuid)) {
            return "Bedrock";
        } else if (isJavaPlayer(playerUuid)) {
            return "Java";
        }
        return "Unknown";
    }
}