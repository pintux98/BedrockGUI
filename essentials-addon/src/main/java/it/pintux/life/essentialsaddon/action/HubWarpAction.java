package it.pintux.life.essentialsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.service.BedrockEssentialsService;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import org.bukkit.entity.Player;

public final class HubWarpAction implements ActionSystem.ActionHandler {
    private final BedrockEssentialsService service;

    public HubWarpAction(BedrockEssentialsService service) {
        this.service = service;
    }

    @Override
    public String getActionType() { return "essentials_hub_warp"; }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkit = FormPlayerResolver.resolve(player);
        if (bukkit == null || !service.shouldHandle(bukkit)) return ActionSystem.ActionResult.success("skipped");
        service.openWarpMenu(bukkit);
        return ActionSystem.ActionResult.success("opened warp menu");
    }

    @Override
    public boolean isValidAction(String actionValue) { return true; }

    @Override
    public String getDescription() { return "Redirects from hub to warp menu"; }

    @Override
    public String[] getUsageExamples() { return new String[]{"essentials_hub_warp:"}; }
}
