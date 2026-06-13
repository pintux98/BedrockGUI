package it.pintux.life.essentialsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.service.BedrockPetService;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import org.bukkit.entity.Player;

public final class OpenPetSkilltreeAction implements ActionSystem.ActionHandler {
    private final BedrockPetService service;

    public OpenPetSkilltreeAction(BedrockPetService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "essentials_pet_skilltree_menu";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        service.openSkilltreeForm(bukkitPlayer);
        return ActionSystem.ActionResult.success("Opened skilltree menu");
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Opens the pet skilltree selection";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"essentials_pet_skilltree_menu:"};
    }
}
