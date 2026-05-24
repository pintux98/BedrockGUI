package it.pintux.life.essentialsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.service.BedrockEconomyShopService;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import it.pintux.life.essentialsaddon.util.ShopGuiActionPayloads;
import org.bukkit.entity.Player;

public final class ExecuteEconomyShopTransactionAction implements ActionSystem.ActionHandler {
    private final BedrockEconomyShopService service;

    public ExecuteEconomyShopTransactionAction(BedrockEconomyShopService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "economyshop_transaction";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        ShopGuiActionPayloads.TransactionPayload payload = ShopGuiActionPayloads.decodeTransaction(actionValue);
        var result = service.executeTransaction(bukkitPlayer, payload.action(), payload.shopId(), payload.itemId(), payload.amount(), payload.page());
        return result.success()
                ? ActionSystem.ActionResult.success(result.message())
                : ActionSystem.ActionResult.failure(result.message());
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return actionValue != null && actionValue.split("\\|", -1).length == 5;
    }

    @Override
    public String getDescription() {
        return "Executes a Bedrock EconomyShopGUI buy or sell request";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"economyshop_transaction:BUY|blocks|pages.page1.items.item1|16|1"};
    }
}
