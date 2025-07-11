package it.pintux.life.geyser.platform;

import it.pintux.life.common.platform.PlatformSoundManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.geyser.utils.GeyserPlayer;
import org.geysermc.geyser.api.connection.GeyserConnection;

import java.util.logging.Logger;

/**
 * Geyser implementation of PlatformSoundManager.
 * Note: Geyser is a proxy that translates between Java and Bedrock protocols,
 * so sound playing capabilities are limited and would typically be handled
 * by the backend server.
 */
public class GeyserSoundManager implements PlatformSoundManager {
    
    private final Logger logger;
    
    public GeyserSoundManager(Logger logger) {
        this.logger = logger;
    }
    
    @Override
    public boolean playSound(FormPlayer player, String soundName, float volume, float pitch) {
        if (!(player instanceof GeyserPlayer)) {
            return false;
        }
        
        try {
            GeyserPlayer geyserPlayer = (GeyserPlayer) player;
            GeyserConnection connection = geyserPlayer.getConnection();
            
            if (connection == null) {
                return false;
            }
            
            // Geyser doesn't have direct sound playing capabilities
            // Sounds would need to be played through the backend server
            // or through Bedrock-specific sound packets if supported
            logger.info("Sound playing not directly supported in Geyser: " + soundName);
            return false;
            
        } catch (Exception e) {
            logger.warning("Failed to play sound: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean stopSound(FormPlayer player, String soundName) {
        if (!(player instanceof GeyserPlayer)) {
            return false;
        }
        
        try {
            GeyserPlayer geyserPlayer = (GeyserPlayer) player;
            GeyserConnection connection = geyserPlayer.getConnection();
            
            if (connection == null) {
                return false;
            }
            
            // Geyser doesn't have direct sound stopping capabilities
            logger.info("Sound stopping not directly supported in Geyser: " + soundName);
            return false;
            
        } catch (Exception e) {
            logger.warning("Failed to stop sound: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean soundExists(String soundName) {
        // Geyser doesn't maintain a sound registry
        // Sound existence would need to be checked on the backend server
        // or against Bedrock sound definitions
        return false;
    }
}