package it.pintux.life.essentialsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.service.BedrockCratePreviewService;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import org.bukkit.entity.Player;

public final class OpenCrateMainAction implements ActionSystem.ActionHandler {
    private final BedrockCratePreviewService service;

    public OpenCrateMainAction(BedrockCratePreviewService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "crate_main";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        service.openCrateMenu(bukkitPlayer);
        return ActionSystem.ActionResult.success("Opened crate menu");
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Opens the Bedrock crate selection menu";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"crate_main:"};
    }
}
