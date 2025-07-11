package org.pintux.life.velocity.platform;

import it.pintux.life.common.platform.PlatformSoundManager;
import it.pintux.life.common.utils.FormPlayer;

/**
 * Velocity implementation of PlatformSoundManager.
 * Note: Velocity doesn't have native sound support, sounds are handled by backend servers.
 */
public class VelocitySoundManager implements PlatformSoundManager {
    
    @Override
    public boolean playSound(FormPlayer player, String soundName, float volume, float pitch) {
        // Velocity doesn't have direct sound playing capabilities
        // Sounds would need to be handled by the backend servers
        // This could be implemented through plugin messaging
        return false;
    }
    
    @Override
    public boolean stopAllSounds(FormPlayer player) {
        // Velocity doesn't have direct sound control capabilities
        return false;
    }
    
    @Override
    public boolean soundExists(String soundName) {
        // Since we can't play sounds directly, we'll do basic validation
        return soundName != null && !soundName.trim().isEmpty();
    }
}