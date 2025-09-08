package it.pintux.life.common.platform;

import it.pintux.life.common.utils.FormPlayer;

/**
 * Platform abstraction for player management operations.
 * This interface allows the common module to interact with players
 * without depending on platform-specific APIs.
 */
public interface PlatformPlayerManager {
    
    /**
     * Get a player by name.
     * 
     * @param playerName The name of the player
     * @return The platform-specific player object, or null if not found
     */
    Object getPlayer(String playerName);
    
    /**
     * Get an offline player by name.
     * 
     * @param playerName The name of the player
     * @return The platform-specific offline player object, or null if not found
     */
    Object getOfflinePlayer(String playerName);
    
    /**
     * Check if a player is online.
     * 
     * @param playerName The name of the player
     * @return true if the player is online, false otherwise
     */
    boolean isPlayerOnline(String playerName);
    
    /**
     * Send a message to a player.
     * 
     * @param playerName The name of the player
     * @param message The message to send
     */
    void sendMessage(String playerName, String message);
    
    /**
     * Send a message to a player using platform-specific player object.
     * 
     * @param player The platform-specific player object
     * @param message The message to send
     */
    void sendMessage(Object player, String message);
    
    /**
     * Get the player's name from a platform-specific player object.
     * 
     * @param player The platform-specific player object
     * @return The player's name
     */
    String getPlayerName(Object player);
    
    /**
     * Get the player's world from a platform-specific player object.
     * 
     * @param player The platform-specific player object
     * @return The platform-specific world object
     */
    Object getPlayerWorld(Object player);
    
    /**
     * Get the player's location from a platform-specific player object.
     * 
     * @param player The platform-specific player object
     * @return The platform-specific location object
     */
    Object getPlayerLocation(Object player);
    
    /**
     * Convert a platform-specific player to FormPlayer.
     * 
     * @param player The platform-specific player object
     * @return The FormPlayer instance
     */
    FormPlayer toFormPlayer(Object player);
    
    /**
     * Get the platform-specific player object from FormPlayer.
     * 
     * @param formPlayer The FormPlayer instance
     * @return The platform-specific player object
     */
    Object fromFormPlayer(FormPlayer formPlayer);
    
    /**
     * Get the world name from a platform-specific world object.
     * 
     * @param world The platform-specific world object
     * @return The world name
     */
    String getWorldName(Object world);
}