package it.pintux.life.bedwarsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;

/**
 * No-op handler for "close" buttons. Tapping any form button already dismisses the form; this exists
 * only so the close action string resolves to a registered handler instead of logging
 * "Invalid action type ... not registered".
 */
public final class NoOpAction implements ActionSystem.ActionHandler {
    private final String type;

    public NoOpAction(String type) {
        this.type = type;
    }

    @Override public String getActionType() { return type; }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        return ActionSystem.ActionResult.success("closed");
    }

    @Override public boolean isValidAction(String actionValue) { return true; }
    @Override public String getDescription() { return "Closes the form (no-op)"; }
    @Override public String[] getUsageExamples() { return new String[]{type + ":"}; }
}
