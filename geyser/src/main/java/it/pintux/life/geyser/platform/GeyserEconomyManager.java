package it.pintux.life.geyser.platform;

import it.pintux.life.common.platform.PlatformEconomyManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.geyser.utils.GeyserPlayer;
import org.geysermc.geyser.api.connection.GeyserConnection;

import java.util.logging.Logger;

/**
 * Geyser implementation of PlatformEconomyManager.
 * Note: Geyser is a proxy that translates between Java and Bedrock protocols,
 * so economy operations are not directly supported and would need to be
 * handled by the backend server.
 */
public class GeyserEconomyManager implements PlatformEconomyManager {
    
    private final Logger logger;
    
    public GeyserEconomyManager(Logger logger) {
        this.logger = logger;
    }
    
    @Override
    public boolean isEconomyAvailable() {
        // Geyser doesn't have direct economy support
        // Economy would be handled by the backend server
        return false;
    }
    
    @Override
    public double getBalance(FormPlayer player) {
        if (!(player instanceof GeyserPlayer)) {
            return 0.0;
        }
        
        try {
            GeyserPlayer geyserPlayer = (GeyserPlayer) player;
            GeyserConnection connection = geyserPlayer.getConnection();
            
            if (connection == null) {
                return 0.0;
            }
            
            // Geyser doesn't have direct economy access
            // Balance would need to be retrieved from the backend server
            logger.info("Economy balance retrieval not directly supported in Geyser");
            return 0.0;
            
        } catch (Exception e) {
            logger.warning("Failed to get player balance: " + e.getMessage());
            return 0.0;
        }
    }
    
    @Override
    public boolean hasBalance(FormPlayer player, double amount) {
        // Since we can't get the actual balance, we return false
        return false;
    }
    
    @Override
    public boolean withdrawBalance(FormPlayer player, double amount) {
        if (!(player instanceof GeyserPlayer)) {
            return false;
        }
        
        try {
            GeyserPlayer geyserPlayer = (GeyserPlayer) player;
            GeyserConnection connection = geyserPlayer.getConnection();
            
            if (connection == null) {
                return false;
            }
            
            // Geyser doesn't have direct economy access
            // Withdrawal would need to be handled by the backend server
            logger.info("Economy withdrawal not directly supported in Geyser");
            return false;
            
        } catch (Exception e) {
            logger.warning("Failed to withdraw from player balance: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean depositBalance(FormPlayer player, double amount) {
        if (!(player instanceof GeyserPlayer)) {
            return false;
        }
        
        try {
            GeyserPlayer geyserPlayer = (GeyserPlayer) player;
            GeyserConnection connection = geyserPlayer.getConnection();
            
            if (connection == null) {
                return false;
            }
            
            // Geyser doesn't have direct economy access
            // Deposit would need to be handled by the backend server
            logger.info("Economy deposit not directly supported in Geyser");
            return false;
            
        } catch (Exception e) {
            logger.warning("Failed to deposit to player balance: " + e.getMessage());
            return false;
        }
    }
}