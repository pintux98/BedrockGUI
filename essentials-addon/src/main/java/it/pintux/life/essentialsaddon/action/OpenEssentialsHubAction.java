package it.pintux.life.essentialsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.service.BedrockHubService;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import org.bukkit.entity.Player;

public final class OpenEssentialsHubAction implements ActionSystem.ActionHandler {
    private final BedrockHubService hubService;

    public OpenEssentialsHubAction(BedrockHubService hubService) {
        this.hubService = hubService;
    }

    @Override
    public String getActionType() {
        return "essentials_hub";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkit = FormPlayerResolver.resolve(player);
        if (bukkit == null || !hubService.shouldHandle(bukkit)) {
            return ActionSystem.ActionResult.success("Not a Bedrock player or no hub service");
        }
        hubService.openHub(bukkit);
        return ActionSystem.ActionResult.success("Opened hub");
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Opens the unified essentials hub menu";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"essentials_hub:"};
    }
}
