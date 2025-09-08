package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.platform.PlatformTitleManager;
import it.pintux.life.common.utils.FormPlayer;

/**
 * Handles sending action bar messages to players.
 * Example: actionbar:&eProcessing your request...
 */
public class ActionBarActionHandler extends BaseActionHandler {
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
        ActionResult validationResult = validateBasicParameters(player, actionValue);
        if (validationResult != null) {
            return validationResult;
        }
        if (titleManager == null || !titleManager.isSupported()) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Action bar system not available on this platform"), player);
        }
        try {
            String processed = processPlaceholders(actionValue, context, player);
            boolean ok = titleManager.sendActionBar(player, processed);
            if (ok) return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Action bar sent"), player);
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to send action bar"), player);
        } catch (Exception e) {
            logger.error("Error executing actionbar for player " + player.getName(), e);
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Error sending action bar: " + e.getMessage()), player);
        }
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return actionValue != null && !actionValue.trim().isEmpty();
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


}