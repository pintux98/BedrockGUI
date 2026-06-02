package it.pintux.life.essentialsaddon.shop.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.shopguiaddon.service.BedrockEconomyShopService;
import it.pintux.life.shopguiaddon.util.FormPlayerResolver;
import org.bukkit.entity.Player;

public final class OpenEconomyShopMainAction implements ActionSystem.ActionHandler {
    private final BedrockEconomyShopService service;

    public OpenEconomyShopMainAction(BedrockEconomyShopService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "economyshop_main";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        service.openMainMenu(bukkitPlayer);
        return ActionSystem.ActionResult.success("Opened EconomyShopGUI main menu");
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return actionValue == null || actionValue.isBlank() || "main".equalsIgnoreCase(actionValue);
    }

    @Override
    public String getDescription() {
        return "Opens the Bedrock EconomyShopGUI category menu";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"economyshop_main:main", "economyshop_main:"};
    }
}
