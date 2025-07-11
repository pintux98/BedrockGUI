package it.pintux.life.geyser.platform;

import it.pintux.life.common.platform.PlatformCommandExecutor;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.geyser.utils.GeyserPlayer;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Geyser implementation of PlatformCommandExecutor.
 * Note: Geyser is a proxy that translates between Java and Bedrock protocols,
 * so command execution capabilities are limited compared to server platforms.
 */
public class GeyserCommandExecutor implements PlatformCommandExecutor {
    
    private final Logger logger;
    
    public GeyserCommandExecutor(Logger logger) {
        this.logger = logger;
    }
    
    @Override
    public boolean executeAsConsole(String command) {
        // Geyser doesn't have direct console command execution capabilities
        // This would need to be handled by the backend server
        logger.warning("Console command execution not supported in Geyser: " + command);
        return false;
    }
    
    @Override
    public boolean executeAsPlayer(FormPlayer player, String command) {
        if (!(player instanceof GeyserPlayer)) {
            return false;
        }
        
        try {
            GeyserPlayer geyserPlayer = (GeyserPlayer) player;
            GeyserConnection connection = geyserPlayer.getConnection();
            
            if (connection == null) {
                return false;
            }
            
            // Geyser doesn't have direct player command execution
            // Commands would need to be sent to the backend server
            logger.info("Player command execution not directly supported in Geyser: " + command);
            return false;
            
        } catch (Exception e) {
            logger.warning("Failed to execute command as player: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean commandExists(String command) {
        // Geyser doesn't maintain a command registry
        // Command existence would need to be checked on the backend server
        return false;
    }
}