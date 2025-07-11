package it.pintux.life.common.utils;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;
import it.pintux.life.common.utils.MessageData;

import java.util.Map;

/**
 * Utility class for evaluating conditions used in conditional buttons and actions.
 * Supports permission-based and placeholder-based conditions.
 */
public class ConditionEvaluator {
    
    private static final Logger logger = Logger.getLogger(ConditionEvaluator.class);
    
    /**
     * Evaluates a condition string for a given player and context.
     * 
     * @param player The player to evaluate the condition for
     * @param condition The condition string (e.g., "permission:some.permission" or "placeholder:$balance:>=:1000")
     * @param context The action context containing placeholders and form results
     * @return true if the condition is met, false otherwise
     */
    public static boolean evaluateCondition(FormPlayer player, String condition, ActionContext context, MessageData messageData) {
        if (condition == null || condition.trim().isEmpty()) {
            return true; // No condition means always show
        }
        
        try {
            // Process placeholders in the condition
            String processedCondition = PlaceholderUtil.processPlaceholders(condition.trim(), context.getPlaceholders(), player, messageData);
            String[] parts = processedCondition.split(":");
            
            if (parts.length < 2) {
                logger.warn("Invalid condition format: " + condition);
                return false;
            }
            
            boolean negate = false;
            int offset = 0;
            
            // Check for negation
            if ("not".equals(parts[0])) {
                negate = true;
                offset = 1;
                if (parts.length < 3) {
                    logger.warn("Invalid condition format with 'not': " + condition);
                    return false;
                }
            }
            
            String conditionType = parts[offset];
            String conditionValue = parts[offset + 1];
            
            boolean conditionMet = false;
            
            switch (conditionType.toLowerCase()) {
                case "permission":
                    conditionMet = evaluatePermissionCondition(player, conditionValue);
                    break;
                case "placeholder":
                    if (parts.length < offset + 4) {
                        logger.warn("Placeholder condition requires operator and expected value: " + condition);
                        return false;
                    }
                    String operator = parts[offset + 2];
                    String expectedValue = parts[offset + 3];
                    conditionMet = evaluatePlaceholderCondition(conditionValue, operator, expectedValue, context);
                    break;
                default:
                    logger.warn("Unknown condition type: " + conditionType);
                    return false;
            }
            
            // Apply negation if specified
            if (negate) {
                conditionMet = !conditionMet;
            }
            
            return conditionMet;
            
        } catch (Exception e) {
            logger.error("Error evaluating condition '" + condition + "' for player " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Evaluates a permission-based condition
     */
    private static boolean evaluatePermissionCondition(FormPlayer player, String permission) {
        return player.hasPermission(permission);
    }
    
    /**
     * Evaluates a placeholder-based condition
     */
    private static boolean evaluatePlaceholderCondition(String placeholderValue, String operator, String expectedValue, ActionContext context) {
        try {
            switch (operator.toLowerCase()) {
                case "equals":
                case "==":
                    return placeholderValue.equals(expectedValue);
                case "not_equals":
                case "!=":
                    return !placeholderValue.equals(expectedValue);
                case "contains":
                    return placeholderValue.contains(expectedValue);
                case "starts_with":
                    return placeholderValue.startsWith(expectedValue);
                case "ends_with":
                    return placeholderValue.endsWith(expectedValue);
                case ">":
                case "greater_than":
                    return compareNumeric(placeholderValue, expectedValue) > 0;
                case ">=":
                case "greater_equal":
                    return compareNumeric(placeholderValue, expectedValue) >= 0;
                case "<":
                case "less_than":
                    return compareNumeric(placeholderValue, expectedValue) < 0;
                case "<=":
                case "less_equal":
                    return compareNumeric(placeholderValue, expectedValue) <= 0;
                case "regex":
                    return placeholderValue.matches(expectedValue);
                case "empty":
                    return placeholderValue == null || placeholderValue.trim().isEmpty();
                case "not_empty":
                    return placeholderValue != null && !placeholderValue.trim().isEmpty();
                default:
                    logger.warn("Unknown operator in placeholder condition: " + operator);
                    return false;
            }
        } catch (Exception e) {
            logger.warn("Error evaluating placeholder condition: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Compares two numeric values
     */
    private static int compareNumeric(String value1, String value2) throws NumberFormatException {
        double num1 = Double.parseDouble(value1);
        double num2 = Double.parseDouble(value2);
        return Double.compare(num1, num2);
    }
    
    /**
     * Validates if a condition string has the correct format
     */
    public static boolean isValidCondition(String condition) {
        if (condition == null || condition.trim().isEmpty()) {
            return true; // Empty condition is valid (always true)
        }
        
        String[] parts = condition.trim().split(":");
        
        if (parts.length < 2) {
            return false;
        }
        
        int offset = 0;
        if ("not".equals(parts[0])) {
            offset = 1;
            if (parts.length < 3) {
                return false;
            }
        }
        
        String conditionType = parts[offset];
        
        switch (conditionType.toLowerCase()) {
            case "permission":
                return parts.length >= offset + 2; // type:permission
            case "placeholder":
                return parts.length >= offset + 4; // type:placeholder:operator:value
            default:
                return false;
        }
    }
}