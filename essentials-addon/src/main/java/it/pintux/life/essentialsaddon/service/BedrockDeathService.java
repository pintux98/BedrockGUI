package it.pintux.life.essentialsaddon.service;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.essentialsaddon.api.BedrockPlayerDetector;
import it.pintux.life.essentialsaddon.config.EssentialsAddonConfiguration;
import it.pintux.life.essentialsaddon.util.BukkitFormPlayer;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import it.pintux.life.essentialsaddon.util.MainThread;
import org.bukkit.entity.Player;

/**
 * Respawn menu for Bedrock players: offers a jump back to the death point or to spawn,
 * so they do not have to type /back or /spawn from a phone keyboard.
 */
public final class BedrockDeathService {
    private final EssentialsAddonConfiguration configuration;
    private final BedrockPlayerDetector bedrockPlayerDetector;

    public BedrockDeathService(
            EssentialsAddonConfiguration configuration,
            BedrockPlayerDetector bedrockPlayerDetector
    ) {
        this.configuration = configuration;
        this.bedrockPlayerDetector = bedrockPlayerDetector;
    }

    public boolean shouldHandle(Player player) {
        return player != null && bedrockPlayerDetector.isBedrockPlayer(player);
    }

    public long formDelayTicks() {
        return configuration.deathFormDelayTicks();
    }

    public void openDeathMenu(Player player) {
        if (player == null || !player.isOnline()) return;

        BedrockGUIApi api = requireApi(player);
        if (api == null) return;

        boolean showBack = configuration.deathShowBack() && hasBackAccess(player);
        boolean showSpawn = configuration.deathShowSpawn();
        if (!showBack && !showSpawn) {
            return;
        }

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(configuration.deathTitle());
        form.content(configuration.deathContent());

        if (showBack) {
            form.button(configuration.deathBackButton(), formPlayer ->
                    dispatch(formPlayer, configuration.deathBackCommand()));
        }
        if (showSpawn) {
            form.button(configuration.deathSpawnButton(), formPlayer ->
                    dispatch(formPlayer, configuration.deathSpawnCommand()));
        }
        form.button(configuration.deathCloseButton(), ignored -> { });

        form.send(new BukkitFormPlayer(player));
    }

    private boolean hasBackAccess(Player player) {
        String permission = configuration.deathBackPermission();
        return permission == null || permission.isBlank() || player.hasPermission(permission);
    }

    private void dispatch(it.pintux.life.common.utils.FormPlayer formPlayer, String command) {
        Player bukkitPlayer = FormPlayerResolver.resolve(formPlayer);
        if (bukkitPlayer == null) return;
        String trimmed = command == null ? "" : command.trim();
        if (trimmed.isEmpty()) return;
        String withoutSlash = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
        // Form callbacks arrive on a Floodgate thread; command dispatch is main-thread only.
        MainThread.run(() -> bukkitPlayer.performCommand(withoutSlash));
    }

    private BedrockGUIApi requireApi(Player player) {
        try {
            BedrockGUIApi api = BedrockGUIApi.getInstance();
            if (api == null) {
                player.sendMessage(configuration.noBedrockGui());
            }
            return api;
        } catch (IllegalStateException e) {
            player.sendMessage(configuration.noBedrockGui());
            return null;
        }
    }
}
