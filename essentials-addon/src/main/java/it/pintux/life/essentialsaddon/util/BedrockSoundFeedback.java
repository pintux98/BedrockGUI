package it.pintux.life.essentialsaddon.util;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class BedrockSoundFeedback {

    private static final ConcurrentMap<UUID, Boolean> playerSoundEnabled = new ConcurrentHashMap<>();

    private boolean enabled = true;
    private Sound formOpenSound = Sound.UI_BUTTON_CLICK;
    private Sound purchaseSuccessSound = Sound.ENTITY_PLAYER_LEVELUP;
    private Sound purchaseFailedSound = Sound.BLOCK_NOTE_BLOCK_PLING;
    private float defaultVolume = 1.0f;
    private float defaultPitch = 1.0f;

    public void configure(boolean enabled, String formOpen, String purchaseSuccess, String purchaseFailed, float volume, float pitch) {
        this.enabled = enabled;
        this.formOpenSound = parseSound(formOpen, Sound.UI_BUTTON_CLICK);
        this.purchaseSuccessSound = parseSound(purchaseSuccess, Sound.ENTITY_PLAYER_LEVELUP);
        this.purchaseFailedSound = parseSound(purchaseFailed, Sound.BLOCK_NOTE_BLOCK_PLING);
        this.defaultVolume = volume;
        this.defaultPitch = pitch;
    }

    public void setVolume(float volume) {
        this.defaultVolume = volume;
    }

    public void setPitch(float pitch) {
        this.defaultPitch = pitch;
    }

    public void playFormOpen(Player player) {
        play(player, formOpenSound);
    }

    public void playPurchaseSuccess(Player player) {
        play(player, purchaseSuccessSound);
    }

    public void playPurchaseFailed(Player player) {
        play(player, purchaseFailedSound);
    }

    public void setPlayerEnabled(UUID playerId, boolean enabled) {
        playerSoundEnabled.put(playerId, enabled);
    }

    private void play(Player player, Sound sound) {
        if (!enabled || player == null || !player.isOnline()) {
            return;
        }
        if (Boolean.FALSE.equals(playerSoundEnabled.get(player.getUniqueId()))) {
            return;
        }
        try {
            player.playSound(player.getLocation(), sound, defaultVolume, defaultPitch);
        } catch (Exception ignored) {
        }
    }

    private static Sound parseSound(String name, Sound fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        try {
            return Sound.valueOf(name.toUpperCase().replace('.', '_'));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
