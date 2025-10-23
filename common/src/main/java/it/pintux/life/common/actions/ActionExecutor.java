package it.pintux.life.common.actions;

import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;
import it.pintux.life.common.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class ActionExecutor {
    
    private static final Logger logger = Logger.getLogger(ActionExecutor.class);
    private final ActionRegistry registry;
    private final ExecutorService executorService;
    
    
    private static final Pattern NEW_FORMAT_PATTERN = Pattern.compile(
        "^\\s*(\\w+)\\s*\\{[\\s\\S]*\\}\\s*$", Pattern.DOTALL
    );
    
    
    private static final Pattern VALUE_PATTERN = Pattern.compile(
        "-\\s*\"([^\"]+)\""
    );
    
    public ActionExecutor(ActionRegistry registry) {
        this.registry = registry;
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "ActionExecutor-" + System.currentTimeMillis());
            thread.setDaemon(true);
            return thread;
        });
    }
    
    /**
     * Executes a single ActionDefinition synchronously
     * @param player the player executing the action
     * @param action the action definition to execute
     * @param context the action context
     * @return the action result
     */
    public ActionResult executeAction(FormPlayer player, ActionDefinition action, ActionContext context) {
        if (player == null) {
            return ActionResult.failure("Player cannot be null");
        }
        
        if (action == null || action.isEmpty()) {
            return ActionResult.failure("Action cannot be null or empty");
        }
        
        // Execute all actions in the definition
        List<ActionResult> results = new ArrayList<>();
        for (String actionType : action.getActionTypes()) {
            Object actionValue = action.getAction(actionType);
            ActionResult result = executeSingleAction(player, actionType, actionValue, context);
            results.add(result);
            
            // Stop on first failure for now (can be made configurable later)
            if (result.isFailure()) {
                return result;
            }
        }
        
        // Return success if all actions succeeded
        return results.isEmpty() ? ActionResult.success("No actions to execute") : results.get(results.size() - 1);
    }
    
    /**
     * Executes a single action type with value
     * @param player the player executing the action
     * @param actionType the type of action
     * @param actionValue the action value/parameters
     * @param context the action context
     * @return the action result
     */
    public ActionResult executeSingleAction(FormPlayer player, String actionType, Object actionValue, ActionContext context) {
        if (player == null) {
            return ActionResult.failure("Player cannot be null");
        }
        
        if (ValidationUtils.isNullOrEmpty(actionType)) {
            return ActionResult.failure("Action type cannot be null or empty");
        }
        
        ActionHandler handler = registry.getHandler(actionType);
        if (handler == null) {
            System.out.println("[BedrockGUI] Invalid action type '" + actionType + "' - not registered. Available actions: " + registry.getRegisteredActionTypes());
            logger.warn("No handler found for action type: " + actionType);
            return ActionResult.failure("Unknown action type: " + actionType);
        }
        
        String valueStr = actionValue != null ? actionValue.toString() : "";
        
        if (!handler.isValidAction(valueStr)) {
            logger.warn("Invalid action value '" + valueStr + "' for action type: " + actionType);
            return ActionResult.failure("Invalid action value for type: " + actionType);
        }
        
        try {
            logger.debug("Executing action: " + actionType + " with value: " + valueStr + " for player: " + player.getName());
            return handler.execute(player, valueStr, context);
        } catch (Exception e) {
            logger.error("Error executing action: " + actionType + " with value: " + actionValue, e);
            return ActionResult.failure("Action execution failed: " + e.getMessage());
        }
    }
    
    /**
     * Evaluates a condition string (simplified implementation)
     */
    private boolean evaluateCondition(FormPlayer player, String condition, ActionContext context) {
        // This is a simplified implementation - in a real system you'd want
        // a proper expression parser for complex conditions
        
        if (condition.contains("placeholder=")) {
            // Handle placeholder conditions like: placeholder=%vault_eco_balance%>=100
            // For now, just return true (implement proper placeholder evaluation later)
            return true;
        }
        
        if (condition.contains("permission=")) {
            // Handle permission conditions like: permission=vip.access
            String permission = condition.substring(condition.indexOf("permission=") + 11);
            return player.hasPermission(permission.trim());
        }
        
        if (condition.contains("world=")) {
            // TODO: Handle world conditions like: world=survival
            // FormPlayer interface doesn't have getWorld() method
            // This requires platform-specific implementation via PlatformPlayerManager
            logger.warn("World conditions are not yet supported in simplified condition evaluation");
            return true; // Default to true for now
        }
        
        // Default to true for unknown conditions (should be enhanced)
        return true;
    }
    
    /**
     * Executes a single action asynchronously
     * @param player the player executing the action
     * @param action the action definition to execute
     * @param context the action context
     * @return CompletableFuture with the action result
     */
    public CompletableFuture<ActionResult> executeActionAsync(FormPlayer player, ActionDefinition action, ActionContext context) {
        return CompletableFuture.supplyAsync(() -> executeAction(player, action, context), executorService);
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
            
            ActionResult result = executeAction(player, action.getActionDefinition(), context);
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
        
        // Check for the new unified format first
        Matcher newFormatMatcher = NEW_FORMAT_PATTERN.matcher(trimmed);
        if (newFormatMatcher.matches()) {
            return parseNewFormat(trimmed);
        }
        
        // Legacy format support for backward compatibility
        // Handle simple "type: value" format
        if (trimmed.contains(":")) {
            String[] parts = trimmed.split(":", 2);
            if (parts.length == 2) {
                String actionType = parts[0].trim();
                String actionValue = parts[1].trim();
                
                ActionDefinition actionDef = new ActionDefinition();
                actionDef.addAction(actionType, actionValue);
                return new Action(actionDef);
            }
        }
        
        // Default to command if no type specified
        ActionDefinition actionDef = new ActionDefinition();
        actionDef.addAction("command", trimmed);
        return new Action(actionDef);
    }
    
    /**
     * Parses the new unified action format
     */
    private Action parseNewFormat(String actionString) {
        Matcher matcher = NEW_FORMAT_PATTERN.matcher(actionString);
        if (!matcher.matches()) {
            return null;
        }
        
        String actionType = matcher.group(1).toLowerCase();
        
        // Extract all values from the action string
        List<String> values = new ArrayList<>();
        Matcher valueMatcher = VALUE_PATTERN.matcher(actionString);
        while (valueMatcher.find()) {
            values.add(valueMatcher.group(1));
        }
        
        if (values.isEmpty()) {
            logger.warn("No values found in new format action: " + actionString);
            return null;
        }
        
        // Create ActionDefinition with the entire action string
        // The individual action handlers will parse the curly brace format themselves
        ActionDefinition actionDef = new ActionDefinition();
        actionDef.addAction(actionType, actionString);
        
        return new Action(actionDef);
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
     * Represents a single action with metadata
     */
    public static class Action {
        private final ActionDefinition actionDefinition;
        private final boolean critical;
        
        public Action(ActionDefinition actionDefinition) {
            this(actionDefinition, false);
        }
        
        public Action(ActionDefinition actionDefinition, boolean critical) {
            this.actionDefinition = actionDefinition;
            this.critical = critical;
        }
        
        // Legacy constructors removed - use ActionDefinition instead
        
        public ActionDefinition getActionDefinition() {
            return actionDefinition;
        }
        
        public boolean isCritical() {
            return critical;
        }
        
        @Override
        public String toString() {
            return "Action{actionDefinition=" + actionDefinition + ", critical=" + critical + "}";
        }
    }
}
