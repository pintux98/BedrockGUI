package it.pintux.life.common.platform;

import it.pintux.life.common.utils.FormPlayer;
import org.geysermc.cumulus.form.Form;

import java.util.UUID;

/**
 * Platform abstraction for sending forms to players.
 * This interface allows the common module to send forms
 * without depending on platform-specific APIs.
 */
public interface PlatformFormSender {
    
    /**
     * Send a form to a specific player.
     * 
     * @param player The player to send the form to
     * @param form The form to send
     * @return true if the form was sent successfully, false otherwise
     */
    boolean sendForm(FormPlayer player, Form form);
    
    /**
     * Check if a player is a Bedrock player (can receive forms).
     * 
     * @param playerUuid The UUID of the player to check
     * @return true if the player is a Bedrock player, false otherwise
     */
    boolean isBedrockPlayer(UUID playerUuid);
    
    /**
     * Check if the form system is available on this platform.
     * 
     * @return true if forms are supported, false otherwise
     */
    boolean isFormSystemAvailable();
}