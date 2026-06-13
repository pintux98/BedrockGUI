package it.pintux.life.essentialsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.service.BedrockPetService;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import it.pintux.life.essentialsaddon.util.PetActionPayloads;
import org.bukkit.entity.Player;

public final class BuyPetShopAction implements ActionSystem.ActionHandler {
    private final BedrockPetService service;

    public BuyPetShopAction(BedrockPetService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "essentials_pet_buy";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        String[] parts = PetActionPayloads.decodeShop(actionValue);
        service.buyPet(bukkitPlayer, parts[0], parts[1]);
        return ActionSystem.ActionResult.success("Bought pet: " + parts[1]);
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return actionValue != null && actionValue.contains("|");
    }

    @Override
    public String getDescription() {
        return "Buys a pet from the pet shop";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"essentials_pet_buy:default|wolf1"};
    }
}
