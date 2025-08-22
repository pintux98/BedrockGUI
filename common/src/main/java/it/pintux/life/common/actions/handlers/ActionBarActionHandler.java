package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionHandler;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.platform.PlatformTitleManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;
import it.pintux.life.common.utils.PlaceholderUtil;
import it.pintux.life.common.utils.ValidationUtils;

/**
 * Handles sending action bar messages to players.
 * Example: actionbar:&eProcessing your request...
 */
public class ActionBarActionHandler implements ActionHandler {
    private static final Logger logger = Logger.getLogger(ActionBarActionHandler.class);
    private final PlatformTitleManager titleManager;

    public ActionBarActionHandler(PlatformTitleManager titleManager) {
        this.titleManager = titleManager;
    }

    @Override
    public String getActionType() {
        return "actionbar";
    }

    @Override
    public ActionResult execute(FormPlayer player, String actionValue, ActionContext context) {
        if (player == null || ValidationUtils.isNullOrEmpty(actionValue)) {
            return ActionResult.failure("Invalid parameters for actionbar action");
        }
        if (titleManager == null || !titleManager.isSupported()) {
            return ActionResult.failure("Action bar system not available on this platform");
        }
        try {
            String processed = processPlaceholders(actionValue, context, player);
            boolean ok = titleManager.sendActionBar(player, processed);
            if (ok) return ActionResult.success("Action bar sent");
            return ActionResult.failure("Failed to send action bar");
        } catch (Exception e) {
            logger.error("Error executing actionbar for player " + player.getName(), e);
            return ActionResult.failure("Error sending action bar: " + e.getMessage());
        }
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return !ValidationUtils.isNullOrEmpty(actionValue);
    }

    @Override
    public String getDescription() {
        return "Sends an action bar message to the player.";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{
                "actionbar:&eProcessing...",
                "actionbar:&aSuccess!"
        };
    }

    private String processPlaceholders(String message, ActionContext context, FormPlayer player) {
        if (context == null) return message;
        String result = PlaceholderUtil.processDynamicPlaceholders(message, context.getPlaceholders());
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