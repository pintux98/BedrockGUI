package it.pintux.life.bedwarsaddon.service;

import it.pintux.life.bedwarsaddon.api.BedrockPlayerDetector;
import it.pintux.life.bedwarsaddon.config.BedwarsAddonConfiguration;
import it.pintux.life.bedwarsaddon.menu.StatsMenuModel;
import it.pintux.life.bedwarsaddon.model.PlayerStatsInfo;
import it.pintux.life.bedwarsaddon.util.BedrockSoundFeedback;
import it.pintux.life.bedwarsaddon.util.BukkitFormPlayer;
import it.pintux.life.common.api.BedrockGUIApi;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class BedrockStatsService {
    private final BedwarsAddonConfiguration config;
    private final StatsCatalogService catalog;
    private final StatsMenuModel menuModel;
    private final BedrockPlayerDetector detector;
    private final BedrockSoundFeedback sound;

    public BedrockStatsService(BedwarsAddonConfiguration config, StatsCatalogService catalog,
                               BedrockPlayerDetector detector, BedrockSoundFeedback sound) {
        this.config = config;
        this.catalog = catalog;
        this.detector = detector;
        this.sound = sound;
        this.menuModel = new StatsMenuModel(config);
    }

    public boolean shouldHandle(Player player) {
        return player != null && detector.isBedrockPlayer(player);
    }

    /** True if the given inventory title is the native stats GUI (config substring match). */
    public boolean matchesTitle(String title) {
        if (title == null) return false;
        String needle = config.statsGuiTitleContains();
        return needle != null && !needle.isBlank()
                && title.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    public void openStats(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!catalog.isReady()) { player.sendMessage(config.statsProviderUnavailable()); return; }

        PlayerStatsInfo stats = catalog.getStats(player);
        if (stats == null) { player.sendMessage(config.statsProviderUnavailable()); return; }

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(config.statsTitle());
        form.content(menuModel.content(stats));
        form.button(config.statsCloseButton(), fp -> { /* no-op: closes the form */ });
        sound.playFormOpen(player);
        form.send(new BukkitFormPlayer(player));
    }

    private BedrockGUIApi requireApi(Player player) {
        try {
            return BedrockGUIApi.getInstance();
        } catch (IllegalStateException e) {
            player.sendMessage(config.statsProviderUnavailable());
            return null;
        }
    }
}
