package it.pintux.life.shopguiaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.shopguiaddon.service.BedrockShopGuiService;
import it.pintux.life.shopguiaddon.util.FormPlayerResolver;
import it.pintux.life.shopguiaddon.util.ShopGuiActionPayloads;
import org.bukkit.entity.Player;

public final class ExecuteShopGuiTransactionAction implements ActionSystem.ActionHandler {
    private final BedrockShopGuiService service;

    public ExecuteShopGuiTransactionAction(BedrockShopGuiService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "shopgui_transaction";
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
        return "Executes a Bedrock ShopGUI+ buy, sell, or trade request";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"shopgui_transaction:BUY|blocks|stone|16|1"};
    }
}
