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

/**
 * Base abstract class for action handlers that provides common functionality
 * and reduces code duplication across different action handler implementations.
 */
public abstract class BaseActionHandler implements ActionHandler {
    
    protected final Logger logger;
    
    protected BaseActionHandler() {
        this.logger = Logger.getLogger(this.getClass());
    }
    
    /**
     * Validates basic parameters (player and actionValue) that are common to most actions
     * @param player the player executing the action
     * @param actionValue the action value/parameters
     * @return ActionResult.failure if validation fails, null if validation passes
     */
    protected ActionResult validateBasicParameters(FormPlayer player, String actionValue) {
        if (player == null) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, null, player));
        }
        
        if (ValidationUtils.isNullOrEmpty(actionValue)) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, null, player));
        }
        
        return null; // Validation passed
    }
    
    /**
     * Processes placeholders in the given text using context and player information
     * @param text the text to process
     * @param context the action context containing placeholders and form results
     * @param player the player for placeholder processing
     * @return processed text with placeholders replaced
     */
    protected String processPlaceholders(String text, ActionContext context, FormPlayer player) {
        if (context == null) {
            return text;
        }
        
        String result = PlaceholderUtil.processDynamicPlaceholders(text, context.getPlaceholders());
        result = PlaceholderUtil.processFormResults(result, context.getFormResults());
        
        // Process PlaceholderAPI placeholders if MessageData is available
        if (context.getMetadata() != null && context.getMetadata().containsKey("messageData")) {
            Object messageData = context.getMetadata().get("messageData");
            result = PlaceholderUtil.processPlaceholders(result, player, messageData);
        }
        
        return result;
    }
    
    /**
     * Creates a success result with a localized message
     * @param messageKey the message key from MessageData
     * @param replacements optional replacements for the message
     * @param player the player for message localization
     * @return ActionResult.success with localized message
     */
    protected ActionResult createSuccessResult(String messageKey, Map<String, Object> replacements, FormPlayer player) {
        MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
        String message = messageData.getValueNoPrefix(messageKey, replacements, player);
        return ActionResult.success(message);
    }
    
    /**
     * Creates a failure result with a localized message
     * @param messageKey the message key from MessageData
     * @param replacements optional replacements for the message
     * @param player the player for message localization
     * @return ActionResult.failure with localized message
     */
    protected ActionResult createFailureResult(String messageKey, Map<String, Object> replacements, FormPlayer player) {
        MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
        String message = messageData.getValueNoPrefix(messageKey, replacements, player);
        return ActionResult.failure(message);
    }
    
    /**
     * Creates a failure result with a localized message and exception
     * @param messageKey the message key from MessageData
     * @param replacements optional replacements for the message
     * @param player the player for message localization
     * @param exception the exception that caused the failure
     * @return ActionResult.failure with localized message and exception
     */
    protected ActionResult createFailureResult(String messageKey, Map<String, Object> replacements, FormPlayer player, Throwable exception) {
        MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
        String message = messageData.getValueNoPrefix(messageKey, replacements, player);
        return ActionResult.failure(message, exception);
    }
    
    /**
     * Logs debug information about successful action execution
     * @param actionType the type of action executed
     * @param actionValue the action value that was executed
     * @param player the player who executed the action
     */
    protected void logSuccess(String actionType, String actionValue, FormPlayer player) {
        logger.debug("Successfully executed " + actionType + " action '" + actionValue + "' for player " + player.getName());
    }
    
    /**
     * Logs warning information about failed action execution
     * @param actionType the type of action that failed
     * @param actionValue the action value that failed
     * @param player the player who executed the action
     */
    protected void logFailure(String actionType, String actionValue, FormPlayer player) {
        logger.warn("Failed to execute " + actionType + " action '" + actionValue + "' for player " + player.getName());
    }
    
    /**
     * Logs error information about action execution with exception
     * @param actionType the type of action that caused the error
     * @param actionValue the action value that caused the error
     * @param player the player who executed the action
     * @param exception the exception that occurred
     */
    protected void logError(String actionType, String actionValue, FormPlayer player, Throwable exception) {
        logger.error("Error executing " + actionType + " action '" + actionValue + "' for player " + player.getName(), exception);
    }
    
    /**
     * Creates a map with a single replacement entry
     * @param key the replacement key
     * @param value the replacement value
     * @return Map containing the single replacement
     */
    protected Map<String, Object> createReplacements(String key, Object value) {
        Map<String, Object> replacements = new HashMap<>();
        replacements.put(key, value);
        return replacements;
    }
}