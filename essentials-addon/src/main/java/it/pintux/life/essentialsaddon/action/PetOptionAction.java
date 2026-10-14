package it.pintux.life.essentialsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.service.BedrockPetService;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import it.pintux.life.essentialsaddon.util.PetActionPayloads;
import org.bukkit.entity.Player;

public final class PetOptionAction implements ActionSystem.ActionHandler {
    private final BedrockPetService service;

    public PetOptionAction(BedrockPetService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "essentials_pet_option";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        String[] parts = PetActionPayloads.decodeOption(actionValue);
        service.runPetOption(bukkitPlayer, parts[0], parts[1]);
        return ActionSystem.ActionResult.success("Ran pet option");
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return actionValue != null && actionValue.contains("|");
    }

    @Override
    public String getDescription() {
        return "Runs a pet menu option: rename, mount, inventory, skins or signal-stick";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"essentials_pet_option:<pet-id>|<option>"};
    }
}
