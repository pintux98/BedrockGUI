package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionHandler;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;
import it.pintux.life.common.utils.PlaceholderUtil;
import it.pintux.life.common.utils.ValidationUtils;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Handles command execution actions
 */
public class CommandActionHandler implements ActionHandler {
     
    private static final Logger logger = Logger.getLogger(CommandActionHandler.class);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)\\}");
    
    @Override
    public String getActionType() {
        return "command";
    }
    
    @Override
    public ActionResult execute(FormPlayer player, String actionValue, ActionContext context) {
        if (player == null) {
            return ActionResult.failure("Player cannot be null");
        }
        
        if (ValidationUtils.isNullOrEmpty(actionValue)) {
            return ActionResult.failure("Command cannot be null or empty");
        }
        
        try {
            String processedCommand = processPlaceholders(actionValue, context);
            
            // Remove leading slash if present
            if (processedCommand.startsWith("/")) {
                processedCommand = processedCommand.substring(1);
            }
            
            // Validate command is not empty after processing
            if (ValidationUtils.isNullOrEmpty(processedCommand.trim())) {
                return ActionResult.failure("Processed command is empty");
            }
            
            // Execute the command
            player.executeAction("/" + processedCommand);
            boolean success = true; // Assume success since executeAction doesn't return a boolean
            
            if (success) {
                logger.debug("Successfully executed command '" + processedCommand + "' for player " + player.getName());
                return ActionResult.success("Command executed successfully");
            } else {
                logger.warn("Failed to execute command '" + processedCommand + "' for player " + player.getName());
                return ActionResult.failure("Command execution failed");
            }
            
        } catch (Exception e) {
            logger.error("Error executing command '" + actionValue + "' for player " + player.getName(), e);
            return ActionResult.failure("Command execution error: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isValidAction(String actionValue) {
        if (ValidationUtils.isNullOrEmpty(actionValue)) {
            return false;
        }
        
        String trimmed = actionValue.trim();
        
        // Check for basic command structure
        if (trimmed.length() == 0) {
            return false;
        }
        
        // Remove leading slash for validation
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        
        // Command should not be empty after removing slash
        if (trimmed.length() == 0) {
            return false;
        }
        
        // Basic validation - no newlines or dangerous characters
        if (trimmed.contains("\n") || trimmed.contains("\r")) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        return "Executes a server command. Supports placeholders for dynamic values.";
    }
    
    @Override
    public String[] getUsageExamples() {
        return new String[]{
            "give {player} diamond 1",
            "/tp {player} 0 100 0",
            "say Hello {player}!",
            "gamemode creative {player}"
        };
    }
    
    /**
     * Processes placeholders in the command string
     * @param command the command with placeholders
     * @param context the action context containing placeholder values
     * @return the processed command
     */
    private String processPlaceholders(String command, ActionContext context) {
        if (context == null) {
            return command;
        }
        
        String result = PlaceholderUtil.processDynamicPlaceholders(command, context.getPlaceholders());
        result = PlaceholderUtil.processFormResults(result, context.getFormResults());
        
        return result;
    }
}