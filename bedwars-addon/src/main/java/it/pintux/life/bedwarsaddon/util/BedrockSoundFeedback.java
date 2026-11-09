package it.pintux.life.bedwarsaddon.util;

import it.pintux.life.paper.platform.PaperSoundManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class BedrockSoundFeedback {

    private boolean enabled = true;
    private Sound formOpenSound = Sound.UI_BUTTON_CLICK;
    private float defaultVolume = 1.0f;
    private float defaultPitch = 1.0f;

    public void configure(boolean enabled, String formOpen, float volume, float pitch) {
        this.enabled = enabled;
        this.formOpenSound = PaperSoundManager.resolve(formOpen, Sound.UI_BUTTON_CLICK);
        this.defaultVolume = volume;
        this.defaultPitch = pitch;
    }

    public void playFormOpen(Player player) {
        if (!enabled || player == null || !player.isOnline()) return;
        try {
            player.playSound(player.getLocation(), formOpenSound, defaultVolume, defaultPitch);
        } catch (Exception ignored) {
        }
    }
}
