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
 * Handles sending titles to players.
 * Format examples:
 *  - title:&aHello
 *  - title:&aHello:&7World
 *  - title:&aHello:&7World:10:60:10 (fadeIn:stay:fadeOut in ticks)
 */
public class TitleActionHandler implements ActionHandler {
    private static final Logger logger = Logger.getLogger(TitleActionHandler.class);
    private final PlatformTitleManager titleManager;

    public TitleActionHandler(PlatformTitleManager titleManager) {
        this.titleManager = titleManager;
    }

    @Override
    public String getActionType() {
        return "title";
    }

    @Override
    public ActionResult execute(FormPlayer player, String actionValue, ActionContext context) {
        if (player == null || ValidationUtils.isNullOrEmpty(actionValue)) {
            return ActionResult.failure("Invalid parameters for title action");
        }
        if (titleManager == null || !titleManager.isSupported()) {
            return ActionResult.failure("Title system not available on this platform");
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
                return ActionResult.success("Title sent");
            } else {
                return ActionResult.failure("Failed to send title");
            }
        } catch (Exception e) {
            logger.error("Error executing title action for player " + player.getName(), e);
            return ActionResult.failure("Error sending title: " + e.getMessage());
        }
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return !ValidationUtils.isNullOrEmpty(actionValue);
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