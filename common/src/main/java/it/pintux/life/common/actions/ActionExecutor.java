package it.pintux.life.common.actions;

import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;
import it.pintux.life.common.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Executes actions using registered action handlers
 */
public class ActionExecutor {
    
    private static final Logger logger = Logger.getLogger(ActionExecutor.class);
    private final ActionRegistry registry;
    private final ExecutorService executorService;
    
    public ActionExecutor(ActionRegistry registry) {
        this.registry = registry;
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "ActionExecutor-" + System.currentTimeMillis());
            thread.setDaemon(true);
            return thread;
        });
    }
    
    /**
     * Executes a single action synchronously
     * @param player the player executing the action
     * @param actionType the type of action
     * @param actionValue the action value/parameters
     * @param context the action context
     * @return the action result
     */
    public ActionResult executeAction(FormPlayer player, String actionType, String actionValue, ActionContext context) {
        if (player == null) {
            return ActionResult.failure("Player cannot be null");
        }
        
        if (ValidationUtils.isNullOrEmpty(actionType)) {
            return ActionResult.failure("Action type cannot be null or empty");
        }
        
        ActionHandler handler = registry.getHandler(actionType);
        if (handler == null) {
            logger.warn("No handler found for action type: " + actionType);
            return ActionResult.failure("Unknown action type: " + actionType);
        }
        
        if (!handler.isValidAction(actionValue)) {
            logger.warn("Invalid action value '" + actionValue + "' for action type: " + actionType);
            return ActionResult.failure("Invalid action value for type: " + actionType);
        }
        
        try {
            logger.debug("Executing action: " + actionType + " with value: " + actionValue + " for player: " + player.getName());
            ActionResult result = handler.execute(player, actionValue, context);
            
            if (result.isFailure()) {
                logger.warn("Action execution failed: " + result.getMessage());
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Unexpected error executing action: " + actionType, e);
            return ActionResult.failure("Unexpected error: " + e.getMessage(), e);
        }
    }
    
    /**
     * Executes a single action asynchronously
     * @param player the player executing the action
     * @param actionType the type of action
     * @param actionValue the action value/parameters
     * @param context the action context
     * @return CompletableFuture with the action result
     */
    public CompletableFuture<ActionResult> executeActionAsync(FormPlayer player, String actionType, String actionValue, ActionContext context) {
        return CompletableFuture.supplyAsync(() -> executeAction(player, actionType, actionValue, context), executorService);
    }
    
    /**
     * Executes multiple actions in sequence
     * @param player the player executing the actions
     * @param actions list of actions to execute
     * @param context the action context
     * @return list of action results
     */
    public List<ActionResult> executeActions(FormPlayer player, List<Action> actions, ActionContext context) {
        List<ActionResult> results = new ArrayList<>();
        
        if (actions == null || actions.isEmpty()) {
            return results;
        }
        
        for (Action action : actions) {
            if (action == null) {
                results.add(ActionResult.failure("Action cannot be null"));
                continue;
            }
            
            ActionResult result = executeAction(player, action.getType(), action.getValue(), context);
            results.add(result);
            
            // Stop execution if action failed and is marked as critical
            if (result.isFailure() && action.isCritical()) {
                logger.warn("Critical action failed, stopping execution chain");
                break;
            }
        }
        
        return results;
    }
    
    /**
     * Executes multiple actions asynchronously
     * @param player the player executing the actions
     * @param actions list of actions to execute
     * @param context the action context
     * @return CompletableFuture with list of action results
     */
    public CompletableFuture<List<ActionResult>> executeActionsAsync(FormPlayer player, List<Action> actions, ActionContext context) {
        return CompletableFuture.supplyAsync(() -> executeActions(player, actions, context), executorService);
    }
    
    /**
     * Parses an action string in the format "type:value" or just "value" (defaults to command)
     * @param actionString the action string to parse
     * @return parsed Action object
     */
    public Action parseAction(String actionString) {
        if (ValidationUtils.isNullOrEmpty(actionString)) {
            return null;
        }
        
        String trimmed = actionString.trim();
        
        // Check if action string contains type separator
        if (trimmed.contains(":")) {
            String[] parts = trimmed.split(":", 2);
            if (parts.length == 2) {
                String type = parts[0].trim();
                String value = parts[1].trim();
                return new Action(type, value);
            }
        }
        
        // Check if the first word matches a registered action type
        String[] words = trimmed.split("\\s+", 2);
        if (words.length >= 1) {
            String firstWord = words[0].toLowerCase();
            if (registry.hasHandler(firstWord)) {
                String value = words.length > 1 ? words[1] : "";
                return new Action(firstWord, value);
            }
        }
        
        // Default to command action if no type specified
        return new Action("command", trimmed);
    }
    
    /**
     * Shuts down the executor service
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            logger.info("ActionExecutor shutdown completed");
        }
    }
    
    /**
     * Represents a single action
     */
    public static class Action {
        private final String type;
        private final String value;
        private final boolean critical;
        
        public Action(String type, String value) {
            this(type, value, false);
        }
        
        public Action(String type, String value, boolean critical) {
            this.type = type;
            this.value = value;
            this.critical = critical;
        }
        
        public String getType() {
            return type;
        }
        
        public String getValue() {
            return value;
        }
        
        public boolean isCritical() {
            return critical;
        }
        
        @Override
        public String toString() {
            return "Action{type='" + type + "', value='" + value + "', critical=" + critical + "}";
        }
    }
}