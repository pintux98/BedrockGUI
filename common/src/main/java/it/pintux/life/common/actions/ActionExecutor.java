package it.pintux.life.common.actions;
import it.pintux.life.common.actions.ActionSystem;

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

    private static final Logger logger = Logger.getLogger(ActionExecutor.class.getSimpleName());
    private final ActionRegistry registry;
    private final ExecutorService executorService;


    private static final Pattern NEW_FORMAT_PATTERN = Pattern.compile(
            "^\\s*(\\w+)\\s*\\{[\\s\\S]*\\}\\s*$", Pattern.DOTALL
    );


    private static final Pattern VALUE_PATTERN = Pattern.compile(
            "-\\s*\"((?:[^\"\\\\]|\\\\.)*)\""
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
     * Executes a single ActionSystem.ActionDefinition synchronously
     *
     * @param player  the player executing the action
     * @param action  the action definition to execute
     * @param context the action context
     * @return the action result
     */
    public ActionSystem.ActionResult executeAction(FormPlayer player, ActionSystem.ActionDefinition action, ActionSystem.ActionContext context) {
        if (player == null) {
            return ActionSystem.ActionResult.failure("Player cannot be null");
        }

        if (action == null || action.isEmpty()) {
            return ActionSystem.ActionResult.failure("Action cannot be null or empty");
        }

        // Execute all actions in the definition
        List<ActionSystem.ActionResult> results = new ArrayList<>();
        for (String actionType : action.getActionTypes()) {
            Object actionValue = action.getAction(actionType);
            ActionSystem.ActionResult result = executeSingleAction(player, actionType, actionValue, context);
            results.add(result);

            // Stop on first failure for now (can be made configurable later)
            if (result.isFailure()) {
                return result;
            }
        }

        // Return success if all actions succeeded
        return results.isEmpty() ? ActionSystem.ActionResult.success("No actions to execute") : results.get(results.size() - 1);
    }

    /**
     * Executes a single action type with value
     *
     * @param player      the player executing the action
     * @param actionType  the type of action
     * @param actionValue the action value/parameters
     * @param context     the action context
     * @return the action result
     */
    public ActionSystem.ActionResult executeSingleAction(FormPlayer player, String actionType, Object actionValue, ActionSystem.ActionContext context) {
        if (player == null) {
            return ActionSystem.ActionResult.failure("Player cannot be null");
        }

        if (ValidationUtils.isNullOrEmpty(actionType)) {
            return ActionSystem.ActionResult.failure("Action type cannot be null or empty");
        }

        ActionSystem.ActionHandler handler = registry.getHandler(actionType);
        if (handler == null) {
            System.out.println("[BedrockGUI] Invalid action type '" + actionType + "' - not registered. Available actions: " + registry.getRegisteredActionTypes());
            logger.warn("No handler found for action type: " + actionType);
            return ActionSystem.ActionResult.failure("Unknown action type: " + actionType);
        }

        String valueStr = actionValue != null ? actionValue.toString() : "";

        if (!handler.isValidAction(valueStr)) {
            logger.warn("Invalid action value '" + valueStr + "' for action type: " + actionType);
            return ActionSystem.ActionResult.failure("Invalid action value for type: " + actionType);
        }

        try {
            logger.debug("Executing action: " + actionType + " with value: " + valueStr + " for player: " + player.getName());
            return handler.execute(player, valueStr, context);
        } catch (Exception e) {
            logger.error("Error executing action: " + actionType + " with value: " + actionValue, e);
            return ActionSystem.ActionResult.failure("Action execution failed: " + e.getMessage());
        }
    }

    /**
     * Executes a single action asynchronously
     *
     * @param player  the player executing the action
     * @param action  the action definition to execute
     * @param context the action context
     * @return CompletableFuture with the action result
     */
    public CompletableFuture<ActionSystem.ActionResult> executeActionAsync(FormPlayer player, ActionSystem.ActionDefinition action, ActionSystem.ActionContext context) {
        return CompletableFuture.supplyAsync(() -> executeAction(player, action, context), executorService);
    }

    /**
     * Executes multiple actions in sequence
     *
     * @param player  the player executing the actions
     * @param actions list of actions to execute
     * @param context the action context
     * @return list of action results
     */
    public List<ActionSystem.ActionResult> executeActions(FormPlayer player, List<ActionSystem.Action> actions, ActionSystem.ActionContext context) {
        List<ActionSystem.ActionResult> results = new ArrayList<>();

        if (actions == null || actions.isEmpty()) {
            return results;
        }

        for (ActionSystem.Action action : actions) {
            if (action == null) {
                results.add(ActionSystem.ActionResult.failure("Action cannot be null"));
                continue;
            }

            ActionSystem.ActionResult result = executeAction(player, action.getActionDefinition(), context);
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
     *
     * @param player  the player executing the actions
     * @param actions list of actions to execute
     * @param context the action context
     * @return CompletableFuture with list of action results
     */
    public CompletableFuture<List<ActionSystem.ActionResult>> executeActionsAsync(FormPlayer player, List<ActionSystem.Action> actions, ActionSystem.ActionContext context) {
        return CompletableFuture.supplyAsync(() -> executeActions(player, actions, context), executorService);
    }

    /**
     * Parses an action string in the format "type:value" or just "value" (defaults to command)
     *
     * @param actionString the action string to parse
     * @return parsed Action object
     */
    public ActionSystem.Action parseAction(String actionString) {
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

                ActionSystem.ActionDefinition actionDef = new ActionSystem.ActionDefinition();
                actionDef.addAction(actionType, actionValue);
                return new ActionSystem.Action(actionDef);
            }
        }

        // Default to command if no type specified
        ActionSystem.ActionDefinition actionDef = new ActionSystem.ActionDefinition();
        actionDef.addAction("command", trimmed);
        return new ActionSystem.Action(actionDef);
    }

    /**
     * Parses the new unified action format
     */
    private ActionSystem.Action parseNewFormat(String actionString) {
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

        // Create ActionSystem.ActionDefinition with the entire action string
        // The individual action handlers will parse the curly brace format themselves
        ActionSystem.ActionDefinition actionDef = new ActionSystem.ActionDefinition();
        actionDef.addAction(actionType, actionString);

        return new ActionSystem.Action(actionDef);
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


}

