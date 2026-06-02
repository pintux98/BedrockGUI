package it.pintux.life.bedwarsaddon.action;

import it.pintux.life.bedwarsaddon.service.BedrockPartyService;
import it.pintux.life.bedwarsaddon.util.FormPlayerResolver;
import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import org.bukkit.entity.Player;

public final class PartyAddAction implements ActionSystem.ActionHandler {
    private final BedrockPartyService service;

    public PartyAddAction(BedrockPartyService service) { this.service = service; }

    @Override public String getActionType() { return "bw_party_add"; }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player p = FormPlayerResolver.resolve(player);
        if (p == null) return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        service.showAddInput(p);
        return ActionSystem.ActionResult.success("Opened add-player input");
    }

    @Override public boolean isValidAction(String actionValue) { return true; }
    @Override public String getDescription() { return "Opens the add-player input"; }
    @Override public String[] getUsageExamples() { return new String[]{"bw_party_add:"}; }
}
