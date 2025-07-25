package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionHandler;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.actions.ActionExecutor;
import it.pintux.life.common.utils.ConditionEvaluator;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;
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
            String processedData = processPlaceholders(actionData.trim(), context, player);
            String[] parts = processedData.split(":", 6);
            
            if (parts.length < 4) {
                return ActionResult.failure("Invalid conditional format. Minimum: condition_type:condition_value:action_type:action_data");
            }
            
            boolean negate = false;
            int offset = 0;
            
            if ("not".equals(parts[0])) {
                negate = true;
                offset = 1;
                if (parts.length < 5) {
                    return ActionResult.failure("Invalid conditional format with 'not'. Minimum: not:condition_type:condition_value:action_type:action_data");
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
                        return ActionResult.failure("Placeholder condition requires operator and expected value");
                    }
                    String operator = parts[offset + 2];
                    String expectedValue = parts[offset + 3];
                    conditionBuilder.append("placeholder:").append(conditionValue)
                                   .append(":").append(operator).append(":").append(expectedValue);
                    offset += 2;
                    break;
                default:
                    return ActionResult.failure("Unknown condition type: " + conditionType);
            }
            
            String condition = conditionBuilder.toString();
            boolean conditionMet = ConditionEvaluator.evaluateCondition(player, condition, context, null);
            
            if (negate) {
                conditionMet = !conditionMet;
            }
            
            if (parts.length < offset + 4) {
                return ActionResult.failure("Missing action type or action data");
            }
            
            String actionType;
            String actionDataToExecute;
            
            if (conditionMet) {
                // Execute success action
                actionType = parts[offset + 2];
                actionDataToExecute = parts[offset + 3];
                
                // Check if there are failure actions after success actions
                int successActionEnd = offset + 4;
                if (parts.length > successActionEnd + 1) {
                    // There might be failure actions, so we need to find where success action ends
                    // For now, assume success action is just one action_type:action_data pair
                }
                
                logger.info("Condition met for player " + player.getName() + ", executing success action: " + actionType + ":" + actionDataToExecute);
            } else {
                // Execute failure action if available
                if (parts.length < offset + 6) {
                    logger.info("Condition not met for player " + player.getName() + ": " + condition + " (no failure action specified)");
                    return ActionResult.success("Condition not met, action skipped");
                }
                
                actionType = parts[offset + 4];
                actionDataToExecute = parts[offset + 5];
                
                // Handle additional parts for failure action
                if (parts.length > offset + 6) {
                    StringBuilder sb = new StringBuilder(actionDataToExecute);
                    for (int i = offset + 6; i < parts.length; i++) {
                        sb.append(":").append(parts[i]);
                    }
                    actionDataToExecute = sb.toString();
                }
                
                logger.info("Condition not met for player " + player.getName() + ", executing failure action: " + actionType + ":" + actionDataToExecute);
            }
            
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
    
    private String processPlaceholders(String data, ActionContext context, FormPlayer player) {
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