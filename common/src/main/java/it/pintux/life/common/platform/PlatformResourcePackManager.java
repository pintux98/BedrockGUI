package it.pintux.life.common.platform;

import java.util.Set;
import java.util.UUID;

/**
 * Platform abstraction for resource pack management.
 * This interface allows the common module to handle resource packs
 * without depending on platform-specific APIs.
 */
public interface PlatformResourcePackManager {
    
    /**
     * Check if resource pack support is available on this platform.
     * 
     * @return true if resource packs are supported, false otherwise
     */
    boolean isResourcePackSupported();
    
    /**
     * Send a resource pack to a specific player.
     * 
     * @param playerUuid The UUID of the player
     * @param packName The name of the resource pack
     * @return true if the pack was sent successfully, false otherwise
     */
    boolean sendResourcePack(UUID playerUuid, String packName);
    
    /**
     * Check if a player has a specific resource pack loaded.
     * 
     * @param playerUuid The UUID of the player
     * @param packName The name of the resource pack
     * @return true if the player has the pack, false otherwise
     */
    boolean hasResourcePack(UUID playerUuid, String packName);
    
    /**
     * Get all loaded resource packs.
     * 
     * @return Set of loaded pack names
     */
    Set<String> getLoadedPacks();
    
    /**
     * Get the resource pack associated with a specific menu.
     * 
     * @param menuName The name of the menu
     * @return The pack name, or null if no pack is associated
     */
    String getMenuResourcePack(String menuName);
    
    /**
     * Send a menu-specific resource pack to a player.
     * 
     * @param playerUuid The UUID of the player
     * @param menuName The name of the menu
     * @return true if the pack was sent successfully, false otherwise
     */
    boolean sendMenuResourcePack(UUID playerUuid, String menuName);
    
    /**
     * Check if resource pack management is enabled.
     * 
     * @return true if enabled, false otherwise
     */
    boolean isEnabled();
}