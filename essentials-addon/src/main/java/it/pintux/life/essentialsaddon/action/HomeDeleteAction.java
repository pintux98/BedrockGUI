package it.pintux.life.essentialsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.service.BedrockHomeService;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import org.bukkit.entity.Player;

public final class HomeDeleteAction implements ActionSystem.ActionHandler {
    private final BedrockHomeService service;

    public HomeDeleteAction(BedrockHomeService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "home_delete";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        service.showDeleteHomeForm(bukkitPlayer);
        return ActionSystem.ActionResult.success("Opened delete home form");
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Opens the form to delete a home";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"home_delete:"};
    }
}
