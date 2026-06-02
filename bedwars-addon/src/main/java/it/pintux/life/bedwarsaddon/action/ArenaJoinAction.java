package it.pintux.life.bedwarsaddon.action;

import it.pintux.life.bedwarsaddon.service.BedrockArenaService;
import it.pintux.life.bedwarsaddon.util.BedwarsActionPayloads;
import it.pintux.life.bedwarsaddon.util.FormPlayerResolver;
import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import org.bukkit.entity.Player;

public final class ArenaJoinAction implements ActionSystem.ActionHandler {
    private final BedrockArenaService service;

    public ArenaJoinAction(BedrockArenaService service) { this.service = service; }

    @Override public String getActionType() { return "bw_arena_join"; }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player p = FormPlayerResolver.resolve(player);
        if (p == null) return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        String arenaName = BedwarsActionPayloads.decode(actionValue);
        service.join(p, arenaName);
        return ActionSystem.ActionResult.success("Join attempted");
    }

    @Override public boolean isValidAction(String actionValue) { return actionValue != null && !actionValue.isBlank(); }
    @Override public String getDescription() { return "Joins a Bedwars arena"; }
    @Override public String[] getUsageExamples() { return new String[]{"bw_arena_join:<base64-name>"}; }
}
