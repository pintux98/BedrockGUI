package it.pintux.life.essentialsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.service.BedrockEssentialsService;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import org.bukkit.entity.Player;

public final class OpenWarpMainAction implements ActionSystem.ActionHandler {
    private final BedrockEssentialsService service;

    public OpenWarpMainAction(BedrockEssentialsService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "essentials_warp_main";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        service.openWarpMenu(bukkitPlayer);
        return ActionSystem.ActionResult.success("Opened warp menu");
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Opens the Bedrock warp selection menu";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"essentials_warp_main:"};
    }
}
