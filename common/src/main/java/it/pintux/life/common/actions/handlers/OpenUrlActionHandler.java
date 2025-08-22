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

/**
 * Handles sending a clickable URL to the player chat.
 * Note: Most Minecraft clients render http(s) links in chat as clickable.
 * Usage: url:https://example.com
 */
public class OpenUrlActionHandler implements ActionHandler {

    private static final Logger logger = Logger.getLogger(OpenUrlActionHandler.class);

    @Override
    public String getActionType() {
        return "url";
    }

    @Override
    public ActionResult execute(FormPlayer player, String actionValue, ActionContext context) {
        if (player == null || ValidationUtils.isNullOrEmpty(actionValue)) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, null, player));
        }

        try {
            String processed = processPlaceholders(actionValue.trim(), context, player);

            // Basic sanitization
            processed = ValidationUtils.sanitizeString(processed);

            if (ValidationUtils.isNullOrEmpty(processed)) {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, null, player));
            }

            // Validate that it looks like a URL. Be permissive here as placeholders may produce valid links.
            if (!(processed.startsWith("http://") || processed.startsWith("https://"))) {
                logger.warn("URL action value does not start with http/https after placeholder processing: " + processed);
            }

            // Send the URL to chat. Most clients will show it as a clickable link with confirmation.
            player.sendMessage(processed);

            logger.debug("Sent URL to player " + player.getName() + ": " + processed);
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return ActionResult.success(messageData != null
                    ? messageData.getValueNoPrefix(MessageData.ACTION_MESSAGE_SENT, null, player)
                    : "URL sent");
        } catch (Exception e) {
            logger.error("Error executing URL action for player " + (player != null ? player.getName() : "null") + ": " + e.getMessage(), e);
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return ActionResult.failure(messageData != null
                    ? messageData.getValueNoPrefix(MessageData.ACTION_MESSAGE_FAILED, null, player)
                    : ("Failed to send URL: " + e.getMessage()), e);
        }
    }

    @Override
    public boolean isValidAction(String actionValue) {
        if (ValidationUtils.isNullOrEmpty(actionValue)) {
            return false;
        }
        String trimmed = actionValue.trim();
        if (trimmed.length() > 2048) {
            return false;
        }
        // Allow placeholders in validation phase. Accept if it already looks like a URL or contains placeholders.
        return trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.contains("{") || trimmed.contains("}");
    }

    @Override
    public String getDescription() {
        return "Sends a URL to the player's chat. Clients typically render HTTP(S) links as clickable.";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[] {
                "url:https://example.com",
                "url:https://docs.server.com/help",
                "url:https://store.server.com/{player}"
        };
    }

    private String processPlaceholders(String text, ActionContext context, FormPlayer player) {
        if (context == null) {
            return text;
        }
        String result = PlaceholderUtil.processDynamicPlaceholders(text, context.getPlaceholders());
        result = PlaceholderUtil.processFormResults(result, context.getFormResults());

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