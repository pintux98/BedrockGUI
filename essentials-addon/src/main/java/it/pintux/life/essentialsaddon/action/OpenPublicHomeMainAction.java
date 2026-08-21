package it.pintux.life.essentialsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.service.BedrockHomeService;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import org.bukkit.entity.Player;

public final class OpenPublicHomeMainAction implements ActionSystem.ActionHandler {
    private final BedrockHomeService service;

    public OpenPublicHomeMainAction(BedrockHomeService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "public_home_main";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        int page = 1;
        if (actionValue != null && !actionValue.isBlank()) {
            try {
                page = Integer.parseInt(actionValue.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        service.openPublicHomeMenu(bukkitPlayer, page);
        return ActionSystem.ActionResult.success("Opened public home menu");
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Opens the Bedrock public home selection menu";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"public_home_main:", "public_home_main:2"};
    }
}
