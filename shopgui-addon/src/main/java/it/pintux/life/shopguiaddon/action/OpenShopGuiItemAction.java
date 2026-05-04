package it.pintux.life.shopguiaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.shopguiaddon.service.BedrockShopGuiService;
import it.pintux.life.shopguiaddon.util.FormPlayerResolver;
import it.pintux.life.shopguiaddon.util.ShopGuiActionPayloads;
import org.bukkit.entity.Player;

public final class OpenShopGuiItemAction implements ActionSystem.ActionHandler {
    private final BedrockShopGuiService service;

    public OpenShopGuiItemAction(BedrockShopGuiService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "shopgui_item";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        ShopGuiActionPayloads.ItemPayload payload = ShopGuiActionPayloads.decodeItem(actionValue);
        service.openItemMenu(bukkitPlayer, payload.shopId(), payload.itemId(), payload.page());
        return ActionSystem.ActionResult.success("Opened ShopGUI+ item actions");
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return actionValue != null && actionValue.split("\\|", -1).length == 3;
    }

    @Override
    public String getDescription() {
        return "Opens an item action screen for a ShopGUI+ item";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"shopgui_item:blocks|stone|1"};
    }
}
