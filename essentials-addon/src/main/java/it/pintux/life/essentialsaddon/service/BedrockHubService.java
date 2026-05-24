package it.pintux.life.essentialsaddon.service;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.essentialsaddon.api.BedrockPlayerDetector;
import it.pintux.life.essentialsaddon.config.EssentialsAddonConfiguration;
import it.pintux.life.essentialsaddon.util.BukkitFormPlayer;
import it.pintux.life.essentialsaddon.util.BedrockSoundFeedback;
import org.bukkit.entity.Player;

public final class BedrockHubService {
    private final EssentialsAddonConfiguration configuration;
    private final BedrockPlayerDetector detector;
    private final BedrockSoundFeedback soundFeedback;

    public BedrockHubService(EssentialsAddonConfiguration configuration,
                             BedrockPlayerDetector detector,
                             BedrockSoundFeedback soundFeedback) {
        this.configuration = configuration;
        this.detector = detector;
        this.soundFeedback = soundFeedback;
    }

    public boolean shouldHandle(Player player) {
        return player != null && detector.isBedrockPlayer(player);
    }

    public void openHub(Player player) {
        BedrockGUIApi api = BedrockGUIApi.getInstance();
        if (api == null) {
            player.sendMessage(configuration.noBedrockGui());
            return;
        }

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(configuration.hubTitle());
        form.content(configuration.hubContent());

        if (configuration.moduleWarps()) {
            form.button(configuration.hubButtonWarps(), formPlayer -> {
                try {
                    api.executeActionString(formPlayer, "essentials_hub_warp:",
                            api.createActionContext(java.util.Map.of(), java.util.Map.of(),
                                    java.util.Map.of("source", "hub", "feature", "warps"), "hub", "simple"));
                } catch (IllegalStateException ignored) {}
            });
        }
        if (configuration.moduleKits()) {
            form.button(configuration.hubButtonKits(), formPlayer -> {
                try {
                    api.executeActionString(formPlayer, "essentials_hub_kit:",
                            api.createActionContext(java.util.Map.of(), java.util.Map.of(),
                                    java.util.Map.of("source", "hub", "feature", "kits"), "hub", "simple"));
                } catch (IllegalStateException ignored) {}
            });
        }
        if (configuration.moduleHomes()) {
            form.button(configuration.hubButtonHomes(), formPlayer -> {
                try {
                    api.executeActionString(formPlayer, "essentials_hub_home:",
                            api.createActionContext(java.util.Map.of(), java.util.Map.of(),
                                    java.util.Map.of("source", "hub", "feature", "homes"), "hub", "simple"));
                } catch (IllegalStateException ignored) {}
            });
        }
        if (configuration.moduleTpa()) {
            form.button(configuration.hubButtonTpa(), formPlayer -> {
                try {
                    api.executeActionString(formPlayer, "essentials_hub_tpa:",
                            api.createActionContext(java.util.Map.of(), java.util.Map.of(),
                                    java.util.Map.of("source", "hub", "feature", "tpa"), "hub", "simple"));
                } catch (IllegalStateException ignored) {}
            });
        }
        if (configuration.moduleShopGuiPlus()) {
            form.button(configuration.hubButtonShopGuiPlus(), formPlayer -> {
                try {
                    api.executeActionString(formPlayer, "shopgui_main:",
                            api.createActionContext(java.util.Map.of(), java.util.Map.of(),
                                    java.util.Map.of("source", "hub", "feature", "shopgui"), "hub", "simple"));
                } catch (IllegalStateException ignored) {}
            });
        }
        if (configuration.moduleEconomyShopGui()) {
            form.button(configuration.hubButtonEconomyShopGui(), formPlayer -> {
                try {
                    api.executeActionString(formPlayer, "economyshop_main:",
                            api.createActionContext(java.util.Map.of(), java.util.Map.of(),
                                    java.util.Map.of("source", "hub", "feature", "economyshop"), "hub", "simple"));
                } catch (IllegalStateException ignored) {}
            });
        }

        form.send(new BukkitFormPlayer(player));
        soundFeedback.playFormOpen(player);
    }
}
