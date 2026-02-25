package it.pintux.life.common.utils;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.actions.ActionSystem.ActionContext;
import it.pintux.life.common.platform.PlatformPluginManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;
import it.pintux.life.common.utils.MessageData;

import java.util.Map;


public class ConditionEvaluator {

    private static PlatformPluginManager pluginManager;

    private static final Logger logger = Logger.getLogger(ConditionEvaluator.class);


    public static void setPluginManager(PlatformPluginManager manager) {
        pluginManager = manager;
    }


    public static boolean evaluateCondition(FormPlayer player, String condition, ActionSystem.ActionContext context, MessageData messageData) {
        if (condition == null || condition.trim().isEmpty()) {
            return true;
        }

        try {

            String processedCondition = PlaceholderUtil.processPlaceholders(condition.trim(), context.getPlaceholders(), player, messageData);
            String[] parts = processedCondition.split(":");

            if (parts.length < 2) {
                logger.warn("Invalid condition format: " + condition);
                return false;
            }

            boolean negate = false;
            int offset = 0;


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
                case "bedrock_player":
                    conditionMet = evaluateBedrockPlayerCondition(player);
                    break;
                case "java_player":
                    conditionMet = evaluateJavaPlayerCondition(player);
                    break;
                default:
                    logger.warn("Unknown condition type: " + conditionType);
                    return false;
            }


            if (negate) {
                conditionMet = !conditionMet;
            }

            return conditionMet;

        } catch (Exception e) {
            logger.error("Error evaluating condition '" + condition + "' for player " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }


    private static boolean evaluatePermissionCondition(FormPlayer player, String permission) {
        return player.hasPermission(permission);
    }


    private static boolean evaluatePluginCondition(String pluginName) {
        if (pluginManager == null) {
            logger.warn("Plugin manager not initialized. Plugin conditions will always return false.");
            return false;
        }
        return pluginManager.isPluginEnabled(pluginName);
    }


    private static boolean evaluateBedrockPlayerCondition(FormPlayer player) {
        try {

            return false;
        } catch (Exception e) {
            logger.warn("Error checking if player is Bedrock player: " + e.getMessage());
            return false;
        }
    }


    private static boolean evaluateJavaPlayerCondition(FormPlayer player) {
        try {

            return true;
        } catch (Exception e) {
            logger.warn("Error checking if player is Java player: " + e.getMessage());
            return false;
        }
    }


    private static boolean evaluatePlaceholderCondition(String placeholderValue, String operator, String expectedValue, ActionSystem.ActionContext context, FormPlayer player, MessageData messageData) {
        try {

            String processedPlaceholderValue = PlaceholderUtil.processPlaceholders(placeholderValue, context.getPlaceholders(), player, messageData);


            String processedExpectedValue = PlaceholderUtil.processPlaceholders(expectedValue, context.getPlaceholders(), player, messageData);

            return switch (operator.toLowerCase()) {
                case "equals", "==" -> processedPlaceholderValue.equals(processedExpectedValue);
                case "not_equals", "!=" -> !processedPlaceholderValue.equals(processedExpectedValue);
                case "contains" -> processedPlaceholderValue.contains(processedExpectedValue);
                case "starts_with" -> processedPlaceholderValue.startsWith(processedExpectedValue);
                case "ends_with" -> processedPlaceholderValue.endsWith(processedExpectedValue);
                case ">", "greater_than" -> compareNumeric(processedPlaceholderValue, processedExpectedValue) > 0;
                case ">=", "greater_equal" -> compareNumeric(processedPlaceholderValue, processedExpectedValue) >= 0;
                case "<", "less_than" -> compareNumeric(processedPlaceholderValue, processedExpectedValue) < 0;
                case "<=", "less_equal" -> compareNumeric(processedPlaceholderValue, processedExpectedValue) <= 0;
                case "regex" -> processedPlaceholderValue.matches(processedExpectedValue);
                case "empty" -> processedPlaceholderValue == null || processedPlaceholderValue.trim().isEmpty();
                case "not_empty" -> processedPlaceholderValue != null && !processedPlaceholderValue.trim().isEmpty();
                default -> {
                    logger.warn("Unknown operator in placeholder condition: " + operator);
                    yield false;
                }
            };
        } catch (Exception e) {
            logger.warn("Error evaluating placeholder condition: " + e.getMessage());
            return false;
        }
    }

    private static int compareNumeric(String value1, String value2) throws NumberFormatException {
        double num1 = Double.parseDouble(value1);
        double num2 = Double.parseDouble(value2);
        return Double.compare(num1, num2);
    }
}

