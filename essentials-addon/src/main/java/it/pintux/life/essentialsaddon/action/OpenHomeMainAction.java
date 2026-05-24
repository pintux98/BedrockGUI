package it.pintux.life.essentialsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.service.BedrockHomeService;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import org.bukkit.entity.Player;

public final class OpenHomeMainAction implements ActionSystem.ActionHandler {
    private final BedrockHomeService service;

    public OpenHomeMainAction(BedrockHomeService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "home_main";
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
            } catch (NumberFormatException ignored) {}
        }
        service.openHomeMenu(bukkitPlayer, page);
        return ActionSystem.ActionResult.success("Opened home menu");
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Opens the Bedrock home selection menu";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"home_main:", "home_main:2"};
    }
}
