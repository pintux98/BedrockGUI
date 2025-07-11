package it.pintux.life.bungee.platform;

import it.pintux.life.common.platform.PlatformSoundManager;
import it.pintux.life.common.utils.FormPlayer;

/**
 * Bungeecord implementation of PlatformSoundManager.
 * Note: BungeeCord doesn't have native sound support, sounds are handled by backend servers.
 */
public class BungeeSoundManager implements PlatformSoundManager {
    
    @Override
    public boolean playSound(FormPlayer player, String soundName, float volume, float pitch) {
        // BungeeCord doesn't have direct sound playing capabilities
        // Sounds would need to be handled by the backend servers
        // This could be implemented through plugin messaging
        return false;
    }
    
    @Override
    public boolean stopAllSounds(FormPlayer player) {
        // BungeeCord doesn't have direct sound control capabilities
        return false;
    }
    
    @Override
    public boolean soundExists(String soundName) {
        // Since we can't play sounds directly, we'll do basic validation
        return soundName != null && !soundName.trim().isEmpty();
    }
}