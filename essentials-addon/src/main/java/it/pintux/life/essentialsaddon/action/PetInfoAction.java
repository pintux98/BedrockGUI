package it.pintux.life.essentialsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.service.BedrockPetService;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import it.pintux.life.essentialsaddon.util.PetActionPayloads;
import org.bukkit.entity.Player;

public final class PetInfoAction implements ActionSystem.ActionHandler {
    private final BedrockPetService service;

    public PetInfoAction(BedrockPetService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "essentials_pet_info";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        service.openPetModal(bukkitPlayer, PetActionPayloads.decodePet(actionValue));
        return ActionSystem.ActionResult.success("Opened pet modal");
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return actionValue != null && !actionValue.isBlank();
    }

    @Override
    public String getDescription() {
        return "Opens the pet info modal";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"essentials_pet_info:<uuid>"};
    }
}
