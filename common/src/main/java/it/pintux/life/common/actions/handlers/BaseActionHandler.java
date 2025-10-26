package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionHandler;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.PlaceholderUtil;
import it.pintux.life.common.utils.ValidationUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;


public abstract class BaseActionHandler implements ActionHandler {

    protected final Logger logger;

    protected BaseActionHandler() {
        this.logger = Logger.getLogger(this.getClass());
    }


    protected ActionResult validateBasicParameters(FormPlayer player, String actionValue) {
        if (player == null) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, null, player));
        }

        if (ValidationUtils.isNullOrEmpty(actionValue)) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, null, player));
        }

        return null;
    }


    protected ActionResult createSuccessResult(String messageKey, Map<String, Object> replacements, FormPlayer player) {
        MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
        String message = messageData.getValueNoPrefix(messageKey, replacements, player);
        return ActionResult.success(message);
    }


    protected ActionResult createFailureResult(String messageKey, Map<String, Object> replacements, FormPlayer player) {
        MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
        String message = messageData.getValueNoPrefix(messageKey, replacements, player);
        return ActionResult.failure(message);
    }


    protected ActionResult createFailureResult(String messageKey, Map<String, Object> replacements, FormPlayer player, Throwable exception) {
        MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
        String message = messageData.getValueNoPrefix(messageKey, replacements, player);
        return ActionResult.failure(message, exception);
    }


    protected void logSuccess(String actionType, String actionValue, FormPlayer player) {
        logger.debug("Successfully executed " + actionType + " action '" + actionValue + "' for player " + player.getName());
    }


    protected void logFailure(String actionType, String actionValue, FormPlayer player) {
        logger.warn("Failed to execute " + actionType + " action '" + actionValue + "' for player " + player.getName());
    }


    protected void logError(String actionType, String actionValue, FormPlayer player, Throwable exception) {
        logger.error("Error executing " + actionType + " action '" + actionValue + "' for player " + player.getName(), exception);
    }


    protected Map<String, Object> createReplacements(String key, Object value) {
        Map<String, Object> replacements = new HashMap<>();
        replacements.put(key, value);
        return replacements;
    }


    protected String processPlaceholders(String text, ActionContext context, FormPlayer player) {
        if (ValidationUtils.isNullOrEmpty(text)) {
            return text;
        }
        return PlaceholderUtil.processPlaceholdersWithContext(text, context, player);
    }


    protected String processPlaceholders(String text, FormPlayer player) {
        if (ValidationUtils.isNullOrEmpty(text)) {
            return text;
        }
        return PlaceholderUtil.processPlaceholders(text, player, null);
    }


    protected String processPlaceholders(String text, Map<String, String> replacements, FormPlayer player, MessageData messageData) {
        if (ValidationUtils.isNullOrEmpty(text)) {
            return text;
        }
        return PlaceholderUtil.processPlaceholders(text, replacements, player, messageData);
    }


    protected boolean containsDynamicPlaceholders(String text) {
        return PlaceholderUtil.containsDynamicPlaceholders(text);
    }


    protected boolean validateServiceAvailability(java.util.function.Supplier<Boolean> serviceCheck, String serviceName, FormPlayer player) {
        try {
            return serviceCheck.get();
        } catch (Exception e) {

            return false;
        }
    }


    protected boolean isValidCommand(String command) {
        if (ValidationUtils.isNullOrEmpty(command)) {
            return false;
        }

        String trimmed = command.trim();
        if (trimmed.isEmpty() || trimmed.equals("/")) {
            return false;
        }


        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }


        return !trimmed.contains("\n") && !trimmed.contains("\r") && !trimmed.trim().isEmpty();
    }


    protected String normalizeCommand(String command) {
        if (ValidationUtils.isNullOrEmpty(command)) {
            return "";
        }

        String normalized = command.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        return normalized.trim();
    }


    protected boolean executeWithErrorHandling(Supplier<Boolean> operation, String operationDescription, FormPlayer player) {
        try {
            logger.debug("Executing operation: " + operationDescription + " for player: " + player.getName());
            boolean result = operation.get();

            if (result) {
                logger.debug("Operation completed successfully: " + operationDescription);
            } else {
                logger.warn("Operation failed: " + operationDescription + " for player: " + player.getName());
            }

            return result;
        } catch (Exception e) {
            logger.error("Error during operation: " + operationDescription + " for player: " + player.getName(), e);
            return false;
        }
    }


    protected ActionResult validateActionParameters(FormPlayer player, String actionValue, Supplier<Boolean> additionalValidation) {

        ActionResult basicValidation = validateBasicParameters(player, actionValue);
        if (basicValidation != null) {
            return basicValidation;
        }


        if (additionalValidation != null) {
            try {
                if (!additionalValidation.get()) {
                    MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                    return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, null, player));
                }
            } catch (Exception e) {
                logger.error("Error during additional validation for player: " + player.getName(), e);
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Validation error: " + e.getMessage()), player, e);
            }
        }

        return null;
    }


    protected boolean isNewCurlyBraceFormat(String actionData, String actionType) {
        if (actionData == null || actionData.trim().isEmpty()) {
            return false;
        }

        String trimmed = actionData.trim();

        String pattern = "^" + actionType + "\\s*\\{[\\s\\S]*\\}$";
        return trimmed.matches(pattern);
    }


    protected java.util.List<String> parseNewFormatValues(String actionData) {
        java.util.List<String> values = new java.util.ArrayList<>();

        if (actionData == null || actionData.trim().isEmpty()) {
            return values;
        }


        java.util.regex.Pattern valuePattern = java.util.regex.Pattern.compile("-\\s*\"([^\"]+)\"");
        java.util.regex.Matcher matcher = valuePattern.matcher(actionData);

        while (matcher.find()) {
            String value = matcher.group(1);
            if (!value.isEmpty()) {
                values.add(value);
            }
        }

        return values;
    }

    /**
     * Parses action data into a list of strings, supporting both single values and the new curly brace format
     *
     * @param actionData the action data to parse
     * @param context    the action context for placeholder processing
     * @param player     the player for placeholder processing
     * @return list of parsed action strings
     */
    protected java.util.List<String> parseActionData(String actionData, ActionContext context, FormPlayer player) {
        java.util.List<String> result = new java.util.ArrayList<>();

        if (ValidationUtils.isNullOrEmpty(actionData)) {
            return result;
        }

        String processedData = processPlaceholders(actionData, context, player);

        // Check if it's the new curly brace format
        if (processedData.trim().contains("{") && processedData.trim().contains("}")) {
            // Parse new format values
            java.util.List<String> values = parseNewFormatValues(processedData);
            if (!values.isEmpty()) {
                return values;
            }
        }

        // Check if it's an array format [item1, item2, item3] (legacy support)
        if (processedData.trim().startsWith("[") && processedData.trim().endsWith("]")) {
            // Parse JSON-like array format
            String content = processedData.trim().substring(1, processedData.trim().length() - 1);
            if (!content.trim().isEmpty()) {
                String[] items = content.split(",");
                for (String item : items) {
                    String trimmed = item.trim();
                    // Remove quotes if present
                    if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                        trimmed = trimmed.substring(1, trimmed.length() - 1);
                    }
                    if (!trimmed.isEmpty()) {
                        result.add(trimmed);
                    }
                }
            }
        } else {
            // Single value
            result.add(processedData);
        }

        return result;
    }

    /**
     * Parses action data for validation purposes (without placeholder processing)
     *
     * @param actionValue the action value to parse
     * @return list of parsed action strings
     */
    protected java.util.List<String> parseActionDataForValidation(String actionValue) {
        java.util.List<String> result = new java.util.ArrayList<>();

        if (ValidationUtils.isNullOrEmpty(actionValue)) {
            return result;
        }

        String trimmed = actionValue.trim();

        // Check if it's the new curly brace format
        if (trimmed.contains("{") && trimmed.contains("}")) {
            // Parse new format values
            java.util.List<String> values = parseNewFormatValues(trimmed);
            if (!values.isEmpty()) {
                return values;
            }
        }

        // Check if it's an array format [item1, item2, item3] (legacy support)
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            // Parse JSON-like array format
            String content = trimmed.substring(1, trimmed.length() - 1);
            if (!content.trim().isEmpty()) {
                String[] items = content.split(",");
                for (String item : items) {
                    String itemTrimmed = item.trim();
                    // Remove quotes if present
                    if (itemTrimmed.startsWith("\"") && itemTrimmed.endsWith("\"")) {
                        itemTrimmed = itemTrimmed.substring(1, itemTrimmed.length() - 1);
                    }
                    if (!itemTrimmed.isEmpty()) {
                        result.add(itemTrimmed);
                    }
                }
            }
        } else {
            // Single value
            result.add(trimmed);
        }

        return result;
    }
}
