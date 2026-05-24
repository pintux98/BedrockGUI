package it.pintux.life.essentialsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.service.BedrockTpaService;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import org.bukkit.entity.Player;

public final class OpenTpaMainAction implements ActionSystem.ActionHandler {
    private final BedrockTpaService service;

    public OpenTpaMainAction(BedrockTpaService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "tpa_main";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        service.openTpaMenu(bukkitPlayer);
        return ActionSystem.ActionResult.success("Opened TPA menu");
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Opens the Bedrock TPA management menu";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"tpa_main:"};
    }
}
