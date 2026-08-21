package it.pintux.life.essentialsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.service.BedrockHomeService;
import it.pintux.life.essentialsaddon.util.EssentialsActionPayloads;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import org.bukkit.entity.Player;

public final class HomeMakePrivateAction implements ActionSystem.ActionHandler {
    private final BedrockHomeService service;

    public HomeMakePrivateAction(BedrockHomeService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "home_make_private";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        String homeName = EssentialsActionPayloads.decodeHome(actionValue);
        service.setHomePrivacy(bukkitPlayer, homeName, false);
        return ActionSystem.ActionResult.success("Made home private: " + homeName);
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return actionValue != null && !actionValue.isBlank();
    }

    @Override
    public String getDescription() {
        return "Makes one of the player\u0027s homes private";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"home_make_private:base"};
    }
}
