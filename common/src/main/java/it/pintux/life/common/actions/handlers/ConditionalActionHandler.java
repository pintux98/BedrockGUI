package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionHandler;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.actions.ActionExecutor;
import it.pintux.life.common.utils.ConditionEvaluator;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;

import java.util.Map;

/**
 * Handles conditional action execution based on various conditions.
 * 
 * Usage: conditional:permission:some.permission:command:give {player} diamond
 * Usage: conditional:placeholder:{balance}:>=:1000:message:You have enough money!
 * Usage: conditional:placeholder:{player}:equals:Admin:server:gamemode creative {player}
 * Usage: conditional:not:permission:vip.access:message:You need VIP access!
 * 
 * Format: conditional:[not:]condition_type:condition_value[:operator:expected_value]:action_type:action_data
 */
public class ConditionalActionHandler implements ActionHandler {
    private static final Logger logger = Logger.getLogger(ConditionalActionHandler.class);
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
            return ActionResult.failure("No conditional data specified");
        }
        
        try {
            // Process placeholders in the action data
            String processedData = processPlaceholders(actionData.trim(), context);
            String[] parts = processedData.split(":", 6);
            
            if (parts.length < 4) {
                return ActionResult.failure("Invalid conditional format. Minimum: condition_type:condition_value:action_type:action_data");
            }
            
            boolean negate = false;
            int offset = 0;
            
            // Check for negation
            if ("not".equals(parts[0])) {
                negate = true;
                offset = 1;
                if (parts.length < 5) {
                    return ActionResult.failure("Invalid conditional format with 'not'. Minimum: not:condition_type:condition_value:action_type:action_data");
                }
            }
            
            // Build condition string for ConditionEvaluator
            StringBuilder conditionBuilder = new StringBuilder();
            String conditionType = parts[offset];
            String conditionValue = parts[offset + 1];
            
            switch (conditionType.toLowerCase()) {
                case "permission":
                    conditionBuilder.append("permission:").append(conditionValue);
                    break;
                case "placeholder":
                    if (parts.length < offset + 5) {
                        return ActionResult.failure("Placeholder condition requires operator and expected value");
                    }
                    String operator = parts[offset + 2];
                    String expectedValue = parts[offset + 3];
                    conditionBuilder.append("placeholder:").append(conditionValue)
                                   .append(":").append(operator).append(":").append(expectedValue);
                    offset += 2; // Adjust offset for operator and expected value
                    break;
                default:
                    return ActionResult.failure("Unknown condition type: " + conditionType);
            }
            
            String condition = conditionBuilder.toString();
            boolean conditionMet = ConditionEvaluator.evaluateCondition(player, condition, context, null);
            
            // Apply negation if specified
            if (negate) {
                conditionMet = !conditionMet;
            }
            
            if (!conditionMet) {
                logger.info("Condition not met for player " + player.getName() + ": " + condition);
                return ActionResult.success("Condition not met, action skipped");
            }
            
            // Extract action type and data
            if (parts.length < offset + 4) {
                return ActionResult.failure("Missing action type or action data");
            }
            
            String actionType = parts[offset + 2];
            String actionDataToExecute = parts[offset + 3];
            
            // If there are more parts, join them as they might contain colons
            if (parts.length > offset + 4) {
                StringBuilder sb = new StringBuilder(actionDataToExecute);
                for (int i = offset + 4; i < parts.length; i++) {
                    sb.append(":").append(parts[i]);
                }
                actionDataToExecute = sb.toString();
            }
            
            logger.info("Condition met for player " + player.getName() + ", executing action: " + actionType + ":" + actionDataToExecute);
            
            // Execute the action
            ActionResult result = actionExecutor.executeAction(player, actionType, actionDataToExecute, context);
            
            if (result.isSuccess()) {
                return ActionResult.success("Conditional action executed: " + result.getMessage());
            } else {
                return ActionResult.failure("Conditional action failed: " + result.getMessage());
            }
            
        } catch (Exception e) {
            logger.error("Error executing conditional action for player " + player.getName() + ": " + e.getMessage());
            return ActionResult.failure("Error executing conditional action: " + e.getMessage());
        }
    }
    

    
    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }
        
        String[] parts = actionValue.trim().split(":");
        
        // Check for minimum parts
        if (parts.length < 4) {
            return false;
        }
        
        // Check for negation
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
                return parts.length >= offset + 4; // condition_type:condition_value:action_type:action_data
            case "placeholder":
                return parts.length >= offset + 6; // condition_type:condition_value:operator:expected_value:action_type:action_data
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
            "conditional:placeholder:{player}:equals:Admin:server:gamemode creative {player}"
        };
    }
    
    private String processPlaceholders(String data, ActionContext context) {
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