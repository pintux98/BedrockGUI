package it.pintux.life.bedwarsaddon.service;

import it.pintux.life.bedwarsaddon.api.BedrockPlayerDetector;
import it.pintux.life.bedwarsaddon.config.BedwarsAddonConfiguration;
import it.pintux.life.bedwarsaddon.menu.MenuButton;
import it.pintux.life.bedwarsaddon.menu.SpectatorMenuModel;
import it.pintux.life.bedwarsaddon.model.SpectateTarget;
import it.pintux.life.bedwarsaddon.util.BedrockSoundFeedback;
import it.pintux.life.bedwarsaddon.util.BukkitFormPlayer;
import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.api.BedrockGUIApi;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public final class BedrockSpectatorService {
    private final BedwarsAddonConfiguration config;
    private final SpectatorCatalogService catalog;
    private final SpectatorMenuModel menuModel;
    private final BedrockPlayerDetector detector;
    private final BedrockSoundFeedback sound;

    public BedrockSpectatorService(BedwarsAddonConfiguration config, SpectatorCatalogService catalog,
                                   BedrockPlayerDetector detector, BedrockSoundFeedback sound) {
        this.config = config;
        this.catalog = catalog;
        this.detector = detector;
        this.sound = sound;
        this.menuModel = new SpectatorMenuModel(config);
    }

    public boolean shouldHandle(Player player) {
        return player != null && detector.isBedrockPlayer(player);
    }

    /** True if the given inventory title is the native teleporter GUI (config substring match). */
    public boolean matchesTitle(String title) {
        if (title == null) return false;
        String needle = config.spectatorGuiTitleContains();
        return needle != null && !needle.isBlank()
                && title.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    public void openTeleporter(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!catalog.isReady()) { player.sendMessage(config.spectatorProviderUnavailable()); return; }

        List<SpectateTarget> targets = catalog.getTargets(player);
        if (targets.isEmpty()) { player.sendMessage(config.spectatorNoTargets()); return; }

        List<MenuButton> buttons = menuModel.targetButtons(targets);
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(config.spectatorTitle());
        form.content(config.spectatorContent());
        for (MenuButton button : buttons) {
            String action = button.actionString();
            form.button(button.label(), fp -> api.executeActionString(fp, action, ctx()));
        }
        sound.playFormOpen(player);
        form.send(new BukkitFormPlayer(player));
    }

    public void teleport(Player player, String targetUuid) {
        if (!catalog.isReady()) { player.sendMessage(config.spectatorProviderUnavailable()); return; }
        catalog.teleport(player, targetUuid);
    }

    private BedrockGUIApi requireApi(Player player) {
        try {
            return BedrockGUIApi.getInstance();
        } catch (IllegalStateException e) {
            player.sendMessage(config.spectatorProviderUnavailable());
            return null;
        }
    }

    private ActionSystem.ActionContext ctx() {
        return ActionSystem.ActionContext.builder()
                .menuName("spectator")
                .formType("bedrock-bw-spectator")
                .build();
    }
}
