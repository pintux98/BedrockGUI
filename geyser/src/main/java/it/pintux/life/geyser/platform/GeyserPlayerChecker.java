package it.pintux.life.geyser.platform;

import it.pintux.life.common.platform.PlatformPlayerChecker;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;

import java.util.UUID;

/**
 * Geyser implementation of PlatformPlayerChecker using Geyser API.
 */
public class GeyserPlayerChecker implements PlatformPlayerChecker {
    
    @Override
    public boolean isBedrockPlayer(UUID playerUuid) {
        try {
            GeyserConnection connection = GeyserApi.api().connectionByUuid(playerUuid);
            return connection != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean isJavaPlayer(UUID playerUuid) {
        // In Geyser environment, all players are Bedrock players
        return false;
    }
    
    @Override
    public boolean isFloodgateAvailable() {
        // Geyser doesn't use Floodgate directly
        return false;
    }
    
    @Override
    public String getPlayerPlatform(UUID playerUuid) {
        if (isBedrockPlayer(playerUuid)) {
            return "BEDROCK";
        }
        return "UNKNOWN";
    }
    
    /**
     * Gets the Bedrock username for a player
     * @param playerUuid the player's UUID
     * @return the Bedrock username or null if not found
     */
    public String getBedrockUsername(UUID playerUuid) {
        try {
            GeyserConnection connection = GeyserApi.api().connectionByUuid(playerUuid);
            return connection != null ? connection.bedrockUsername() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Gets the Java username for a player
     * @param playerUuid the player's UUID
     * @return the Java username or null if not found
     */
    public String getJavaUsername(UUID playerUuid) {
        try {
            GeyserConnection connection = GeyserApi.api().connectionByUuid(playerUuid);
            return connection != null ? connection.javaUsername() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Gets the device OS for a Bedrock player
     * @param playerUuid the player's UUID
     * @return the device OS or null if not found
     */
    public String getDeviceOS(UUID playerUuid) {
        try {
            GeyserConnection connection = GeyserApi.api().connectionByUuid(playerUuid);
            if (connection != null) {
                return connection.platform().toString();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Gets the Geyser connection for a player
     * @param playerUuid the player's UUID
     * @return the GeyserConnection or null if not found
     */
    public GeyserConnection getConnection(UUID playerUuid) {
        try {
            return GeyserApi.api().connectionByUuid(playerUuid);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Checks if a player is connected through Geyser
     * @param playerUuid the player's UUID
     * @return true if the player is connected through Geyser
     */
    public boolean isGeyserPlayer(UUID playerUuid) {
        return isBedrockPlayer(playerUuid);
    }
    
    /**
     * Gets the XUID for a Bedrock player
     * @param playerUuid the player's UUID
     * @return the XUID or null if not found
     */
    public String getXuid(UUID playerUuid) {
        try {
            GeyserConnection connection = GeyserApi.api().connectionByUuid(playerUuid);
            return connection != null ? connection.xuid() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Checks if the player's connection is still active
     * @param playerUuid the player's UUID
     * @return true if the connection is active
     */
    public boolean isConnectionActive(UUID playerUuid) {
        try {
            GeyserConnection connection = GeyserApi.api().connectionByUuid(playerUuid);
            return connection != null;
        } catch (Exception e) {
            return false;
        }
    }
}