package it.pintux.life.bedwarsaddon.action;

import it.pintux.life.bedwarsaddon.service.BedrockPartyService;
import it.pintux.life.bedwarsaddon.util.BedwarsActionPayloads;
import it.pintux.life.bedwarsaddon.util.FormPlayerResolver;
import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import org.bukkit.entity.Player;

public final class PartyKickDoAction implements ActionSystem.ActionHandler {
    private final BedrockPartyService service;

    public PartyKickDoAction(BedrockPartyService service) { this.service = service; }

    @Override public String getActionType() { return "bw_party_kickdo"; }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player p = FormPlayerResolver.resolve(player);
        if (p == null) return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        String targetName = BedwarsActionPayloads.decode(actionValue);
        service.kick(p, targetName);
        return ActionSystem.ActionResult.success("Kick attempted");
    }

    @Override public boolean isValidAction(String actionValue) { return actionValue != null && !actionValue.isBlank(); }
    @Override public String getDescription() { return "Kicks a party member"; }
    @Override public String[] getUsageExamples() { return new String[]{"bw_party_kickdo:<base64-name>"}; }
}
