package it.pintux.life.bedwarsaddon.action;

import it.pintux.life.bedwarsaddon.service.BedrockUpgradeService;
import it.pintux.life.bedwarsaddon.util.BedwarsActionPayloads;
import it.pintux.life.bedwarsaddon.util.FormPlayerResolver;
import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import org.bukkit.entity.Player;

public final class UpgradeBuyAction implements ActionSystem.ActionHandler {
    private final BedrockUpgradeService service;

    public UpgradeBuyAction(BedrockUpgradeService service) { this.service = service; }

    @Override public String getActionType() { return "bw_upgrade_buy"; }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player p = FormPlayerResolver.resolve(player);
        if (p == null) return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        String upgradeId = BedwarsActionPayloads.decode(actionValue);
        service.buy(p, upgradeId);
        return ActionSystem.ActionResult.success("Upgrade attempted");
    }

    @Override public boolean isValidAction(String actionValue) { return actionValue != null && !actionValue.isBlank(); }
    @Override public String getDescription() { return "Buys a team upgrade"; }
    @Override public String[] getUsageExamples() { return new String[]{"bw_upgrade_buy:<base64-id>"}; }
}
