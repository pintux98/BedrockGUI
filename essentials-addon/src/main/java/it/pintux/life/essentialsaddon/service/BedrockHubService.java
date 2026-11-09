package it.pintux.life.essentialsaddon.service;

import it.pintux.life.paper.addon.AddonText;
import it.pintux.life.paper.addon.BedrockPlayerDetector;
import it.pintux.life.paper.utils.PaperPlayer;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.essentialsaddon.config.EssentialsAddonConfiguration;
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
     *                       click for that tap, so the form-open sound is left out to avoid
     *                       hearing the same sound twice.
     */
    public void openHub(Player player, boolean openedByButton) {
        BedrockGUIApi api = BedrockGUIApi.getInstance();
        if (api == null) {
            AddonText.send(player, configuration.noBedrockGui());
            return;
        }

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(configuration.hubTitle());
        form.content(configuration.hubContent());

        if (configuration.moduleWarps() && has(api, "essentials_hub_warp")) {
            form.button(configuration.hubButtonWarps(), formPlayer -> {
                try {
                    api.executeActionString(formPlayer, "essentials_hub_warp:",
                            api.createActionContext(java.util.Map.of(), java.util.Map.of(),
                                    java.util.Map.of("source", "hub", "feature", "warps"), "hub", "simple"));
                } catch (IllegalStateException ignored) {}
            });
        }
        if (configuration.moduleKits() && has(api, "essentials_hub_kit")) {
            form.button(configuration.hubButtonKits(), formPlayer -> {
                try {
                    api.executeActionString(formPlayer, "essentials_hub_kit:",
                            api.createActionContext(java.util.Map.of(), java.util.Map.of(),
                                    java.util.Map.of("source", "hub", "feature", "kits"), "hub", "simple"));
                } catch (IllegalStateException ignored) {}
            });
        }
        if (configuration.moduleHomes() && has(api, "essentials_hub_home")) {
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
        if (configuration.moduleTpa() && has(api, "essentials_hub_tpa")) {
            form.button(configuration.hubButtonTpa(), formPlayer -> {
                try {
                    api.executeActionString(formPlayer, "essentials_hub_tpa:",
                            api.createActionContext(java.util.Map.of(), java.util.Map.of(),
                                    java.util.Map.of("source", "hub", "feature", "tpa"), "hub", "simple"));
                } catch (IllegalStateException ignored) {}
            });
        }
        if (configuration.moduleShopGuiPlus() && has(api, "shopgui_main")) {
            form.button(configuration.hubButtonShopGuiPlus(), formPlayer -> {
                try {
                    api.executeActionString(formPlayer, "shopgui_main:",
                            api.createActionContext(java.util.Map.of(), java.util.Map.of(),
                                    java.util.Map.of("source", "hub", "feature", "shopgui"), "hub", "simple"));
                } catch (IllegalStateException ignored) {}
            });
        }
        if (configuration.moduleEconomyShopGui() && has(api, "economyshop_main")) {
            form.button(configuration.hubButtonEconomyShopGui(), formPlayer -> {
                try {
                    api.executeActionString(formPlayer, "economyshop_main:",
                            api.createActionContext(java.util.Map.of(), java.util.Map.of(),
                                    java.util.Map.of("source", "hub", "feature", "economyshop"), "hub", "simple"));
                } catch (IllegalStateException ignored) {}
            });
        }

        if ((configuration.moduleMyPet() || configuration.moduleMcPets()) && has(api, "essentials_pet_hub")) {
            form.button(configuration.hubButtonMyPet(), formPlayer -> {
                try {
                    api.executeActionString(formPlayer, "essentials_pet_hub:",
                            api.createActionContext(java.util.Map.of(), java.util.Map.of(),
                                    java.util.Map.of("source", "hub", "feature", "pets"), "hub", "simple"));
                } catch (IllegalStateException ignored) {}
            });
        }

        form.send(new PaperPlayer(player));
        if (!openedByButton) {
            soundFeedback.playFormOpen(player);
        }
    }

    private static boolean has(BedrockGUIApi api, String actionType) {
        return api.getActionRegistry().hasHandler(actionType);
    }
}
