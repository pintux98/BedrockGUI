package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionHandler;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.PlaceholderUtil;
import it.pintux.life.common.utils.ValidationUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles spawning particle effects for players.
 * 
 * Usage examples:
 * - particle:heart (spawns heart particles at player location)
 * - particle:flame:10 (spawns 10 flame particles)
 * - particle:smoke:5:2.0:1.0:2.0 (spawns 5 smoke particles at offset x=2.0, y=1.0, z=2.0)
 * - particle:enchant:20:0:3:0:0.5 (spawns 20 enchant particles 3 blocks above player with 0.5 speed)
 */
public class ParticleActionHandler implements ActionHandler {
    
    private static final Logger logger = Logger.getLogger(ParticleActionHandler.class);
    
    // Common particle types that work across different Minecraft versions
    private static final String[] VALID_PARTICLES = {
        "heart", "flame", "smoke", "enchant", "portal", "note", "splash", 
        "bubble", "critical", "magic", "snowball", "slime", "villager_happy",
        "villager_angry", "redstone", "spell", "drip_water", "drip_lava",
        "cloud", "explosion", "firework", "end_rod", "dragon_breath"
    };
    
    @Override
    public String getActionType() {
        return "particle";
    }
    
    @Override
    public ActionResult execute(FormPlayer player, String actionValue, ActionContext context) {
        if (player == null) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, null, player));
        }
        
        if (ValidationUtils.isNullOrEmpty(actionValue)) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, null, player));
        }
        
        try {
            String processedValue = processPlaceholders(actionValue, context, player);
            
            // Parse particle parameters: particle[:count[:offsetX[:offsetY[:offsetZ[:speed]]]]]
            String[] parts = processedValue.split(":");
            
            if (parts.length == 0 || ValidationUtils.isNullOrEmpty(parts[0])) {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, null, player));
            }
            
            String particleType = parts[0].toLowerCase().trim();
            
            // Validate particle type
            if (!isValidParticle(particleType)) {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                Map<String, Object> replacements = new HashMap<>();
                replacements.put("particle", particleType);
                return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_INVALID_FORMAT, replacements, player));
            }
            
            // Parse optional parameters with defaults
            int count = 1;
            double offsetX = 0.0;
            double offsetY = 0.0;
            double offsetZ = 0.0;
            double speed = 0.1;
            
            try {
                if (parts.length > 1 && !ValidationUtils.isNullOrEmpty(parts[1])) {
                    count = Math.max(1, Math.min(100, Integer.parseInt(parts[1]))); // Limit between 1-100
                }
                if (parts.length > 2 && !ValidationUtils.isNullOrEmpty(parts[2])) {
                    offsetX = Double.parseDouble(parts[2]);
                }
                if (parts.length > 3 && !ValidationUtils.isNullOrEmpty(parts[3])) {
                    offsetY = Double.parseDouble(parts[3]);
                }
                if (parts.length > 4 && !ValidationUtils.isNullOrEmpty(parts[4])) {
                    offsetZ = Double.parseDouble(parts[4]);
                }
                if (parts.length > 5 && !ValidationUtils.isNullOrEmpty(parts[5])) {
                    speed = Math.max(0.0, Math.min(2.0, Double.parseDouble(parts[5]))); // Limit between 0-2
                }
            } catch (NumberFormatException e) {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_INVALID_FORMAT, null, player));
            }
            
            // Create particle command and execute it
            String particleCommand = String.format("/particle %s ~ ~ ~ %.2f %.2f %.2f %.2f %d", 
                particleType, offsetX, offsetY, offsetZ, speed, count);
            boolean success = player.executeAction(particleCommand);
            
            if (success) {
                logger.debug("Successfully spawned " + count + " " + particleType + " particles for player " + player.getName());
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                Map<String, Object> replacements = new HashMap<>();
                replacements.put("particle", particleType);
                replacements.put("count", String.valueOf(count));
                return ActionResult.success(messageData.getValueNoPrefix(MessageData.ACTION_PARTICLE_SUCCESS, replacements, player));
            } else {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_PARTICLE_FAILED, null, player));
            }
            
        } catch (Exception e) {
            logger.error("Error spawning particles for player " + player.getName(), e);
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("error", e.getMessage());
            return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_EXECUTION_ERROR, replacements, player), e);
        }
    }
    
    @Override
    public boolean isValidAction(String actionValue) {
        if (ValidationUtils.isNullOrEmpty(actionValue)) {
            return false;
        }
        
        String[] parts = actionValue.trim().split(":");
        
        // Must have at least particle type
        if (parts.length == 0 || ValidationUtils.isNullOrEmpty(parts[0])) {
            return false;
        }
        
        String particleType = parts[0].toLowerCase().trim();
        if (!isValidParticle(particleType)) {
            return false;
        }
        
        // Validate numeric parameters if provided
        try {
            if (parts.length > 1 && !ValidationUtils.isNullOrEmpty(parts[1])) {
                int count = Integer.parseInt(parts[1]);
                if (count < 1 || count > 100) return false;
            }
            if (parts.length > 2 && !ValidationUtils.isNullOrEmpty(parts[2])) {
                Double.parseDouble(parts[2]); // offsetX
            }
            if (parts.length > 3 && !ValidationUtils.isNullOrEmpty(parts[3])) {
                Double.parseDouble(parts[3]); // offsetY
            }
            if (parts.length > 4 && !ValidationUtils.isNullOrEmpty(parts[4])) {
                Double.parseDouble(parts[4]); // offsetZ
            }
            if (parts.length > 5 && !ValidationUtils.isNullOrEmpty(parts[5])) {
                double speed = Double.parseDouble(parts[5]);
                if (speed < 0.0 || speed > 2.0) return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        return "Spawns particle effects at the player's location. Supports various particle types with customizable count, offset, and speed parameters.";
    }
    
    @Override
    public String[] getUsageExamples() {
        return new String[]{
            "heart - Spawns heart particles at player location",
            "flame:10 - Spawns 10 flame particles",
            "smoke:5:2.0:1.0:2.0 - Spawns 5 smoke particles with offset",
            "enchant:20:0:3:0:0.5 - Spawns 20 enchant particles 3 blocks above with speed 0.5",
            "portal:15:1:1:1:1.0 - Spawns 15 portal particles in a small area"
        };
    }
    
    /**
     * Checks if the particle type is valid
     * @param particleType the particle type to check
     * @return true if valid, false otherwise
     */
    private boolean isValidParticle(String particleType) {
        for (String validParticle : VALID_PARTICLES) {
            if (validParticle.equals(particleType)) {
                return true;
            }
        }
        return false;
    }
    

    
    /**
     * Processes placeholders in the action value
     * @param actionValue the action value with placeholders
     * @param context the action context containing placeholder values
     * @param player the player for PlaceholderAPI processing
     * @return the processed action value
     */
    private String processPlaceholders(String actionValue, ActionContext context, FormPlayer player) {
        if (context == null) {
            return actionValue;
        }
        
        // Process dynamic placeholders first
        String result = PlaceholderUtil.processDynamicPlaceholders(actionValue, context.getPlaceholders());
        result = PlaceholderUtil.processFormResults(result, context.getFormResults());
        
        // Then process PlaceholderAPI placeholders if available
        if (context.getMetadata() != null && context.getMetadata().containsKey("messageData")) {
            Object messageDataObj = context.getMetadata().get("messageData");
            if (messageDataObj instanceof MessageData) {
                MessageData messageData = (MessageData) messageDataObj;
                result = PlaceholderUtil.processPlaceholders(result, null, player, messageData);
            }
        }
        
        return result;
    }
}