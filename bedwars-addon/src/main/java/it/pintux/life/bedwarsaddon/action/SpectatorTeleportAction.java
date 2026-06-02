package it.pintux.life.bedwarsaddon.action;

import it.pintux.life.bedwarsaddon.service.BedrockSpectatorService;
import it.pintux.life.bedwarsaddon.util.BedwarsActionPayloads;
import it.pintux.life.bedwarsaddon.util.FormPlayerResolver;
import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import org.bukkit.entity.Player;

public final class SpectatorTeleportAction implements ActionSystem.ActionHandler {
    private final BedrockSpectatorService service;

    public SpectatorTeleportAction(BedrockSpectatorService service) { this.service = service; }

    @Override public String getActionType() { return "bw_spec_tp"; }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player p = FormPlayerResolver.resolve(player);
        if (p == null) return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        String targetUuid = BedwarsActionPayloads.decode(actionValue);
        service.teleport(p, targetUuid);
        return ActionSystem.ActionResult.success("Teleport attempted");
    }

    @Override public boolean isValidAction(String actionValue) { return actionValue != null && !actionValue.isBlank(); }
    @Override public String getDescription() { return "Teleports a spectator to a player"; }
    @Override public String[] getUsageExamples() { return new String[]{"bw_spec_tp:<base64-uuid>"}; }
}
