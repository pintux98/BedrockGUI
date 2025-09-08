package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.platform.PlatformCommandExecutor;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.PlaceholderUtil;
import it.pintux.life.common.utils.ErrorHandlingUtil;

import java.util.Map;

/**
 * Handles server command execution where commands are run by the server console
 * rather than by the player. This allows for administrative commands that
 * players cannot normally execute.
 * 
 * Usage: server:give {player} diamond 64
 * Usage: server:gamemode creative {player}
 * Usage: server:tp {player} spawn
 */
public class ServerActionHandler extends BaseActionHandler {
    private final PlatformCommandExecutor commandExecutor;
    
    public ServerActionHandler(PlatformCommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }
    
    @Override
    public String getActionType() {
        return "server";
    }
    
    private boolean validateParameters(String actionData) {
        return actionData != null && !actionData.trim().isEmpty();
    }

    @Override
    public ActionResult execute(FormPlayer player, String actionData, ActionContext context) {
        if (!validateParameters(actionData)) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid action parameters"), player);
        }
        
        if (actionData == null || actionData.trim().isEmpty()) {
            logger.warn("Server action called with empty command for player: " + player.getName());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "No server command specified"), player);
        }
        
        try {
            // Process placeholders in the command
            String processedCommand = processPlaceholders(actionData.trim(), context, player);
            
            logger.info("Executing server command for player " + player.getName() + ": " + processedCommand);
            
            // Execute command with enhanced error handling and retry logic
            boolean success = ErrorHandlingUtil.executeCommandWithFallback(
                () -> commandExecutor.executeAsConsole(processedCommand),
                "Server command: " + processedCommand,
                player
            );
            
            if (success) {
                logger.info("Server command executed successfully");
                return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Server command executed successfully"), player);
            } else {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to execute server command after retries"), player);
            }
            
        } catch (Exception e) {
            logger.error("Error executing server command for player " + player.getName() + ": " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Error executing server command: " + e.getMessage()), player);
        }
    }
    
    @Override
    public boolean isValidAction(String actionValue) {
        return actionValue != null && !actionValue.trim().isEmpty();
    }
    
    @Override
    public String getDescription() {
        return "Executes commands as the server console with full administrative privileges";
    }
    
    @Override
    public String[] getUsageExamples() {
        return new String[]{
            "server:give {player} diamond 64",
            "server:gamemode creative {player}",
            "server:tp {player} spawn",
            "server:lp user {player} parent add vip"
        };
    }
    
    protected String processPlaceholders(String command, ActionContext context, FormPlayer player) {
        if (context == null) {
            return command;
        }
        
        String result = command;
        
        // Replace placeholders from context
        if (context.getPlaceholders() != null) {
            for (Map.Entry<String, String> entry : context.getPlaceholders().entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                result = result.replace(placeholder, entry.getValue());
            }
        }
        
        // Process dynamic placeholders (prefixed with $)
        result = PlaceholderUtil.processDynamicPlaceholders(result, context.getPlaceholders());
        
        // Process form results
        result = PlaceholderUtil.processFormResults(result, context.getFormResults());
        
        // Process PlaceholderAPI placeholders if MessageData is available
        if (context.getMetadata() != null && context.getMetadata().containsKey("messageData")) {
            Object messageData = context.getMetadata().get("messageData");
            result = PlaceholderUtil.processPlaceholders(result, player, messageData);
        }
        
        return result;
    }
}