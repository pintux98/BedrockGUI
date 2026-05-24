package it.pintux.life.essentialsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.service.BedrockHomeService;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import org.bukkit.entity.Player;

public final class HomeSetAction implements ActionSystem.ActionHandler {
    private final BedrockHomeService service;

    public HomeSetAction(BedrockHomeService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "home_set";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        service.showSetHomeForm(bukkitPlayer);
        return ActionSystem.ActionResult.success("Opened set home form");
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Opens the form to set a new home";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"home_set:"};
    }
}
