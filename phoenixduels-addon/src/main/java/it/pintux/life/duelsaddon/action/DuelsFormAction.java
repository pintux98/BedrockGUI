package it.pintux.life.duelsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.duelsaddon.util.FormPlayerResolver;
import org.bukkit.entity.Player;

/**
 * One reusable BedrockGUI action handler for every {@code pd_*} action.
 *
 * <p>The plugin registers one instance per action type with a lambda body, so the whole routing
 * table lives in {@code DuelsAddonPlugin.registerActions} instead of being spread across a class
 * per action. Payload decoding throws {@link IllegalArgumentException}, which is converted here
 * into a failed action carrying the message, so a typo in a menu YAML reports itself instead of
 * failing silently.</p>
 */
public final class DuelsFormAction implements ActionSystem.ActionHandler {

    /**
     * What to run once the Bukkit player is resolved.
     */
    @FunctionalInterface
    public interface Callback {
        /**
         * @param actionValue the raw payload after the colon, or an empty string
         */
        void run(Player player, String actionValue);
    }

    private final String type;
    private final String description;
    private final Callback callback;

    public DuelsFormAction(String type, String description, Callback callback) {
        this.type = type;
        this.description = description;
        this.callback = callback;
    }

    @Override
    public String getActionType() {
        return type;
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkit = FormPlayerResolver.resolve(player);
        if (bukkit == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        try {
            callback.run(bukkit, actionValue);
            return ActionSystem.ActionResult.success();
        } catch (IllegalArgumentException e) {
            return ActionSystem.ActionResult.failure(e.getMessage());
        }
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return true;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{type + ":"};
    }
}
