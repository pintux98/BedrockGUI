package it.pintux.life.essentialsaddon.service;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.essentialsaddon.api.BedrockPlayerDetector;
import it.pintux.life.essentialsaddon.config.EssentialsAddonConfiguration;
import it.pintux.life.essentialsaddon.util.BukkitFormPlayer;
import it.pintux.life.essentialsaddon.util.EssentialsActionPayloads;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class BedrockEssentialsService {
    private final Logger logger;
    private final EssentialsAddonConfiguration configuration;
    private final WarpCatalogService warpCatalog;
    private final KitCatalogService kitCatalog;
    private final BedrockPlayerDetector bedrockPlayerDetector;

    public BedrockEssentialsService(
            Logger logger,
            EssentialsAddonConfiguration configuration,
            WarpCatalogService warpCatalog,
            KitCatalogService kitCatalog,
            BedrockPlayerDetector bedrockPlayerDetector
    ) {
        this.logger = logger;
        this.configuration = configuration;
        this.warpCatalog = warpCatalog;
        this.kitCatalog = kitCatalog;
        this.bedrockPlayerDetector = bedrockPlayerDetector;
    }

    public boolean shouldHandle(Player player) {
        return player != null && bedrockPlayerDetector.isBedrockPlayer(player);
    }

    public void openWarpMenu(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!ensureWarpCatalog(player)) return;

        List<String> warps = warpCatalog.getAccessibleWarps(player);
        if (warps.isEmpty()) {
            player.sendMessage(configuration.noWarpsMessage());
            return;
        }

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(configuration.warpTitle());
        form.content(configuration.warpContent());

        for (String warpName : warps) {
            String displayName = warpCatalog.getDisplayName(warpName);
            String buttonText = configuration.render(configuration.warpButton(),
                    Map.of("warp_name", displayName));
            form.button(buttonText, formPlayer ->
                    api.executeActionString(formPlayer,
                            "essentials_warp_teleport:" + EssentialsActionPayloads.encodeWarp(warpName),
                            context("warp-menu", warpName)));
        }

        form.send(new BukkitFormPlayer(player));
        playSound(player, configuration.soundFormOpen());
    }

    public void teleportToWarp(Player player, String warpName) {
        if (!ensureWarpCatalog(player)) return;

        if (!warpCatalog.getAccessibleWarps(player).contains(warpName)) {
            player.sendMessage(configuration.noWarpAccess());
            return;
        }

        boolean success = warpCatalog.getProvider().teleport(player, warpName);
        if (success) {
            player.sendMessage(configuration.render(configuration.teleportSuccess(),
                    Map.of("warp_name", warpCatalog.getDisplayName(warpName))));
            playSound(player, configuration.soundTeleportSuccess());
        } else {
            player.sendMessage(configuration.render(configuration.teleportFailed(),
                    Map.of("reason", "Teleport failed")));
            playSound(player, configuration.soundActionFailed());
        }
    }

    public void openKitMenu(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!ensureKitCatalog(player)) return;

        List<String> kits = kitCatalog.getAccessibleKits(player);
        if (kits.isEmpty()) {
            player.sendMessage(configuration.noKitsMessage());
            return;
        }

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(configuration.kitTitle());
        form.content(configuration.kitContent());

        for (String kitName : kits) {
            String displayName = kitCatalog.getDisplayName(kitName);
            String buttonText = configuration.render(configuration.kitButton(),
                    Map.of("kit_name", displayName));
            form.button(buttonText, formPlayer ->
                    api.executeActionString(formPlayer,
                            "essentials_kit_claim:" + EssentialsActionPayloads.encodeKit(kitName),
                            context("kit-menu", kitName)));
        }

        form.send(new BukkitFormPlayer(player));
        playSound(player, configuration.soundFormOpen());
    }

    public void claimKit(Player player, String kitName) {
        if (!ensureKitCatalog(player)) return;

        if (!kitCatalog.getAccessibleKits(player).contains(kitName)) {
            player.sendMessage(configuration.noKitAccess());
            return;
        }

        if (!kitCatalog.isAvailable(player, kitName)) {
            long cooldown = kitCatalog.getCooldownSeconds(player, kitName);
            String time = formatTime(cooldown);
            player.sendMessage(configuration.render(configuration.kitOnCooldown(),
                    Map.of("time", time)));
            return;
        }

        boolean success = kitCatalog.getProvider().claimKit(player, kitName);
        if (success) {
            player.sendMessage(configuration.kitClaimSuccess());
            playSound(player, configuration.soundKitClaimSuccess());
        } else {
            player.sendMessage(configuration.render(configuration.kitClaimFailed(),
                    Map.of("reason", "Kit claim failed")));
            playSound(player, configuration.soundActionFailed());
        }
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

    private boolean ensureWarpCatalog(Player player) {
        if (!warpCatalog.isReady()) {
            warpCatalog.refresh();
        }
        if (!warpCatalog.isReady()) {
            if (warpCatalog.getProvider() == null) {
                player.sendMessage(configuration.providerUnavailable());
            } else {
                player.sendMessage(configuration.essentialsNotReady());
            }
            return false;
        }
        return true;
    }

    private boolean ensureKitCatalog(Player player) {
        if (!kitCatalog.isReady()) {
            kitCatalog.refresh();
        }
        if (!kitCatalog.isReady()) {
            if (kitCatalog.getProvider() == null) {
                player.sendMessage(configuration.providerUnavailable());
            } else {
                player.sendMessage(configuration.essentialsNotReady());
            }
            return false;
        }
        return true;
    }

    private ActionSystem.ActionContext context(String source, String metadata) {
        return ActionSystem.ActionContext.builder()
                .menuName(source)
                .formType("bedrock-essentials")
                .metadata("feature", metadata)
                .build();
    }

    private String formatTime(long seconds) {
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        if (minutes < 60) return minutes + "m " + remainingSeconds + "s";
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        return hours + "h " + remainingMinutes + "m";
    }

    private void playSound(Player player, String soundName) {
        if (!configuration.soundsEnabled() || player == null || !player.isOnline()) return;
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase().replace('.', '_'));
            player.playSound(player.getLocation(), sound, configuration.soundVolume(), configuration.soundPitch());
        } catch (IllegalArgumentException ignored) {
        }
    }
}
