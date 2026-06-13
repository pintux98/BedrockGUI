package it.pintux.life.essentialsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.service.BedrockPetService;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import org.bukkit.entity.Player;

public final class OpenNativePetShopAction implements ActionSystem.ActionHandler {
    private final BedrockPetService service;

    public OpenNativePetShopAction(BedrockPetService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "essentials_pet_shop_open";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        service.openNativeShop(bukkitPlayer, actionValue == null ? "" : actionValue);
        return ActionSystem.ActionResult.success("Opened native pet shop");
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Opens MyPet's native pet shop GUI (Geyser-translated for Bedrock)";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"essentials_pet_shop_open:", "essentials_pet_shop_open:default"};
    }
}
