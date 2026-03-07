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