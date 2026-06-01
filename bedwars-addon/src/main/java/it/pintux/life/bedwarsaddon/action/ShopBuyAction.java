package it.pintux.life.bedwarsaddon.action;

import it.pintux.life.bedwarsaddon.service.BedrockShopService;
import it.pintux.life.bedwarsaddon.util.BedwarsActionPayloads;
import it.pintux.life.bedwarsaddon.util.FormPlayerResolver;
import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import org.bukkit.entity.Player;

public final class ShopBuyAction implements ActionSystem.ActionHandler {
    private final BedrockShopService service;

    public ShopBuyAction(BedrockShopService service) { this.service = service; }

    @Override public String getActionType() { return "bw_shop_buy"; }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player p = FormPlayerResolver.resolve(player);
        if (p == null) return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        String contentId = BedwarsActionPayloads.decode(actionValue);
        service.buy(p, contentId);
        return ActionSystem.ActionResult.success("Purchase attempted");
    }

    @Override public boolean isValidAction(String actionValue) { return actionValue != null && !actionValue.isBlank(); }
    @Override public String getDescription() { return "Buys a shop item"; }
    @Override public String[] getUsageExamples() { return new String[]{"bw_shop_buy:<base64-id>"}; }
}
