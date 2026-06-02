package it.pintux.life.essentialsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.service.BedrockCrateOpeningService;
import it.pintux.life.essentialsaddon.util.EssentialsActionPayloads;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import org.bukkit.entity.Player;

public final class CrateOpenAction implements ActionSystem.ActionHandler {
    private final BedrockCrateOpeningService service;

    public CrateOpenAction(BedrockCrateOpeningService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "crate_open";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        String crateId = EssentialsActionPayloads.decodeCrate(actionValue);
        service.openCrate(bukkitPlayer, crateId);
        return ActionSystem.ActionResult.success("Opened crate: " + crateId);
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return actionValue != null && !actionValue.isBlank();
    }

    @Override
    public String getDescription() {
        return "Opens a crate for the player";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"crate_open:common", "crate_open:rare"};
    }
}
