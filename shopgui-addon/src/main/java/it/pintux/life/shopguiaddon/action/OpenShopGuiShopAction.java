package it.pintux.life.shopguiaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.shopguiaddon.service.BedrockShopGuiService;
import it.pintux.life.shopguiaddon.util.FormPlayerResolver;
import it.pintux.life.shopguiaddon.util.ShopGuiActionPayloads;
import org.bukkit.entity.Player;

public final class OpenShopGuiShopAction implements ActionSystem.ActionHandler {
    private final BedrockShopGuiService service;

    public OpenShopGuiShopAction(BedrockShopGuiService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "shopgui_shop";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        ShopGuiActionPayloads.ShopPayload payload = ShopGuiActionPayloads.decodeShop(actionValue);
        service.openShop(bukkitPlayer, payload.shopId(), payload.page());
        return ActionSystem.ActionResult.success("Opened Bedrock ShopGUI+ category");
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return actionValue != null && actionValue.contains("|");
    }

    @Override
    public String getDescription() {
        return "Opens a specific Bedrock ShopGUI+ category page";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"shopgui_shop:blocks|1", "shopgui_shop:farming|2"};
    }
}
