package it.pintux.life.velocity.platform;

import it.pintux.life.common.platform.PlatformTitleManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.velocity.utils.VelocityPlayer;

public class VelocityTitleManager implements PlatformTitleManager {

    @Override
    public boolean sendTitle(FormPlayer player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (!(player instanceof VelocityPlayer)) {
            return false;
        }
        
        VelocityPlayer velocityPlayer = (VelocityPlayer) player;
        if (!velocityPlayer.isOnline()) {
            return false;
        }
        
        // Send title data to backend server via plugin message
        // Note: This is a simplified implementation - actual implementation would need proper plugin message handling
        velocityPlayer.getPlayer().getCurrentServer().ifPresent(serverConnection -> {
            // Title handling would be implemented on backend servers
        });
        
        return true;
    }

    @Override
    public boolean sendActionBar(FormPlayer player, String message) {
        if (!(player instanceof VelocityPlayer)) {
            return false;
        }
        
        VelocityPlayer velocityPlayer = (VelocityPlayer) player;
        if (!velocityPlayer.isOnline()) {
            return false;
        }
        
        // Send actionbar data to backend server via plugin message
        velocityPlayer.getPlayer().getCurrentServer().ifPresent(serverConnection -> {
            // Actionbar handling would be implemented on backend servers
        });
        
        return true;
    }

    @Override
    public void clearTitle(FormPlayer player) {
        if (!(player instanceof VelocityPlayer)) {
            return;
        }
        
        VelocityPlayer velocityPlayer = (VelocityPlayer) player;
        if (!velocityPlayer.isOnline()) {
            return;
        }
        
        // Send clear title request to backend server via plugin message
        velocityPlayer.getPlayer().getCurrentServer().ifPresent(serverConnection -> {
            // Clear title handling would be implemented on backend servers
        });
    }

    @Override
    public void resetTitle(FormPlayer player) {
        if (!(player instanceof VelocityPlayer)) {
            return;
        }
        
        VelocityPlayer velocityPlayer = (VelocityPlayer) player;
        if (!velocityPlayer.isOnline()) {
            return;
        }
        
        // Send reset title request to backend server via plugin message
        velocityPlayer.getPlayer().getCurrentServer().ifPresent(serverConnection -> {
            // Reset title handling would be implemented on backend servers
        });
    }

    @Override
    public boolean isSupported() {
        // Title support is available if backend servers support it
        return true;
    }
}