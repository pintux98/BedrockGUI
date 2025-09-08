package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.platform.PlatformSoundManager;
import it.pintux.life.common.utils.FormPlayer;

/**
 * Handles playing sounds to players.
 * 
 * Usage: sound:ui.button.click
 * Usage: sound:entity.experience_orb.pickup:0.5:1.2 (sound:volume:pitch)
 * Usage: sound:block.note_block.harp:1.0:0.8
 */
public class SoundActionHandler extends BaseActionHandler {
    private final PlatformSoundManager soundManager;
    
    public SoundActionHandler(PlatformSoundManager soundManager) {
        this.soundManager = soundManager;
    }
    
    @Override
    public String getActionType() {
        return "sound";
    }
    
    private boolean validateParameters(FormPlayer player, String actionData) {
        return player != null && actionData != null && !actionData.trim().isEmpty();
    }

    @Override
    public ActionResult execute(FormPlayer player, String actionData, ActionContext context) {
        if (!validateParameters(player, actionData)) {
            logger.warn("Sound action called with invalid parameters for player: " + player.getName());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "No sound specified"), player);
        }
        
        try {
            // Process placeholders in the action data
            String processedData = processPlaceholders(actionData.trim(), context, player);
            
            // Parse sound data: sound[:volume[:pitch]]
            String[] parts = processedData.split(":");
            String soundName = parts[0];
            float volume = 1.0f;
            float pitch = 1.0f;
            
            if (parts.length > 1) {
                try {
                    volume = Float.parseFloat(parts[1]);
                    volume = Math.max(0.0f, Math.min(1.0f, volume)); // Clamp between 0.0 and 1.0
                } catch (NumberFormatException e) {
                    logger.warn("Invalid volume value in sound action: " + parts[1]);
                }
            }
            
            if (parts.length > 2) {
                try {
                    pitch = Float.parseFloat(parts[2]);
                    pitch = Math.max(0.5f, Math.min(2.0f, pitch)); // Clamp between 0.5 and 2.0
                } catch (NumberFormatException e) {
                    logger.warn("Invalid pitch value in sound action: " + parts[2]);
                }
            }
            
            logger.info("Playing sound for player " + player.getName() + ": " + soundName + " (volume: " + volume + ", pitch: " + pitch + ")");
            
            boolean success = soundManager.playSound(player, soundName, volume, pitch);
            
            if (success) {
                return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Sound played: " + soundName), player);
            } else {
                logger.warn("Failed to play sound: " + soundName);
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to play sound: " + soundName), player);
            }
            
        } catch (Exception e) {
            logger.error("Error playing sound for player " + player.getName() + ": " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Error playing sound: " + e.getMessage()), player);
        }
    }
    
    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }
        
        String[] parts = actionValue.trim().split(":");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return false;
        }
        
        // Validate volume if provided
        if (parts.length > 1) {
            try {
                float volume = Float.parseFloat(parts[1]);
                if (volume < 0.0f || volume > 1.0f) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        // Validate pitch if provided
        if (parts.length > 2) {
            try {
                float pitch = Float.parseFloat(parts[2]);
                if (pitch < 0.5f || pitch > 2.0f) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        return "Plays sounds to players with customizable volume and pitch";
    }
    
    @Override
    public String[] getUsageExamples() {
        return new String[]{
            "sound:ui.button.click",
            "sound:entity.experience_orb.pickup:1.0:1.2",
            "sound:block.note_block.harp:0.5",
            "sound:entity.player.levelup:1.0:0.8"
        };
    }
    

}
