package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.actions.ActionExecutor;
import it.pintux.life.common.utils.ConditionEvaluator;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.PlaceholderUtil;
import it.pintux.life.common.utils.MessageData;

import java.util.Map;

/**
 * Handles conditional action execution based on various conditions.
 * 
 * Usage: conditional:permission:some.permission:command:give {player} diamond
 * Usage: conditional:placeholder:{balance}:>=:1000:message:You have enough money!
 * Usage: conditional:placeholder:{player}:equals:Admin:server:gamemode creative {player}
 * Usage: conditional:not:permission:vip.access:message:You need VIP access!
 * Usage: conditional:placeholder:{world}:equals:world:message:You're in overworld:message:You're not in overworld
 * 
 * Format: conditional:[not:]condition_type:condition_value[:operator:expected_value]:success_action_type:success_action_data[:failure_action_type:failure_action_data]
 */
public class ConditionalActionHandler extends BaseActionHandler {

    private final ActionExecutor actionExecutor;
    
    public ConditionalActionHandler(ActionExecutor actionExecutor) {
        this.actionExecutor = actionExecutor;
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
            String[] parts = processedData.split(":");
            
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
                // Execute success action
                actionType = parts[offset + 2];
                
                // Build success action data - handle case where action data contains colons
                StringBuilder successActionBuilder = new StringBuilder();
                int successActionStart = offset + 3;
                
                // Find where failure action starts (if any)
                int failureActionStart = -1;
                if (parts.length > offset + 4) {
                    // Look for failure action pattern - we need at least 2 more parts for action_type:action_data
                    failureActionStart = offset + 4;
                    // If there are more parts, the failure action might start later
                    // For now, assume failure action starts right after success action
                }
                
                if (failureActionStart != -1 && parts.length > failureActionStart + 1) {
                    // There's a failure action, so success action data is just one part
                    actionDataToExecute = parts[successActionStart];
                } else {
                    // No failure action, so success action data can span multiple parts
                    for (int i = successActionStart; i < parts.length; i++) {
                        if (successActionBuilder.length() > 0) {
                            successActionBuilder.append(":");
                        }
                        successActionBuilder.append(parts[i]);
                    }
                    actionDataToExecute = successActionBuilder.toString();
                }
                
                logger.info("Condition met for player " + player.getName() + ", executing success action: " + actionType + ":" + actionDataToExecute);
            } else {
                // Execute failure action if available
                int failureActionTypeIndex = offset + 4;
                int failureActionDataIndex = offset + 5;
                
                if (parts.length < failureActionDataIndex + 1) {
                    logger.info("Condition not met for player " + player.getName() + ": " + condition + " (no failure action specified)");
                    return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Condition not met, action skipped"), player);
                }
                
                actionType = parts[failureActionTypeIndex];
                
                // Build failure action data - handle case where action data contains colons
                StringBuilder failureActionBuilder = new StringBuilder();
                for (int i = failureActionDataIndex; i < parts.length; i++) {
                    if (failureActionBuilder.length() > 0) {
                        failureActionBuilder.append(":");
                    }
                    failureActionBuilder.append(parts[i]);
                }
                actionDataToExecute = failureActionBuilder.toString();
                
                logger.info("Condition not met for player " + player.getName() + ", executing failure action: " + actionType + ":" + actionDataToExecute);
            }
            
            ActionResult result = actionExecutor.executeAction(player, actionType, actionDataToExecute, context);
            
            if (result.isSuccess()) {
                return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Conditional action executed: " + result.getMessage()), player);
            } else {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Conditional action failed: " + result.getMessage()), player);
            }
            
        } catch (Exception e) {
            logger.error("Error executing conditional action for player " + player.getName() + ": " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Error executing conditional action: " + e.getMessage()), player);
        }
    }
    

    
    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }
        
        String[] parts = actionValue.trim().split(":");
        
        if (parts.length < 4) {
            return false;
        }
        
        int offset = 0;
        if ("not".equals(parts[0])) {
            offset = 1;
            if (parts.length < 5) {
                return false;
            }
        }
        
        String conditionType = parts[offset];
        
        switch (conditionType.toLowerCase()) {
            case "permission":
                return parts.length >= offset + 4; // condition_type:condition_value:success_action_type:success_action_data (failure actions optional)
            case "placeholder":
                return parts.length >= offset + 6; // condition_type:condition_value:operator:expected_value:success_action_type:success_action_data (failure actions optional)
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
    
    protected String processPlaceholders(String data, ActionContext context, FormPlayer player) {
        if (context == null) {
            return data;
        }
        
        String result = data;
        Map<String, String> placeholders = context.getPlaceholders();
        
        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                String value = entry.getValue() != null ? entry.getValue() : "";
                result = result.replace(placeholder, value);
            }
        }
        
        Map<String, Object> formResults = context.getFormResults();
        if (formResults != null && !formResults.isEmpty()) {
            for (Map.Entry<String, Object> entry : formResults.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                result = result.replace(placeholder, value);
            }
        }
        
        // Process PlaceholderAPI placeholders if player is provided
        if (player != null) {
            Object messageData = context.getMetadata().get("messageData");
            result = PlaceholderUtil.processPlaceholders(result, player, messageData);
        }
        
        return result;
    }
}