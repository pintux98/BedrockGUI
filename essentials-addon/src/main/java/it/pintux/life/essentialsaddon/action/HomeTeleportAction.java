package it.pintux.life.essentialsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.service.BedrockHomeService;
import it.pintux.life.essentialsaddon.util.EssentialsActionPayloads;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import org.bukkit.entity.Player;

public final class HomeTeleportAction implements ActionSystem.ActionHandler {
    private final BedrockHomeService service;

    public HomeTeleportAction(BedrockHomeService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "home_teleport";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        String homeName = EssentialsActionPayloads.decodeHome(actionValue);
        service.teleportHome(bukkitPlayer, homeName);
        return ActionSystem.ActionResult.success("Teleported to home: " + homeName);
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return actionValue != null && !actionValue.isBlank();
    }

    @Override
    public String getDescription() {
        return "Teleports the player to a specific home";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"home_teleport:spawn", "home_teleport:base"};
    }
}
