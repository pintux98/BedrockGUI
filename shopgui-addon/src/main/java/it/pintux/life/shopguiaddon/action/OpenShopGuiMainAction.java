package it.pintux.life.shopguiaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.shopguiaddon.service.BedrockShopGuiService;
import it.pintux.life.shopguiaddon.util.FormPlayerResolver;
import org.bukkit.entity.Player;

public final class OpenShopGuiMainAction implements ActionSystem.ActionHandler {
    private final BedrockShopGuiService service;

    public OpenShopGuiMainAction(BedrockShopGuiService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "shopgui_main";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        service.openMainMenu(bukkitPlayer);
        return ActionSystem.ActionResult.success("Opened ShopGUI+ main menu");
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return actionValue == null || actionValue.isBlank() || "main".equalsIgnoreCase(actionValue);
    }

    @Override
    public String getDescription() {
        return "Opens the Bedrock ShopGUI+ category menu";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"shopgui_main:main", "shopgui_main:"};
    }
}
