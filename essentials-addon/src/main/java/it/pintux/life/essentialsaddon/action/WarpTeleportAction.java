package it.pintux.life.essentialsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.service.BedrockEssentialsService;
import it.pintux.life.essentialsaddon.util.EssentialsActionPayloads;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import org.bukkit.entity.Player;

public final class WarpTeleportAction implements ActionSystem.ActionHandler {
    private final BedrockEssentialsService service;

    public WarpTeleportAction(BedrockEssentialsService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "essentials_warp_teleport";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        String warpName = EssentialsActionPayloads.decodeWarp(actionValue);
        service.teleportToWarp(bukkitPlayer, warpName);
        return ActionSystem.ActionResult.success("Teleported to warp: " + warpName);
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return actionValue != null && !actionValue.isBlank();
    }

    @Override
    public String getDescription() {
        return "Teleports the player to a specific warp";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"essentials_warp_teleport:spawn", "essentials_warp_teleport:lobby"};
    }
}
