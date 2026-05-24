package it.pintux.life.essentialsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.service.BedrockHomeService;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import org.bukkit.entity.Player;

public final class HubHomeAction implements ActionSystem.ActionHandler {
    private final BedrockHomeService service;

    public HubHomeAction(BedrockHomeService service) {
        this.service = service;
    }

    @Override
    public String getActionType() { return "essentials_hub_home"; }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkit = FormPlayerResolver.resolve(player);
        if (bukkit == null || !service.shouldHandle(bukkit)) return ActionSystem.ActionResult.success("skipped");
        service.openHomeMenu(bukkit, 1);
        return ActionSystem.ActionResult.success("opened home menu");
    }

    @Override
    public boolean isValidAction(String actionValue) { return true; }

    @Override
    public String getDescription() { return "Redirects from hub to home menu"; }

    @Override
    public String[] getUsageExamples() { return new String[]{"essentials_hub_home:"}; }
}
