package it.pintux.life.bedwarsaddon.action;

import it.pintux.life.bedwarsaddon.service.BedrockShopService;
import it.pintux.life.bedwarsaddon.util.FormPlayerResolver;
import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import org.bukkit.entity.Player;

public final class OpenShopMainAction implements ActionSystem.ActionHandler {
    private final BedrockShopService service;

    public OpenShopMainAction(BedrockShopService service) { this.service = service; }

    @Override public String getActionType() { return "bw_shop_main"; }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player p = FormPlayerResolver.resolve(player);
        if (p == null) return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        service.openMain(p);
        return ActionSystem.ActionResult.success("Opened shop");
    }

    @Override public boolean isValidAction(String actionValue) { return true; }
    @Override public String getDescription() { return "Opens the Bedrock shop category list"; }
    @Override public String[] getUsageExamples() { return new String[]{"bw_shop_main:"}; }
}
