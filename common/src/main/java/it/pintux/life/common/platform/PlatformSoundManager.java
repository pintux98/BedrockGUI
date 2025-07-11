package it.pintux.life.common.platform;

import it.pintux.life.common.utils.FormPlayer;

/**
 * Platform abstraction for playing sounds to players.
 * This interface allows the common module to play sounds
 * without depending on platform-specific APIs.
 */
public interface PlatformSoundManager {
    
    /**
     * Play a sound to a specific player.
     * 
     * @param player The player to play the sound to
     * @param soundName The name of the sound to play
     * @param volume The volume of the sound (0.0 to 1.0)
     * @param pitch The pitch of the sound (0.5 to 2.0)
     * @return true if the sound was played successfully, false otherwise
     */
    boolean playSound(FormPlayer player, String soundName, float volume, float pitch);
    
    /**
     * Play a sound to a specific player with default volume and pitch.
     * 
     * @param player The player to play the sound to
     * @param soundName The name of the sound to play
     * @return true if the sound was played successfully, false otherwise
     */
    default boolean playSound(FormPlayer player, String soundName) {
        return playSound(player, soundName, 1.0f, 1.0f);
    }
    
    /**
     * Stop all sounds for a specific player.
     * 
     * @param player The player to stop sounds for
     * @return true if sounds were stopped successfully, false otherwise
     */
    boolean stopAllSounds(FormPlayer player);
    
    /**
     * Check if a sound exists on this platform.
     * 
     * @param soundName The name of the sound to check
     * @return true if the sound exists, false otherwise
     */
    boolean soundExists(String soundName);
}