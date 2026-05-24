package it.pintux.life.essentialsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.service.BedrockEconomyShopService;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import it.pintux.life.essentialsaddon.util.ShopGuiActionPayloads;
import org.bukkit.entity.Player;

public final class OpenEconomyShopItemAction implements ActionSystem.ActionHandler {
    private final BedrockEconomyShopService service;

    public OpenEconomyShopItemAction(BedrockEconomyShopService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "economyshop_item";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        ShopGuiActionPayloads.ItemPayload payload = ShopGuiActionPayloads.decodeItem(actionValue);
        service.openItemMenu(bukkitPlayer, payload.shopId(), payload.itemId(), payload.page());
        return ActionSystem.ActionResult.success("Opened EconomyShopGUI item menu");
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return actionValue != null && actionValue.split("\\|", -1).length == 3;
    }

    @Override
    public String getDescription() {
        return "Opens a Bedrock EconomyShopGUI item action menu";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"economyshop_item:blocks|pages.page1.items.item1|1"};
    }
}
