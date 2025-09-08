package it.pintux.life.common.utils;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.platform.PlatformPluginManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;
import it.pintux.life.common.utils.MessageData;

import java.util.Map;

/**
 * Utility class for evaluating conditions used in conditional buttons and actions.
 * Supports permission-based, placeholder-based, and plugin-based conditions.
 */
public class ConditionEvaluator {
    
    private static PlatformPluginManager pluginManager;
    
    private static final Logger logger = Logger.getLogger(ConditionEvaluator.class);
    
    /**
     * Sets the plugin manager for plugin condition evaluation.
     * This should be called during plugin initialization.
     * 
     * @param manager The platform plugin manager
     */
    public static void setPluginManager(PlatformPluginManager manager) {
        pluginManager = manager;
    }
    
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
                    conditionMet = evaluatePlaceholderCondition(conditionValue, operator, expectedValue, context, player, messageData);
                    break;
                case "plugin":
                    conditionMet = evaluatePluginCondition(conditionValue);
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
     * Evaluates a plugin-based condition
     */
    private static boolean evaluatePluginCondition(String pluginName) {
        if (pluginManager == null) {
            logger.warn("Plugin manager not initialized. Plugin conditions will always return false.");
            return false;
        }
        return pluginManager.isPluginEnabled(pluginName);
    }
    
    /**
     * Evaluates a placeholder-based condition
     */
    private static boolean evaluatePlaceholderCondition(String placeholderValue, String operator, String expectedValue, ActionContext context, FormPlayer player, MessageData messageData) {
        try {
            // Process any remaining placeholders in the placeholder value (including PlaceholderAPI)
            String processedPlaceholderValue = PlaceholderUtil.processPlaceholders(placeholderValue, context.getPlaceholders(), player, messageData);
            
            // Also process any remaining placeholders in the expected value (including PlaceholderAPI)
            String processedExpectedValue = PlaceholderUtil.processPlaceholders(expectedValue, context.getPlaceholders(), player, messageData);
            
            switch (operator.toLowerCase()) {
                case "equals":
                case "==":
                    return processedPlaceholderValue.equals(processedExpectedValue);
                case "not_equals":
                case "!=":
                    return !processedPlaceholderValue.equals(processedExpectedValue);
                case "contains":
                    return processedPlaceholderValue.contains(processedExpectedValue);
                case "starts_with":
                    return processedPlaceholderValue.startsWith(processedExpectedValue);
                case "ends_with":
                    return processedPlaceholderValue.endsWith(processedExpectedValue);
                case ">":
                case "greater_than":
                    return compareNumeric(processedPlaceholderValue, processedExpectedValue) > 0;
                case ">=":
                case "greater_equal":
                    return compareNumeric(processedPlaceholderValue, processedExpectedValue) >= 0;
                case "<":
                case "less_than":
                    return compareNumeric(processedPlaceholderValue, processedExpectedValue) < 0;
                case "<=":
                case "less_equal":
                    return compareNumeric(processedPlaceholderValue, processedExpectedValue) <= 0;
                case "regex":
                    return processedPlaceholderValue.matches(processedExpectedValue);
                case "empty":
                    return processedPlaceholderValue == null || processedPlaceholderValue.trim().isEmpty();
                case "not_empty":
                    return processedPlaceholderValue != null && !processedPlaceholderValue.trim().isEmpty();
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
            case "plugin":
                return parts.length >= offset + 2; // type:plugin_name
            default:
                return false;
        }
    }
}