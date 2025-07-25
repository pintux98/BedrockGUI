package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionHandler;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.PlaceholderUtil;
import java.util.HashMap;
import java.util.Map;
import it.pintux.life.common.utils.ValidationUtils;
import org.geysermc.cumulus.form.ModalForm;


import java.util.Map;

/**
 * Handles sending messages to players
 */
public class MessageActionHandler implements ActionHandler {
    
    private static final Logger logger = Logger.getLogger(MessageActionHandler.class);
    
    @Override
    public String getActionType() {
        return "message";
    }
    
    @Override
    public ActionResult execute(FormPlayer player, String actionValue, ActionContext context) {
        if (player == null) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, null, player));
        }
        
        if (ValidationUtils.isNullOrEmpty(actionValue)) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, null, player));
        }
        
        try {
            String processedMessage = processPlaceholders(actionValue, context, player);
            
            if (ValidationUtils.isNullOrEmpty(processedMessage.trim())) {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, null, player));
            }
            
            // Send the message to the player
            player.sendMessage(processedMessage);
            
            logger.debug("Successfully sent message to player " + player.getName() + ": " + processedMessage);
            // Get MessageData from context if available
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            if (messageData != null) {
                return ActionResult.success(messageData.getValueNoPrefix(MessageData.ACTION_MESSAGE_SENT, null, player));
            } else {
                return ActionResult.success("Message sent successfully");
            }
            
        } catch (Exception e) {
            logger.error("Error sending message to player " + player.getName(), e);
            // Get MessageData from context if available
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            if (messageData != null) {
                Map<String, Object> replacements = new HashMap<>();
                replacements.put("error", e.getMessage());
                return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_MESSAGE_FAILED, replacements, player), e);
            } else {
                return ActionResult.failure("Failed to send message: " + e.getMessage(), e);
            }
        }
    }
    
    @Override
    public boolean isValidAction(String actionValue) {
        if (ValidationUtils.isNullOrEmpty(actionValue)) {
            return false;
        }
        
        // Basic validation - message should not be too long
        String trimmed = actionValue.trim();
        return trimmed.length() > 0 && trimmed.length() <= 1000; // Reasonable message length limit
    }
    
    @Override
    public String getDescription() {
        return "Sends a chat message to the player. Supports placeholders for dynamic content.";
    }
    
    @Override
    public String[] getUsageExamples() {
        return new String[]{
            "Welcome to the server, {player}!",
            "You have selected: {selected_option}",
            "Your balance is: ${balance}",
            "Thank you for your purchase!"
        };
    }
    
    /**
     * Processes placeholders in the message
     * @param message the message with placeholders
     * @param context the action context containing placeholder values
     * @param player the player for PlaceholderAPI processing
     * @return the processed message
     */
    private String processPlaceholders(String message, ActionContext context, FormPlayer player) {
        if (context == null) {
            return message;
        }
        
        // Process dynamic placeholders first
        String result = PlaceholderUtil.processDynamicPlaceholders(message, context.getPlaceholders());
        result = PlaceholderUtil.processFormResults(result, context.getFormResults());
        
        // Then process PlaceholderAPI placeholders if available
        if (context.getMetadata() != null && context.getMetadata().containsKey("messageData")) {
            Object messageDataObj = context.getMetadata().get("messageData");
            if (messageDataObj instanceof it.pintux.life.common.utils.MessageData) {
                it.pintux.life.common.utils.MessageData messageData = (it.pintux.life.common.utils.MessageData) messageDataObj;
                result = PlaceholderUtil.processPlaceholders(result, null, player, messageData);
            }
        }
        
        return result;
    }
}