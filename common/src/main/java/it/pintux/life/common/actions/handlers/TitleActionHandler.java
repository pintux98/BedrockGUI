package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.platform.PlatformTitleManager;
import it.pintux.life.common.utils.FormPlayer;

/**
 * Handles sending titles to players.
 * Format examples:
 *  - title:&aHello
 *  - title:&aHello:&7World
 *  - title:&aHello:&7World:10:60:10 (fadeIn:stay:fadeOut in ticks)
 */
public class TitleActionHandler extends BaseActionHandler {
    private final PlatformTitleManager titleManager;

    public TitleActionHandler(PlatformTitleManager titleManager) {
        this.titleManager = titleManager;
    }

    @Override
    public String getActionType() {
        return "title";
    }

    private boolean validateParameters(FormPlayer player, String actionValue) {
        return player != null && actionValue != null && !actionValue.trim().isEmpty();
    }

    @Override
    public ActionResult execute(FormPlayer player, String actionValue, ActionContext context) {
        if (!validateParameters(player, actionValue)) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid parameters for title action"), player);
        }
        if (titleManager == null || !titleManager.isSupported()) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Title system not available on this platform"), player);
        }
        try {
            String processed = processPlaceholders(actionValue, context, player);
            String[] parts = processed.split(":");
            String title = parts.length > 0 ? parts[0] : "";
            String subtitle = parts.length > 1 ? parts[1] : "";
            int fadeIn = parts.length > 2 ? parseIntSafe(parts[2], 10) : 10;
            int stay = parts.length > 3 ? parseIntSafe(parts[3], 60) : 60;
            int fadeOut = parts.length > 4 ? parseIntSafe(parts[4], 10) : 10;

            boolean ok = titleManager.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
            if (ok) {
                return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Title sent"), player);
            } else {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to send title"), player);
            }
        } catch (Exception e) {
            logger.error("Error executing title action for player " + player.getName(), e);
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Error sending title: " + e.getMessage()), player);
        }
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return actionValue != null && !actionValue.trim().isEmpty();
    }

    @Override
    public String getDescription() {
        return "Sends a title (and optional subtitle) to the player with optional timing parameters.";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{
                "title:&aWelcome",
                "title:&aWelcome:&7to the server",
                "title:&aWelcome:&7Player:10:60:10"
        };
    }

    private int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }


}
