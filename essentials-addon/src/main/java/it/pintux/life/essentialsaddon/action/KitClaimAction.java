package it.pintux.life.essentialsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.service.BedrockEssentialsService;
import it.pintux.life.essentialsaddon.util.EssentialsActionPayloads;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import org.bukkit.entity.Player;

public final class KitClaimAction implements ActionSystem.ActionHandler {
    private final BedrockEssentialsService service;

    public KitClaimAction(BedrockEssentialsService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "essentials_kit_claim";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        String kitName = EssentialsActionPayloads.decodeKit(actionValue);
        service.claimKit(bukkitPlayer, kitName);
        return ActionSystem.ActionResult.success("Claimed kit: " + kitName);
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return actionValue != null && !actionValue.isBlank();
    }

    @Override
    public String getDescription() {
        return "Claims a specific kit for the player";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"essentials_kit_claim:starter", "essentials_kit_claim:vip"};
    }
}
