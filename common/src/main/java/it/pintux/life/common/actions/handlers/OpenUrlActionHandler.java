package it.pintux.life.common.actions.handlers;

import java.util.List;

import it.pintux.life.common.actions.ActionSystem;



import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.platform.PlatformPlayerManager;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.ValidationUtils;


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
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        if (!validateParameters(player, actionValue)) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, null, player)), player);
        }

        try {
            String processed;
            
            // Check if it's the new YAML format with curly braces
            if (actionValue.trim().startsWith("{") && actionValue.trim().endsWith("}")) {
                List<String> urls = parseNewFormatValues(actionValue);
                if (urls.isEmpty()) {
                    MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                    return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "No valid URLs found"), player);
                }
                // For URL action, we only use the first URL if multiple are provided
                processed = processPlaceholders(urls.get(0), context, player);
            } else {
                // Legacy format support
                processed = processPlaceholders(actionValue.trim(), context, player);
            }

            processed = ValidationUtils.sanitizeString(processed);

            if (processed == null || processed.trim().isEmpty()) {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, null, player)), player);
            }

            // Validate URL format
            if (!(processed.startsWith("http://") || processed.startsWith("https://"))) {
                logger.warn("URL action value does not start with http/https after placeholder processing: " + processed);
            }

            // Send URL to player
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
        
        // Support new YAML format
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                List<String> urls = parseNewFormatValues(trimmed);
                for (String url : urls) {
                    if (!isValidUrl(url)) {
                        return false;
                    }
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        
        // Legacy format support
        if (trimmed.length() > 2048) {
            return false;
        }

        return trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.contains("{") || trimmed.contains("}");
    }
    
    private boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        String trimmed = url.trim();
        if (trimmed.length() > 2048) {
            return false;
        }
        return trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.contains("{") || trimmed.contains("}");
    }

    @Override
    public String getDescription() {
        return "Sends a URL to the player's chat. Clients typically render HTTP(S) links as clickable.";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{
                "New Format Examples:",
                "url { - \"https://example.com\" }",
                "url { - \"https://docs.server.com/help\" }",
                "url { - \"https://store.server.com/{player}\" }",
                "url { - \"https://api.server.com/stats?player={player_name}\" }",
                "Legacy Format Examples:",
                "url:https://example.com",
                "url:https://docs.server.com/help",
                "url:https://store.server.com/{player}"
        };
    }


}

