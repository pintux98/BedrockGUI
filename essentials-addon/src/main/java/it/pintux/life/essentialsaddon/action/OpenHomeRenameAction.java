package it.pintux.life.essentialsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.service.BedrockHomeService;
import it.pintux.life.essentialsaddon.util.EssentialsActionPayloads;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import org.bukkit.entity.Player;

public final class OpenHomeRenameAction implements ActionSystem.ActionHandler {
    private final BedrockHomeService service;

    public OpenHomeRenameAction(BedrockHomeService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "home_rename";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        String homeName = EssentialsActionPayloads.decodeHome(actionValue);
        service.showRenameHomeForm(bukkitPlayer, homeName);
        return ActionSystem.ActionResult.success("Opened the rename form for home: " + homeName);
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return actionValue != null && !actionValue.isBlank();
    }

    @Override
    public String getDescription() {
        return "Opens the form to rename one home";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"home_rename:base"};
    }
}
