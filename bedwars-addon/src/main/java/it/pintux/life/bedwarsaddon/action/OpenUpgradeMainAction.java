package it.pintux.life.bedwarsaddon.action;

import it.pintux.life.bedwarsaddon.service.BedrockUpgradeService;
import it.pintux.life.bedwarsaddon.util.FormPlayerResolver;
import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import org.bukkit.entity.Player;

public final class OpenUpgradeMainAction implements ActionSystem.ActionHandler {
    private final BedrockUpgradeService service;

    public OpenUpgradeMainAction(BedrockUpgradeService service) { this.service = service; }

    @Override public String getActionType() { return "bw_upgrade_main"; }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player p = FormPlayerResolver.resolve(player);
        if (p == null) return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        service.openMain(p);
        return ActionSystem.ActionResult.success("Opened upgrades");
    }

    @Override public boolean isValidAction(String actionValue) { return true; }
    @Override public String getDescription() { return "Opens the Bedrock team-upgrades menu"; }
    @Override public String[] getUsageExamples() { return new String[]{"bw_upgrade_main:"}; }
}
