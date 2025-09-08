package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.platform.PlatformPlayerManager;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.ValidationUtils;

/**
 * Handles sending a clickable URL to the player chat.
 * Note: Most Minecraft clients render http(s) links in chat as clickable.
 * Usage: url:https://example.com
 */
public class OpenUrlActionHandler extends BaseActionHandler {

    private final PlatformPlayerManager playerManager;
    
    public OpenUrlActionHandler(PlatformPlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    @Override
    public String getActionType() {
        return "url";
    }

    private boolean validateParameters(FormPlayer player, String actionValue) {
        return player != null && actionValue != null && !actionValue.trim().isEmpty();
    }

    @Override
    public ActionResult execute(FormPlayer player, String actionValue, ActionContext context) {
        if (!validateParameters(player, actionValue)) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, null, player)), player);
        }

        try {
            String processed = processPlaceholders(actionValue.trim(), context, player);

            // Basic sanitization
            processed = ValidationUtils.sanitizeString(processed);

            if (processed == null || processed.trim().isEmpty()) {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, null, player)), player);
            }

            // Validate that it looks like a URL. Be permissive here as placeholders may produce valid links.
            if (!(processed.startsWith("http://") || processed.startsWith("https://"))) {
                logger.warn("URL action value does not start with http/https after placeholder processing: " + processed);
            }

            // Send the URL to chat. Most clients will show it as a clickable link with confirmation.
            playerManager.sendMessage(player, processed);

            logger.debug("Sent URL to player " + player.getName() + ": " + processed);
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return createSuccessResult("ACTION_SUCCESS", createReplacements("message", messageData != null
                    ? messageData.getValueNoPrefix(MessageData.ACTION_MESSAGE_SENT, null, player)
                    : "URL sent"), player);
        } catch (Exception e) {
            logger.error("Error executing URL action for player " + (player != null ? player.getName() : "null") + ": " + e.getMessage(), e);
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to send URL: " + e.getMessage()), player);
        }
    }

    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
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


}