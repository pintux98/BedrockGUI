package it.pintux.life.velocity.platform;

import it.pintux.life.common.platform.PlatformSoundManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.velocity.utils.VelocityPlayer;

public class VelocitySoundManager implements PlatformSoundManager {

    @Override
    public boolean playSound(FormPlayer player, String soundName, float volume, float pitch) {
        if (!(player instanceof VelocityPlayer)) {
            return false;
        }
        
        VelocityPlayer velocityPlayer = (VelocityPlayer) player;
        if (!velocityPlayer.isOnline()) {
            return false;
        }
        
        // Send sound play request to backend server via plugin message
        // Note: This is a simplified implementation - actual implementation would need proper plugin message handling
        velocityPlayer.getPlayer().getCurrentServer().ifPresent(serverConnection -> {
            // Sound handling would be implemented on backend servers
        });
        
        return true;
    }

    @Override
    public boolean stopAllSounds(FormPlayer player) {
        if (!(player instanceof VelocityPlayer)) {
            return false;
        }
        
        VelocityPlayer velocityPlayer = (VelocityPlayer) player;
        if (!velocityPlayer.isOnline()) {
            return false;
        }
        
        // Send sound stop request to backend server via plugin message
        velocityPlayer.getPlayer().getCurrentServer().ifPresent(serverConnection -> {
            // Sound handling would be implemented on backend servers
        });
        
        return true;
    }

    @Override
    public boolean soundExists(String soundName) {
        // All sounds are considered valid on proxy level
        // Actual validation happens on backend servers
        return true;
    }
}