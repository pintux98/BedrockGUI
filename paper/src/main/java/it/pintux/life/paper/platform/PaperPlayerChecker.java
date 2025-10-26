package it.pintux.life.paper.platform;

import it.pintux.life.common.platform.PlatformPlayerChecker;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;


public class PaperPlayerChecker implements PlatformPlayerChecker {

    @Override
    public boolean isBedrockPlayer(UUID playerUuid) {
        try {
            return isFloodgateAvailable() && FloodgateApi.getInstance().isFloodgatePlayer(playerUuid);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isJavaPlayer(UUID playerUuid) {
        return !isBedrockPlayer(playerUuid);
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
            return "BEDROCK";
        } else if (isJavaPlayer(playerUuid)) {
            return "JAVA";
        }
        return "UNKNOWN";
    }
}
