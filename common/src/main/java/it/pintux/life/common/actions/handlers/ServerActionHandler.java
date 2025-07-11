package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionHandler;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.platform.PlatformCommandExecutor;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;

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
public class ServerActionHandler implements ActionHandler {
    private static final Logger logger = Logger.getLogger(ServerActionHandler.class);
    private final PlatformCommandExecutor commandExecutor;
    
    public ServerActionHandler(PlatformCommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }
    
    @Override
    public String getActionType() {
        return "server";
    }
    
    @Override
    public ActionResult execute(FormPlayer player, String actionData, ActionContext context) {
        if (actionData == null || actionData.trim().isEmpty()) {
            logger.warn("Server action called with empty command for player: " + player.getName());
            return ActionResult.failure("No server command specified");
        }
        
        try {
            // Process placeholders in the command
            String processedCommand = processPlaceholders(actionData.trim(), context);
            
            logger.info("Executing server command for player " + player.getName() + ": " + processedCommand);
            
            // Execute command as console/server through platform abstraction
            boolean success = commandExecutor.executeAsConsole(processedCommand);
            
            if (success) {
                logger.info("Server command executed successfully: " + processedCommand);
                return ActionResult.success("Server command executed: " + processedCommand);
            } else {
                logger.warn("Server command failed to execute: " + processedCommand);
                return ActionResult.failure("Failed to execute server command: " + processedCommand);
            }
            
        } catch (Exception e) {
            logger.error("Error executing server command for player " + player.getName() + ": " + e.getMessage());
            return ActionResult.failure("Error executing server command: " + e.getMessage());
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
    
    private String processPlaceholders(String command, ActionContext context) {
        if (context == null) {
            return command;
        }
        
        String result = command;
        Map<String, String> placeholders = context.getPlaceholders();
        
        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                String value = entry.getValue() != null ? entry.getValue() : "";
                result = result.replace(placeholder, value);
            }
        }
        
        // Process form results as placeholders
        Map<String, Object> formResults = context.getFormResults();
        if (formResults != null && !formResults.isEmpty()) {
            for (Map.Entry<String, Object> entry : formResults.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                result = result.replace(placeholder, value);
            }
        }
        
        return result;
    }
}