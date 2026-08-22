package it.pintux.life.essentialsaddon.service;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.essentialsaddon.api.BedrockPlayerDetector;
import it.pintux.life.essentialsaddon.config.EssentialsAddonConfiguration;
import it.pintux.life.essentialsaddon.util.BukkitFormPlayer;
import it.pintux.life.essentialsaddon.util.BedrockSoundFeedback;
import org.bukkit.entity.Player;

import java.util.function.BooleanSupplier;

public final class BedrockHubService {
    private final EssentialsAddonConfiguration configuration;
    private final BedrockPlayerDetector detector;
    private final BedrockSoundFeedback soundFeedback;
    private BooleanSupplier publicHomesAvailable = () -> false;

    public BedrockHubService(EssentialsAddonConfiguration configuration,
                             BedrockPlayerDetector detector,
                             BedrockSoundFeedback soundFeedback) {
        this.configuration = configuration;
        this.detector = detector;
        this.soundFeedback = soundFeedback;
    }

    /**
     * Set by the plugin once the homes module is built, because the hub is created first and the
     * button only belongs there when the active provider serves public homes.
     */
    public void setPublicHomesAvailable(BooleanSupplier publicHomesAvailable) {
        this.publicHomesAvailable = publicHomesAvailable == null ? () -> false : publicHomesAvailable;
    }

    public boolean shouldHandle(Player player) {
        return player != null && detector.isBedrockPlayer(player);
    }

    public void openHub(Player player) {
        openHub(player, true);
    }

    /**
     * @param openedByButton true when a form button led here. The Bedrock client plays its own
     *                       click for that tap, so the form-open sound would be the second one
     *                       heard unless sounds.play-when-opened-by-button says otherwise.
     */
    public void openHub(Player player, boolean openedByButton) {
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
        if (configuration.moduleHomes() && publicHomesAvailable.getAsBoolean()) {
            form.button(configuration.hubButtonPublicHomes(), formPlayer -> {
                try {
                    api.executeActionString(formPlayer, "public_home_main:",
                            api.createActionContext(java.util.Map.of(), java.util.Map.of(),
                                    java.util.Map.of("source", "hub", "feature", "public-homes"), "hub", "simple"));
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
        if (!openedByButton || configuration.soundOnButtonOpen()) {
            soundFeedback.playFormOpen(player);
        }
    }
}
