package it.pintux.life.essentialsaddon.action;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.essentialsaddon.service.BedrockHomeService;
import it.pintux.life.essentialsaddon.util.EssentialsActionPayloads;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import org.bukkit.entity.Player;

public final class PublicHomeTeleportAction implements ActionSystem.ActionHandler {
    private final BedrockHomeService service;

    public PublicHomeTeleportAction(BedrockHomeService service) {
        this.service = service;
    }

    @Override
    public String getActionType() {
        return "public_home_teleport";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player bukkitPlayer = FormPlayerResolver.resolve(player);
        if (bukkitPlayer == null) {
            return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        }
        String identifier = EssentialsActionPayloads.decodeHome(actionValue);
        service.teleportPublicHome(bukkitPlayer, identifier);
        return ActionSystem.ActionResult.success("Teleporting to public home: " + identifier);
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return actionValue != null && !actionValue.isBlank();
    }

    @Override
    public String getDescription() {
        return "Teleports the player to a public home, addressed as owner.name";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"public_home_teleport:Steve.shop", "public_home_teleport:Alex.mine"};
    }
}
