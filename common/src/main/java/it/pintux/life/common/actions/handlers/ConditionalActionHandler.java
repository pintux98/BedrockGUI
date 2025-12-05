package it.pintux.life.common.actions.handlers;
import it.pintux.life.common.actions.ActionSystem;

import it.pintux.life.common.actions.ActionSystem;

import it.pintux.life.common.actions.*;
import it.pintux.life.common.actions.ActionRegistry;
import it.pintux.life.common.utils.ConditionEvaluator;
import it.pintux.life.common.utils.FormPlayer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConditionalActionHandler extends BaseActionHandler {

    private final ActionExecutor actionExecutor;

    private static final Pattern CHECK_PATTERN = Pattern.compile(
            "check:\\s*\"([^\"]+)\""
    );

    private static final Pattern TRUE_ACTIONS_PATTERN = Pattern.compile(
            "true:\\s*([\\s\\S]*?)(?=\\s*false:|\\Z)", Pattern.DOTALL
    );

    private static final Pattern FALSE_ACTIONS_PATTERN = Pattern.compile(
            "false:\\s*([\\s\\S]*?)(?=\\Z)", Pattern.DOTALL
    );

    private static final Pattern ACTION_LINE_PATTERN = Pattern.compile(
            "-\\s*\"((?:[^\"\\\\]|\\\\.)*)\""
    );

    public ConditionalActionHandler(ActionExecutor actionExecutor) {
        this.actionExecutor = actionExecutor;
    }

    @Override
    public String getActionType() {
        return "conditional";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionData, ActionSystem.ActionContext context) {
        if (actionData == null || actionData.trim().isEmpty()) {
            logger.warn("Conditional action called with empty data for player: " + player.getName());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "No conditional data specified"), player);
        }

        try {
            String processedData = processPlaceholders(actionData.trim(), context, player);
            return executeNewFormat(player, processedData, context);

        } catch (Exception e) {
            logger.error("Error executing conditional action for player " + player.getName() + ": " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Error executing conditional action: " + e.getMessage()), player);
        }
    }


    private ActionSystem.ActionResult executeNewFormat(FormPlayer player, String actionData, ActionSystem.ActionContext context) {
        try {

            Matcher checkMatcher = CHECK_PATTERN.matcher(actionData);
            if (!checkMatcher.find()) {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "No check condition found in conditional"), player);
            }

            String checkCondition = checkMatcher.group(1);
            boolean conditionMet = evaluateComplexCondition(player, checkCondition, context);

            List<String> actionsToExecute;
            if (conditionMet) {
                actionsToExecute = parseActionList(actionData, TRUE_ACTIONS_PATTERN);
                logger.info("Condition met for player " + player.getName() + ", executing " + actionsToExecute.size() + " success actions");
            } else {
                actionsToExecute = parseActionList(actionData, FALSE_ACTIONS_PATTERN);
                if (actionsToExecute.isEmpty()) {
                    logger.info("Condition not met for player " + player.getName() + ": " + checkCondition + " (no failure actions specified)");
                    return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Condition not met, action skipped"), player);
                }
                logger.info("Condition not met for player " + player.getName() + ", executing " + actionsToExecute.size() + " failure actions");
            }


            ActionSystem.ActionResult lastResult = null;
            for (String action : actionsToExecute) {
                ActionSystem.Action parsed = actionExecutor.parseAction(action);
                ActionSystem.ActionDefinition actionDef = parsed != null ? parsed.getActionDefinition() : parseActionString(action);
                ActionSystem.ActionResult result = actionExecutor.executeAction(player, actionDef, context);
                lastResult = result;

                if (!result.isSuccess()) {
                    logger.warn("Action failed during conditional execution: " + action + " - " + result.message());

                }
            }

            if (lastResult != null && lastResult.isSuccess()) {
                return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Conditional actions executed successfully"), player);
            } else {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Some conditional actions failed"), player);
            }

        } catch (Exception e) {
            logger.error("Error executing new format conditional for player " + player.getName() + ": " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Error parsing new conditional format: " + e.getMessage()), player);
        }
    }


    private boolean evaluateComplexCondition(FormPlayer player, String condition, ActionSystem.ActionContext context) {

        if (condition.contains("||")) {
            String[] orParts = condition.split("\\|\\|");
            for (String orPart : orParts) {
                if (evaluateComplexCondition(player, orPart.trim(), context)) {
                    return true;
                }
            }
            return false;
        }


        if (condition.contains("&&")) {
            String[] andParts = condition.split("&&");
            for (String andPart : andParts) {
                if (!evaluateComplexCondition(player, andPart.trim(), context)) {
                    return false;
                }
            }
            return true;
        }


        return evaluateSingleCondition(player, condition.trim(), context);
    }


    private boolean evaluateSingleCondition(FormPlayer player, String condition, ActionSystem.ActionContext context) {

        if (condition.startsWith("placeholder:")) {
            return evaluatePlaceholderCondition(player, condition, context);
        } else if (condition.startsWith("permission:")) {
            return evaluatePermissionCondition(player, condition, context);
        } else {
            logger.warn("Unknown condition type in: " + condition);
            return false;
        }
    }


    private boolean evaluatePlaceholderCondition(FormPlayer player, String condition, ActionSystem.ActionContext context) {

        String conditionPart = condition.substring("placeholder:".length());


        String[] operators = {">=", "<=", "==", "!=", ">", "<"};
        String operator = null;
        String[] parts = null;

        for (String op : operators) {
            if (conditionPart.contains(" " + op + " ")) {
                operator = op;
                parts = conditionPart.split(" " + Pattern.quote(op) + " ", 2);
                break;
            }
        }

        if (operator == null || parts == null || parts.length != 2) {
            logger.warn("Invalid placeholder condition format: " + condition);
            return false;
        }

        String placeholder = parts[0].trim();
        String expectedValue = parts[1].trim();


        String conditionString = "placeholder:" + placeholder + ":" + operator + ":" + expectedValue;
        return ConditionEvaluator.evaluateCondition(player, conditionString, context, null);
    }


    private boolean evaluatePermissionCondition(FormPlayer player, String condition, ActionSystem.ActionContext context) {
        String permission = condition.substring("permission:".length()).trim();
        String conditionString = "permission:" + permission;
        return ConditionEvaluator.evaluateCondition(player, conditionString, context, null);
    }


    private List<String> parseActionList(String actionData, Pattern pattern) {
        List<String> actions = new ArrayList<>();
        Matcher matcher = pattern.matcher(actionData);

        if (matcher.find()) {
            String actionsBlock = matcher.group(1);
            // First, handle multiple inline '- |' actions on a single line
            java.util.regex.Matcher inlineMulti = java.util.regex.Pattern.compile("-\\s*\\|\\s*").matcher(actionsBlock);
            if (inlineMulti.find()) {
                java.util.regex.Matcher splitter = java.util.regex.Pattern.compile("-\\s*\\|\\s*(.+?)(?=\\s*-\\s*\\||\\Z)", java.util.regex.Pattern.DOTALL).matcher(actionsBlock);
                while (splitter.find()) {
                    String content = splitter.group(1).trim();
                    if (!content.isEmpty()) {
                        actions.add(content);
                    }
                }
                return actions;
            }

            String[] lines = actionsBlock.split("\\r?\\n");
            int i = 0;
            while (i < lines.length) {
                String line = lines[i];
                if (line.trim().isEmpty()) { i++; continue; }

                Matcher pipeMatcher = Pattern.compile("^\\s*-\\s*\\|\\s*$").matcher(line);
                if (pipeMatcher.matches()) {
                    int dashIndex = line.indexOf('-');
                    String indent = dashIndex > 0 ? line.substring(0, dashIndex) : "";
                    StringBuilder sb = new StringBuilder();
                    i++;
                    while (i < lines.length) {
                        String next = lines[i];
                        if (next.startsWith(indent + "-")) {
                            break;
                        }
                        sb.append(next).append("\n");
                        i++;
                    }
                    String content = sb.toString().trim();
                    if (!content.isEmpty()) {
                        actions.add(content);
                    }
                    continue;
                }

                Matcher simpleMatcher = ACTION_LINE_PATTERN.matcher(line);
                if (simpleMatcher.find()) {
                    String value = simpleMatcher.group(1).trim();
                    if (!value.isEmpty()) {
                        actions.add(value);
                    }
                } else {
                    Matcher inlinePipe = Pattern.compile("^\\s*-\\s*\\|\\s*(.+)$").matcher(line);
                    if (inlinePipe.find()) {
                        String content = inlinePipe.group(1).trim();
                        if (!content.isEmpty()) {
                            actions.add(content);
                        }
                    }
                }

                i++;
            }
        }

        return actions;
    }


    private ActionSystem.ActionDefinition parseActionString(String actionString) {
        ActionSystem.ActionDefinition actionDef = new ActionSystem.ActionDefinition();


        actionString = actionString.replaceAll("^\"|\"$", "");


        int colonIndex = actionString.indexOf(':');
        if (colonIndex != -1) {
            String actionType = actionString.substring(0, colonIndex);
            String actionData = actionString.substring(colonIndex + 1);
            actionDef.addAction(actionType, actionData);
        } else {

            actionDef.addAction("command", actionString);
        }

        return actionDef;
    }

    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }

        String[] parts = actionValue.split(":");
        if (parts.length < 4) {
            return false;
        }

        int offset = 1;


        if (parts[offset].equalsIgnoreCase("not")) {
            offset++;
            if (parts.length < 5) {
                return false;
            }
        }

        String conditionType = parts[offset].replaceAll("\"", "").trim();

        switch (conditionType.toLowerCase()) {
            case "permission":
                return parts.length >= offset + 4;
            case "placeholder":
                return parts.length >= offset + 6;
            default:
                return false;
        }
    }

    @Override
    public String getDescription() {
        return "Executes actions based on conditions like permissions or placeholder values";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{};
    }
}

