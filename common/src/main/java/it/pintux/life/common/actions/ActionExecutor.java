package it.pintux.life.common.actions;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.platform.PlatformScheduler;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class ActionExecutor {

    private static final Logger logger = Logger.getLogger(ActionExecutor.class.getSimpleName());
    private final ActionRegistry registry;
    private PlatformScheduler scheduler;


    private static final Pattern NEW_FORMAT_PATTERN = Pattern.compile(
            "^\\s*(\\w+)\\s*\\{[\\s\\S]*\\}\\s*$", Pattern.DOTALL
    );


    private static final Pattern VALUE_PATTERN = Pattern.compile(
            "-\\s*\"([^\"\\\\]*+(?:\\\\.[^\"\\\\]*+)*+)\""
    );

    public ActionExecutor(ActionRegistry registry) {
        this.registry = registry;
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
            return fail(player, MessageData.ACTION_INVALID_FORMAT, null, null);
        }

        List<ActionSystem.ActionResult> results = new ArrayList<>();
        for (String actionType : action.getActionTypes()) {
            Object actionValue = action.getAction(actionType);
            ActionSystem.ActionResult result = executeSingleAction(player, actionType, actionValue, context);
            results.add(result);

            if (result.isFailure()) {
                return result;
            }
        }

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
            return fail(player, MessageData.ACTION_INVALID_FORMAT, null, null);
        }

        ActionSystem.ActionHandler handler = registry.getHandler(actionType);
        if (handler == null) {
            if (ActionRegistry.isPlatformAction(actionType)) {
                return fail(player, "action.unavailable", "type", actionType);
            }
            logger.warn("Unknown action type '" + actionType + "'. Available actions: " + registry.getRegisteredActionTypes());
            return fail(player, "action.unknown_type", "type", actionType);
        }

        String valueStr = actionValue != null ? actionValue.toString() : "";

        if (!handler.parsesRawActionBlock()) {
            valueStr = unwrapActionBlock(actionType, valueStr);
        }

        if (!handler.isValidAction(valueStr)) {
            logger.warn("Invalid action value '" + valueStr + "' for action type: " + actionType);
            return fail(player, MessageData.ACTION_INVALID_FORMAT, null, null);
        }

        try {
            logger.debug("Executing action: " + actionType + " with value: " + valueStr + " for player: " + player.getName());
            return handler.execute(player, valueStr, context);
        } catch (Exception e) {
            logger.error("Error executing action: " + actionType + " with value: " + actionValue, e);
            return fail(player, "action.failed", "action", actionType);
        }
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

        for (int i = 0; i < actions.size(); i++) {
            ActionSystem.Action action = actions.get(i);
            if (action == null) {
                results.add(fail(player, MessageData.ACTION_INVALID_FORMAT, null, null));
                continue;
            }

            Long pauseMs = bareDelayMillis(action);
            if (pauseMs != null && scheduler != null && i + 1 < actions.size()) {
                List<ActionSystem.Action> remaining = new ArrayList<>(actions.subList(i + 1, actions.size()));
                scheduler.runLaterSync(pauseMs, () -> executeActions(player, remaining, context));
                results.add(ActionSystem.ActionResult.success("Paused " + pauseMs + "ms before the remaining " + remaining.size() + " action(s)"));
                return results;
            }

            ActionSystem.ActionResult result = executeAction(player, action.getActionDefinition(), context);
            results.add(result);

            if (result.isFailure()) {
                break;
            }
        }

        return results;
    }

    /**
     * @return the pause a lone "delay" action asks for, or null when this action is anything else
     */
    private Long bareDelayMillis(ActionSystem.Action action) {
        ActionSystem.ActionDefinition definition = action.getActionDefinition();
        if (definition == null || definition.getActionTypes().size() != 1) {
            return null;
        }
        if (!definition.hasAction("delay")) {
            return null;
        }
        Object value = definition.getAction("delay");
        return it.pintux.life.common.actions.handlers.DelayActionHandler.bareDelayMillis(
                value != null ? value.toString() : null);
    }

    public void setScheduler(PlatformScheduler scheduler) {
        this.scheduler = scheduler;
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

        Matcher newFormatMatcher = NEW_FORMAT_PATTERN.matcher(trimmed);
        if (newFormatMatcher.matches()) {
            return parseNewFormat(trimmed);
        }

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

        ActionSystem.ActionDefinition actionDef = new ActionSystem.ActionDefinition();
        actionDef.addAction("command", trimmed);
        return new ActionSystem.Action(actionDef);
    }

    private String unwrapActionBlock(String actionType, String actionValue) {
        if (actionType == null || actionValue == null) {
            return actionValue;
        }

        Matcher blockMatcher = NEW_FORMAT_PATTERN.matcher(actionValue.trim());
        if (!blockMatcher.matches() || !blockMatcher.group(1).equalsIgnoreCase(actionType.trim())) {
            return actionValue;
        }

        Matcher valueMatcher = VALUE_PATTERN.matcher(actionValue);
        return valueMatcher.find() ? valueMatcher.group(1).trim() : "";
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

        List<String> values = new ArrayList<>();
        Matcher valueMatcher = VALUE_PATTERN.matcher(actionString);
        while (valueMatcher.find()) {
            values.add(valueMatcher.group(1));
        }

        if (values.isEmpty()) {
            logger.warn("No values found in new format action: " + actionString);
            return null;
        }

        ActionSystem.ActionDefinition actionDef = new ActionSystem.ActionDefinition();
        actionDef.addAction(actionType, actionString);

        return new ActionSystem.Action(actionDef);
    }

    private ActionSystem.ActionResult fail(FormPlayer player, String key, String name, Object value) {
        MessageData messageData;
        try {
            messageData = BedrockGUIApi.getInstance().getMessageData();
        } catch (IllegalStateException e) {
            return ActionSystem.ActionResult.failure(key);
        }
        Map<String, Object> replacements = name == null ? null : Map.of(name, value);
        return ActionSystem.ActionResult.failure(messageData.getValue(key, replacements, player));
    }
}
