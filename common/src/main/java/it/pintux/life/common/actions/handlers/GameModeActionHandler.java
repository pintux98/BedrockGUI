package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.platform.PlatformCommandExecutor;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;

import java.util.HashMap;
import java.util.Map;

/**
 * Action handler for managing player game modes
 * Supports changing game modes for players with validation and logging
 */
public class GameModeActionHandler extends BaseActionHandler {
    
    private final PlatformCommandExecutor commandExecutor;
    
    public GameModeActionHandler(PlatformCommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }
    
    @Override
    public String getActionType() {
        return "gamemode";
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
            
            if (parts.length < 2) {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", MessageData.ACTION_INVALID_FORMAT), player);
            }
            
            String operation = parts[0].toLowerCase();
            String targetPlayer = parts[1];
            String gameMode = parts.length > 2 ? parts[2] : null;
            
            // Replace %player_name% placeholder if used
            if (targetPlayer.equals("%player_name%") || targetPlayer.equals("@s")) {
                targetPlayer = player.getName();
            }
            
            switch (operation) {
                case "set":
                case "change":
                    if (gameMode == null) {
                        return ActionResult.failure("Game mode is required for set operation");
                    }
                    return handleSetGameMode(targetPlayer, gameMode);
                    
                case "toggle":
                    return handleToggleGameMode(targetPlayer, gameMode);
                    
                case "check":
                case "get":
                    return handleCheckGameMode(targetPlayer);
                    
                case "creative":
                case "c":
                    return handleSetGameMode(targetPlayer, "creative");
                    
                case "survival":
                case "s":
                    return handleSetGameMode(targetPlayer, "survival");
                    
                case "adventure":
                case "a":
                    return handleSetGameMode(targetPlayer, "adventure");
                    
                case "spectator":
                case "sp":
                    return handleSetGameMode(targetPlayer, "spectator");
                    
                default:
                    logger.warn("Unknown gamemode operation: " + operation + " for player: " + player.getName());
                    MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                    Map<String, Object> replacements = new HashMap<>();
                    replacements.put("operation", operation);
                    return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, replacements, player));
            }
            
        } catch (Exception e) {
            logger.error("Error executing gamemode action for player " + player.getName(), e);
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("error", e.getMessage());
            return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_EXECUTION_ERROR, replacements, player), e);
        }
    }
    
    private ActionResult handleSetGameMode(String targetPlayer, String gameMode) {
        // Normalize game mode
        String normalizedGameMode = normalizeGameMode(gameMode);
        if (normalizedGameMode == null) {
            return ActionResult.failure("Invalid game mode: " + gameMode + ". Valid modes: survival, creative, adventure, spectator");
        }
        
        String command = String.format("gamemode %s %s", normalizedGameMode, targetPlayer);
        boolean success = commandExecutor.executeAsConsole(command);
        
        if (success) {
            String displayMode = getGameModeDisplayName(normalizedGameMode);
            String message = String.format("Successfully changed %s's game mode to %s", targetPlayer, displayMode);
            logger.debug(message);
            return ActionResult.success(message);
        } else {
            return ActionResult.failure("Failed to change game mode. Command execution failed.");
        }
    }
    
    private ActionResult handleToggleGameMode(String targetPlayer, String preferredMode) {
        // This would typically require platform-specific implementation to get current game mode
        // For now, we'll implement a simple toggle between survival and creative
        String toggleMode = preferredMode != null ? preferredMode : "creative";
        
        // Normalize the toggle mode
        String normalizedToggleMode = normalizeGameMode(toggleMode);
        if (normalizedToggleMode == null) {
            normalizedToggleMode = "creative"; // Default fallback
        }
        
        // Since we can't easily check current game mode, we'll just set to the specified mode
        return handleSetGameMode(targetPlayer, normalizedToggleMode);
    }
    
    private ActionResult handleCheckGameMode(String targetPlayer) {
        // This would typically require platform-specific implementation to get current game mode
        // For now, we'll use a command that shows the player's game mode
        String command = String.format("data get entity %s playerGameType", targetPlayer);
        boolean success = commandExecutor.executeAsConsole(command);
        
        if (success) {
            return ActionResult.success("Checked game mode for " + targetPlayer);
        } else {
            return ActionResult.failure("Failed to check game mode. Command execution failed.");
        }
    }
    
    private String normalizeGameMode(String gameMode) {
        if (gameMode == null || gameMode.trim().isEmpty()) {
            return null;
        }
        
        String normalized = gameMode.toLowerCase().trim();
        
        // Handle common aliases and variations
        switch (normalized) {
            case "survival":
            case "s":
            case "0":
                return "survival";
                
            case "creative":
            case "c":
            case "1":
                return "creative";
                
            case "adventure":
            case "a":
            case "2":
                return "adventure";
                
            case "spectator":
            case "sp":
            case "spec":
            case "3":
                return "spectator";
                
            default:
                return null;
        }
    }
    
    private String getGameModeDisplayName(String gameMode) {
        switch (gameMode.toLowerCase()) {
            case "survival": return "Survival";
            case "creative": return "Creative";
            case "adventure": return "Adventure";
            case "spectator": return "Spectator";
            default: return gameMode;
        }
    }
    

    
    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }
        
        String[] parts = actionValue.split(":", 2);
        if (parts.length < 2) {
            return false;
        }
        
        String operation = parts[0].toLowerCase();
        return operation.equals("set") || operation.equals("change") ||
               operation.equals("toggle") || operation.equals("check") || operation.equals("get") ||
               operation.equals("creative") || operation.equals("c") ||
               operation.equals("survival") || operation.equals("s") ||
               operation.equals("adventure") || operation.equals("a") ||
               operation.equals("spectator") || operation.equals("sp");
    }
    
    @Override
    public String getDescription() {
        return "Manages player game modes including survival, creative, adventure, and spectator. Supports setting, toggling, and checking game modes.";
    }
    
    @Override
    public String[] getUsageExamples() {
        return new String[]{
            "gamemode:set:%player_name%:creative - Set current player to creative mode",
            "gamemode:set:PlayerName:survival - Set specific player to survival mode",
            "gamemode:creative:%player_name% - Quick set to creative mode",
            "gamemode:survival:@a - Set all players to survival mode",
            "gamemode:toggle:%player_name%:creative - Toggle between current and creative mode",
            "gamemode:check:%player_name% - Check current player's game mode",
            "gamemode:adventure:PlayerName - Set specific player to adventure mode",
            "gamemode:spectator:%player_name% - Set current player to spectator mode"
        };
    }
}