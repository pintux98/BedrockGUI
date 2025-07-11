package it.pintux.life.common.platform;

import java.util.UUID;

/**
 * Platform abstraction for checking player types and capabilities.
 * This interface allows the common module to check player properties
 * without depending on platform-specific APIs like Floodgate.
 */
public interface PlatformPlayerChecker {
    
    /**
     * Check if a player is a Bedrock player.
     * 
     * @param playerUuid The UUID of the player to check
     * @return true if the player is a Bedrock player, false otherwise
     */
    boolean isBedrockPlayer(UUID playerUuid);
    
    /**
     * Check if a player is a Java player.
     * 
     * @param playerUuid The UUID of the player to check
     * @return true if the player is a Java player, false otherwise
     */
    boolean isJavaPlayer(UUID playerUuid);
    
    /**
     * Check if Floodgate is available on this platform.
     * 
     * @return true if Floodgate is available, false otherwise
     */
    boolean isFloodgateAvailable();
    
    /**
     * Get the platform type of a player (BEDROCK, JAVA, UNKNOWN).
     * 
     * @param playerUuid The UUID of the player
     * @return The platform type as a string
     */
    String getPlayerPlatform(UUID playerUuid);
}