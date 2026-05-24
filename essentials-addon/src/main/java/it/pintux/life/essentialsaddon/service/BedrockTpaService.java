package it.pintux.life.essentialsaddon.service;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.essentialsaddon.api.BedrockPlayerDetector;
import it.pintux.life.essentialsaddon.config.EssentialsAddonConfiguration;
import it.pintux.life.essentialsaddon.util.BukkitFormPlayer;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public final class BedrockTpaService {
    private final EssentialsAddonConfiguration configuration;
    private final TpaCatalogService tpaCatalog;
    private final BedrockPlayerDetector bedrockPlayerDetector;

    public BedrockTpaService(
            EssentialsAddonConfiguration configuration,
            TpaCatalogService tpaCatalog,
            BedrockPlayerDetector bedrockPlayerDetector
    ) {
        this.configuration = configuration;
        this.tpaCatalog = tpaCatalog;
        this.bedrockPlayerDetector = bedrockPlayerDetector;
    }

    public boolean shouldHandle(Player player) {
        return player != null && bedrockPlayerDetector.isBedrockPlayer(player);
    }

    public void openTpaMenu(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!ensureTpaCatalog(player)) return;

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(configuration.tpaTitle());
        form.content(configuration.tpaContent());

        form.button(configuration.tpaAcceptButton(), formPlayer -> {
            Player bukkitPlayer = FormPlayerResolver.resolve(formPlayer);
            if (bukkitPlayer != null) {
                if (tpaCatalog.hasPendingRequest(bukkitPlayer)) {
                    tpaCatalog.acceptTpa(bukkitPlayer);
                } else {
                    bukkitPlayer.sendMessage(configuration.tpaNoPending());
                }
            }
        });

        form.button(configuration.tpaDenyButton(), formPlayer -> {
            Player bukkitPlayer = FormPlayerResolver.resolve(formPlayer);
            if (bukkitPlayer != null) {
                if (tpaCatalog.hasPendingRequest(bukkitPlayer)) {
                    tpaCatalog.denyTpa(bukkitPlayer);
                } else {
                    bukkitPlayer.sendMessage(configuration.tpaNoPending());
                }
            }
        });

        form.button(configuration.tpaSendButton(), formPlayer -> {
            Player bukkitPlayer = FormPlayerResolver.resolve(formPlayer);
            if (bukkitPlayer != null) {
                showTpaTargetForm(bukkitPlayer, false);
            }
        });

        form.button(configuration.tpaHereButton(), formPlayer -> {
            Player bukkitPlayer = FormPlayerResolver.resolve(formPlayer);
            if (bukkitPlayer != null) {
                showTpaTargetForm(bukkitPlayer, true);
            }
        });

        form.button(configuration.tpaCancelButton(), formPlayer -> {
            Player bukkitPlayer = FormPlayerResolver.resolve(formPlayer);
            if (bukkitPlayer != null) {
                tpaCatalog.cancelTpa(bukkitPlayer);
            }
        });

        if (tpaCatalog.hasPendingRequest(player)) {
            List<String> requests = tpaCatalog.getPendingRequests(player);
            form.content(configuration.render(configuration.tpaPendingContent(),
                    Map.of("players", String.join(", ", requests))));
        }

        form.send(new BukkitFormPlayer(player));
    }

    public void showTpaTargetForm(Player player, boolean here) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;

        String title = here ? configuration.tpaTitleHere() : configuration.tpaTitleSend();
        BedrockGUIApi.CustomFormBuilder form = api.createCustomForm(title);
        form.content(configuration.tpaSendContent());
        form.input(
                configuration.tpaPlayerInputText(),
                configuration.tpaPlayerInputPlaceholder(),
                ""
        );

        form.onSubmit(results -> {
            String targetName = results.values().stream()
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .findFirst()
                    .orElse(null);
            if (targetName == null) return;

            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                player.sendMessage(configuration.tpaSendFailed());
                return;
            }

            boolean success = here
                    ? tpaCatalog.sendTpahere(player, targetName)
                    : tpaCatalog.sendTpa(player, targetName);
            if (!success) {
                player.sendMessage(configuration.tpaSendFailed());
            }
        });

        form.send(new BukkitFormPlayer(player));
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

    private boolean ensureTpaCatalog(Player player) {
        if (!tpaCatalog.isReady()) {
            tpaCatalog.refresh();
        }
        if (!tpaCatalog.isReady()) {
            player.sendMessage(configuration.tpaProviderUnavailable());
            return false;
        }
        return true;
    }
}
