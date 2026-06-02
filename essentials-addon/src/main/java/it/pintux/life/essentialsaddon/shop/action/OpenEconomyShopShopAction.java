package it.pintux.life.essentialsaddon.shop.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.shopguiaddon.service.BedrockEconomyShopService;
import it.pintux.life.shopguiaddon.util.FormPlayerResolver;
import it.pintux.life.shopguiaddon.util.ShopGuiActionPayloads;
import org.bukkit.entity.Player;

public final class OpenEconomyShopShopAction implements ActionSystem.ActionHandler {
    private final BedrockEconomyShopService service;

    public OpenEconomyShopShopAction(BedrockEconomyShopService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "economyshop_shop";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        ShopGuiActionPayloads.ShopPayload payload = ShopGuiActionPayloads.decodeShop(actionValue);
        service.openShop(bukkitPlayer, payload.shopId(), payload.page());
        return ActionSystem.ActionResult.success("Opened EconomyShopGUI shop");
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return actionValue != null && actionValue.split("\\|", -1).length == 2;
    }

    @Override
    public String getDescription() {
        return "Opens a Bedrock EconomyShopGUI section page";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"economyshop_shop:blocks|1"};
    }
}
