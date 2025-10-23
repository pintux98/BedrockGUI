package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.*;
import it.pintux.life.common.actions.ActionRegistry;
import it.pintux.life.common.utils.ConditionEvaluator;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConditionalActionHandler extends BaseActionHandler {

    private final ActionExecutor actionExecutor;
    private final ActionRegistry actionRegistry;
    
    
    private static final Pattern NEW_FORMAT_PATTERN = Pattern.compile(
        "^conditional\\s*\\{[\\s\\S]*\\}$", Pattern.MULTILINE
    );
    
    
    private static final Pattern CHECK_PATTERN = Pattern.compile(
        "check:\\s*\"([^\"]+)\""
    );
    
    private static final Pattern TRUE_ACTIONS_PATTERN = Pattern.compile(
        "true:\\s*([^}]*?)(?=false:|\\})"
    );
    
    private static final Pattern FALSE_ACTIONS_PATTERN = Pattern.compile(
        "false:\\s*([^}]*?)\\}"
    );
    
    private static final Pattern ACTION_LINE_PATTERN = Pattern.compile(
        "-\\s*\"([^\"]+)\""
    );
    
    public ConditionalActionHandler(ActionExecutor actionExecutor) {
        this.actionExecutor = actionExecutor;
        this.actionRegistry = ActionRegistry.getInstance();
    }
    
    @Override
    public String getActionType() {
        return "conditional";
    }
    
    @Override
    public ActionResult execute(FormPlayer player, String actionData, ActionContext context) {
        if (actionData == null || actionData.trim().isEmpty()) {
            logger.warn("Conditional action called with empty data for player: " + player.getName());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "No conditional data specified"), player);
        }
        
        try {
            String processedData = processPlaceholders(actionData.trim(), context, player);
            
            
            if (NEW_FORMAT_PATTERN.matcher(processedData).matches()) {
                return executeNewFormat(player, processedData, context);
            } else {
                
                return executeLegacyFormat(player, processedData, context);
            }
            
        } catch (Exception e) {
            logger.error("Error executing conditional action for player " + player.getName() + ": " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Error executing conditional action: " + e.getMessage()), player);
        }
    }
    
    
    private ActionResult executeNewFormat(FormPlayer player, String actionData, ActionContext context) {
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
            
            
            ActionResult lastResult = null;
            for (String action : actionsToExecute) {
                ActionDefinition actionDef = parseActionString(action);
                ActionResult result = actionExecutor.executeAction(player, actionDef, context);
                lastResult = result;
                
                if (!result.isSuccess()) {
                    logger.warn("Action failed during conditional execution: " + action + " - " + result.getMessage());
                    
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
    
    
    private ActionResult executeLegacyFormat(FormPlayer player, String actionData, ActionContext context) {
        String[] parts = actionData.split(":");
        
        if (parts.length < 4) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid conditional format. Minimum: condition_type:condition_value:action_type:action_data"), player);
        }
        
        boolean negate = false;
        int offset = 0;
        
        if ("not".equals(parts[0])) {
            negate = true;
            offset = 1;
            if (parts.length < 5) {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid conditional format with 'not'. Minimum: not:condition_type:condition_value:action_type:action_data"), player);
            }
        }
        
        StringBuilder conditionBuilder = new StringBuilder();
        String conditionType = parts[offset];
        String conditionValue = parts[offset + 1];
        
        switch (conditionType.toLowerCase()) {
            case "permission":
                conditionBuilder.append("permission:").append(conditionValue);
                break;
            case "placeholder":
                if (parts.length < offset + 5) {
                    return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Placeholder condition requires operator and expected value"), player);
                }
                String operator = parts[offset + 2];
                String expectedValue = parts[offset + 3];
                conditionBuilder.append("placeholder:").append(conditionValue)
                               .append(":").append(operator).append(":").append(expectedValue);
                offset += 2;
                break;
            default:
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Unknown condition type: " + conditionType), player);
        }
        
        String condition = conditionBuilder.toString();
        boolean conditionMet = ConditionEvaluator.evaluateCondition(player, condition, context, null);
        
        if (negate) {
            conditionMet = !conditionMet;
        }
        
        if (parts.length < offset + 4) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Missing action type or action data"), player);
        }
        
        String actionType;
        String actionDataToExecute;
        
        if (conditionMet) {
            
            actionType = parts[offset + 2];
            
            
            StringBuilder successActionBuilder = new StringBuilder();
            int successActionStart = offset + 3;
            
            
            int failureActionStart = findNextActionStart(parts, successActionStart + 1);
            
            if (failureActionStart != -1) {
                
                for (int i = successActionStart; i < failureActionStart - 1; i++) {
                    if (successActionBuilder.length() > 0) {
                        successActionBuilder.append(":");
                    }
                    successActionBuilder.append(parts[i]);
                }
            } else {
                
                for (int i = successActionStart; i < parts.length; i++) {
                    if (successActionBuilder.length() > 0) {
                        successActionBuilder.append(":");
                    }
                    successActionBuilder.append(parts[i]);
                }
            }
            actionDataToExecute = successActionBuilder.toString();
            
            logger.info("Condition met for player " + player.getName() + ", executing success action: " + actionType + ":" + actionDataToExecute);
        } else {
            
            int successActionStart = offset + 3;
            int failureActionStart = findNextActionStart(parts, successActionStart + 1);
            
            if (failureActionStart == -1) {
                logger.info("Condition not met for player " + player.getName() + ": " + condition + " (no failure action specified)");
                return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Condition not met, action skipped"), player);
            }
            
            actionType = parts[failureActionStart - 1];
            
            
            StringBuilder failureActionBuilder = new StringBuilder();
            for (int i = failureActionStart; i < parts.length; i++) {
                if (failureActionBuilder.length() > 0) {
                    failureActionBuilder.append(":");
                }
                failureActionBuilder.append(parts[i]);
            }
            actionDataToExecute = failureActionBuilder.toString();
            
            logger.info("Condition not met for player " + player.getName() + ", executing failure action: " + actionType + ":" + actionDataToExecute);
        }
        
        
        ActionDefinition actionToExecute = new ActionDefinition();
        actionToExecute.addAction(actionType, actionDataToExecute);
        ActionResult result = actionExecutor.executeAction(player, actionToExecute, context);
        
        if (result.isSuccess()) {
            return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Conditional action executed: " + result.getMessage()), player);
        } else {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Conditional action failed: " + result.getMessage()), player);
        }
    }
    
    
    private boolean evaluateComplexCondition(FormPlayer player, String condition, ActionContext context) {
        
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
    
    
    private boolean evaluateSingleCondition(FormPlayer player, String condition, ActionContext context) {
        
        if (condition.startsWith("placeholder:")) {
            return evaluatePlaceholderCondition(player, condition, context);
        } else if (condition.startsWith("permission:")) {
            return evaluatePermissionCondition(player, condition, context);
        } else {
            logger.warn("Unknown condition type in: " + condition);
            return false;
        }
    }
    
    
    private boolean evaluatePlaceholderCondition(FormPlayer player, String condition, ActionContext context) {
        
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
    
    
    private boolean evaluatePermissionCondition(FormPlayer player, String condition, ActionContext context) {
        String permission = condition.substring("permission:".length()).trim();
        String conditionString = "permission:" + permission;
        return ConditionEvaluator.evaluateCondition(player, conditionString, context, null);
    }
    
    
    private List<String> parseActionList(String actionData, Pattern pattern) {
        List<String> actions = new ArrayList<>();
        Matcher matcher = pattern.matcher(actionData);
        
        if (matcher.find()) {
            String actionsBlock = matcher.group(1);
            Matcher actionMatcher = ACTION_LINE_PATTERN.matcher(actionsBlock);
            
            while (actionMatcher.find()) {
                String action = actionMatcher.group(1).trim();
                if (!action.isEmpty()) {
                    actions.add(action);
                }
            }
        }
        
        return actions;
    }
    
    
    private ActionDefinition parseActionString(String actionString) {
        ActionDefinition actionDef = new ActionDefinition();
        
        
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
    
    
    private int findNextActionStart(String[] parts, int startIndex) {
        Set<String> knownActionTypes = actionRegistry.getRegisteredActionTypes();
        
        for (int i = startIndex; i < parts.length; i++) {
            if (knownActionTypes.contains(parts[i].toLowerCase())) {
                return i + 1; 
            }
        }
        return -1; 
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
        
        String conditionType = parts[offset];
        
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
        return new String[]{
            "conditional:permission:some.permission:message:You have permission!",
            "conditional:placeholder:{balance}:>=:1000:message:You have enough money!",
            "conditional:not:permission:vip.access:message:You need VIP access!",
            "conditional:placeholder:{player}:equals:Admin:server:gamemode creative {player}",
            "conditional:placeholder:{world}:equals:world:message:You're in overworld:message:You're not in overworld",
            "conditional:permission:vip.access:message:VIP welcome!:message:You need VIP access!"
        };
    }
}
