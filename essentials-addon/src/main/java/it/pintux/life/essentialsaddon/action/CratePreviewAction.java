package it.pintux.life.essentialsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.service.BedrockCratePreviewService;
import it.pintux.life.essentialsaddon.util.EssentialsActionPayloads;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import org.bukkit.entity.Player;

public final class CratePreviewAction implements ActionSystem.ActionHandler {
    private final BedrockCratePreviewService service;

    public CratePreviewAction(BedrockCratePreviewService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "crate_preview";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        String crateId = EssentialsActionPayloads.decodeCrate(actionValue);
        service.openCratePreview(bukkitPlayer, crateId);
        return ActionSystem.ActionResult.success("Opened crate preview: " + crateId);
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return actionValue != null && !actionValue.isBlank();
    }

    @Override
    public String getDescription() {
        return "Opens a Bedrock crate preview form";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"crate_preview:common", "crate_preview:rare"};
    }
}
