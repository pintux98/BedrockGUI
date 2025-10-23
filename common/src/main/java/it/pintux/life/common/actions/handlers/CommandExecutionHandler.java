package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.platform.PlatformCommandExecutor;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;


public class CommandExecutionHandler extends BaseActionHandler {
    
    private final PlatformCommandExecutor commandExecutor;
    
    public CommandExecutionHandler() {
        this.commandExecutor = null; 
    }
    
    public CommandExecutionHandler(PlatformCommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }
    
    @Override
    public String getActionType() {
        return "command";
    }
    
    @Override
    public ActionResult execute(FormPlayer player, String actionValue, ActionContext context) {
        
        ActionResult validationResult = validateActionParameters(player, actionValue, 
            () -> isValidAction(actionValue));
        if (validationResult != null) {
            return validationResult;
        }
        
        try {
            
            boolean isServerCommand = actionValue.toLowerCase().startsWith("server:");
            
            if (isServerCommand) {
                return executeServerCommand(player, actionValue, context);
            } else {
                return executePlayerCommand(player, actionValue, context);
            }
            
        } catch (Exception e) {
            logError("command", actionValue, player, e);
            return createFailureResult("ACTION_EXECUTION_ERROR", 
                createReplacements("error", e.getMessage()), player, e);
        }
    }
    
    
    private ActionResult executePlayerCommand(FormPlayer player, String actionValue, ActionContext context) {
        String processedCommand = processPlaceholders(actionValue, context, player);
        String normalizedCommand = normalizeCommand(processedCommand);
        
        if (normalizedCommand.isEmpty()) {
            return createFailureResult("ACTION_EXECUTION_ERROR", 
                createReplacements("error", "Empty command after processing"), player);
        }
        
        boolean success = executeWithErrorHandling(
            () -> player.executeAction("/" + normalizedCommand),
            "Player command: " + normalizedCommand,
            player
        );
        
        if (success) {
            logSuccess("command", normalizedCommand, player);
            return createSuccessResult(MessageData.ACTION_COMMAND_SUCCESS, 
                createReplacements("command", normalizedCommand), player);
        } else {
            logFailure("command", normalizedCommand, player);
            return createFailureResult(MessageData.ACTION_COMMAND_FAILED, 
                createReplacements("command", normalizedCommand), player);
        }
    }
    
    
    private ActionResult executeServerCommand(FormPlayer player, String actionValue, ActionContext context) {
        if (commandExecutor == null) {
            return createFailureResult("ACTION_EXECUTION_ERROR", 
                createReplacements("error", "Server command execution not available"), player);
        }
        
        
        String serverCommand = actionValue.substring(7).trim();
        String processedCommand = processPlaceholders(serverCommand, context, player);
        
        if (processedCommand.trim().isEmpty()) {
            return createFailureResult("ACTION_EXECUTION_ERROR", 
                createReplacements("error", "Empty server command after processing"), player);
        }
        
        boolean success = executeWithErrorHandling(
            () -> commandExecutor.executeAsConsole(processedCommand),
            "Server command: " + processedCommand,
            player
        );
        
        if (success) {
            logSuccess("server command", processedCommand, player);
            return createSuccessResult("ACTION_SUCCESS", 
                createReplacements("message", "Server command executed successfully"), player);
        } else {
            logFailure("server command", processedCommand, player);
            return createFailureResult("ACTION_EXECUTION_ERROR", 
                createReplacements("error", "Failed to execute server command"), player);
        }
    }
    
    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = actionValue.trim();
        
        
        if (trimmed.toLowerCase().startsWith("server:")) {
            String serverCommand = trimmed.substring(7).trim();
            return !serverCommand.isEmpty();
        }
        
        
        return isValidCommand(trimmed);
    }
    
    @Override
    public String getDescription() {
        return "Executes commands either as the player or as the server console. " +
               "Use 'server:' prefix for console commands. Supports placeholders for dynamic values.";
    }
    
    @Override
    public String[] getUsageExamples() {
        return new String[]{
                
                "give $player diamond 1",
                "/tp $player 0 100 0", 
                "say Hello $player!",
                "gamemode creative $player",
                
                "server:give {player} diamond 64",
                "server:gamemode creative {player}",
                "server:tp {player} spawn",
                "server:lp user {player} parent add vip"
        };
    }
}
