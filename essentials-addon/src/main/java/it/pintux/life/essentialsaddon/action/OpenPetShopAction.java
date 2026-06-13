package it.pintux.life.essentialsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.service.BedrockPetService;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import org.bukkit.entity.Player;

public final class OpenPetShopAction implements ActionSystem.ActionHandler {
    private final BedrockPetService service;

    public OpenPetShopAction(BedrockPetService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "essentials_pet_shop";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        if (actionValue == null || actionValue.isBlank()) {
            service.openPetShop(bukkitPlayer);
        } else {
            service.openPetShop(bukkitPlayer, actionValue);
        }
        return ActionSystem.ActionResult.success("Opened pet shop");
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Opens the Bedrock pet shop";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"essentials_pet_shop:", "essentials_pet_shop:default"};
    }
}
