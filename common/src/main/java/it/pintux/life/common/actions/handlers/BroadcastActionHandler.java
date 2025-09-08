package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;

import java.util.Map;
import java.util.HashMap;
import it.pintux.life.common.platform.PlatformCommandExecutor;

/**
 * Handles broadcasting messages to all players or specific groups.
 * Uses the platform command executor to send broadcast commands.
 * 
 * Usage: broadcast:Welcome to the server!
 * Usage: broadcast:permission:vip.access:VIP only message!
 * Usage: broadcast:world:world_nether:Nether announcement!
 */
public class BroadcastActionHandler extends BaseActionHandler {
    private final PlatformCommandExecutor commandExecutor;
    
    public BroadcastActionHandler(PlatformCommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }
    
    @Override
    public String getActionType() {
        return "broadcast";
    }
    
    @Override
    public ActionResult execute(FormPlayer player, String actionData, ActionContext context) {
        // Validate basic parameters using base class method
        ActionResult validationResult = validateBasicParameters(player, actionData);
        if (validationResult != null) {
            return validationResult;
        }
        
        try {
            // Process placeholders in the action data
            String processedData = processPlaceholders(actionData.trim(), context, player);
            String[] parts = processedData.split(":", 3);
            
            String message;
            String broadcastCommand;
            
            if (parts.length == 1) {
                // Simple broadcast: broadcast:message
                message = parts[0];
                broadcastCommand = "say " + message;
            } else if (parts.length == 3) {
                // Targeted broadcast: broadcast:type:target:message
                String type = parts[0];
                String target = parts[1];
                message = parts[2];
                
                switch (type.toLowerCase()) {
                    case "permission":
                        // Use a plugin command that supports permission-based broadcasting
                        // This is platform-specific, so we'll use a generic approach
                        broadcastCommand = "broadcast permission " + target + " " + message;
                        break;
                    case "world":
                        // Broadcast to specific world
                        broadcastCommand = "broadcast world " + target + " " + message;
                        break;
                    case "radius":
                        // Broadcast within radius of player
                        broadcastCommand = "broadcast radius " + target + " " + player.getName() + " " + message;
                        break;
                    default:
                        return createFailureResult(MessageData.ACTION_INVALID_PARAMETERS, createReplacements("type", type), player);
                }
            } else {
                // parts.length == 2, treat as simple message with colon
                message = parts[0] + ":" + parts[1];
                broadcastCommand = "say " + message;
            }
            
            logger.info("Broadcasting message from player " + player.getName() + ": " + message);
            
            // Execute broadcast command through platform abstraction
            boolean success = commandExecutor.executeAsConsole(broadcastCommand);
            
            if (success) {
                logger.info("Broadcast sent successfully: " + message);
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                Map<String, Object> replacements = new HashMap<>();
                replacements.put("message", message);
                return ActionResult.success(messageData.getValueNoPrefix(MessageData.ACTION_BROADCAST_SUCCESS, replacements, player));
            } else {
                // Fallback: try alternative broadcast commands
                String[] fallbackCommands = {
                    "bc " + message,
                    "broadcast " + message,
                    "announce " + message
                };
                
                for (String fallbackCommand : fallbackCommands) {
                    if (commandExecutor.executeAsConsole(fallbackCommand)) {
                        logger.info("Broadcast sent successfully with fallback command: " + message);
                        MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                        Map<String, Object> replacements = new HashMap<>();
                        replacements.put("message", message);
                        return ActionResult.success(messageData.getValueNoPrefix(MessageData.ACTION_BROADCAST_SUCCESS, replacements, player));
                    }
                }
                
                logger.warn("Failed to broadcast message: " + message);
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_BROADCAST_FAILED, null, player));
            }
            
        } catch (Exception e) {
            logger.error("Error executing broadcast action for player " + player.getName() + ": " + e.getMessage());
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("error", e.getMessage());
            return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_EXECUTION_ERROR, replacements, player));
        }
    }
    
    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }
        
        String[] parts = actionValue.trim().split(":", 3);
        
        if (parts.length == 1) {
            // Simple broadcast
            return !parts[0].isEmpty();
        } else if (parts.length == 3) {
            // Targeted broadcast
            String type = parts[0].toLowerCase();
            String target = parts[1];
            String message = parts[2];
            
            if (target.isEmpty() || message.isEmpty()) {
                return false;
            }
            
            switch (type) {
                case "permission":
                case "world":
                case "radius":
                    return true;
                default:
                    return false;
            }
        }
        
        return true; // parts.length == 2 is also valid
    }
    
    @Override
    public String getDescription() {
        return "Broadcasts messages to all players or specific groups based on permissions, worlds, or proximity";
    }
    
    @Override
    public String[] getUsageExamples() {
        return new String[]{
            "broadcast:Welcome to the server!",
            "broadcast:permission:vip.access:VIP only message!",
            "broadcast:world:world_nether:Nether announcement!",
            "broadcast:radius:50:Local announcement!"
        };
    }
    

}
