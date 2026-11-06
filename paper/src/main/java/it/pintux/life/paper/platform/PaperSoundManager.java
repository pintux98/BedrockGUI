package it.pintux.life.paper.platform;

import it.pintux.life.common.platform.PlatformSoundManager;
import it.pintux.life.common.utils.FormPlayer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class PaperSoundManager implements PlatformSoundManager {

    private static final Map<String, String> LEGACY_NAMES = Map.ofEntries(
            Map.entry("RANDOM_LEVELUP", "ENTITY_PLAYER_LEVELUP"),
            Map.entry("RANDOM_CLICK", "UI_BUTTON_CLICK"),
            Map.entry("RANDOM_POP", "ENTITY_ITEM_PICKUP"),
            Map.entry("RANDOM_ORB", "ENTITY_EXPERIENCE_ORB_PICKUP"),
            Map.entry("MOB_VILLAGER_YES", "ENTITY_VILLAGER_YES"),
            Map.entry("MOB_VILLAGER_NO", "ENTITY_VILLAGER_NO"),
            Map.entry("NOTE_PLING", "BLOCK_NOTE_BLOCK_PLING"),
            Map.entry("NOTE_HARP", "BLOCK_NOTE_BLOCK_HARP"),
            Map.entry("RANDOM_BREAK", "ENTITY_ITEM_BREAK"),
            Map.entry("DIG_STONE", "BLOCK_STONE_BREAK"),
            Map.entry("STEP_STONE", "BLOCK_STONE_STEP")
    );
    private static final String[] PREFIXES = {"", "ENTITY_", "BLOCK_", "UI_"};

    private static volatile Map<String, Sound> soundsByName;

    @Override
    public boolean playSound(FormPlayer player, String soundName, float volume, float pitch) {
        try {
            Player bukkitPlayer = Bukkit.getPlayer(player.getUniqueId());
            if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
                return false;
            }

            Sound sound = resolve(soundName);
            if (sound == null) {
                return false;
            }

            bukkitPlayer.playSound(bukkitPlayer.getLocation(), sound, volume, pitch);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static Sound resolve(String soundName) {
        if (soundName == null || soundName.isBlank()) {
            return null;
        }
        NamespacedKey key = NamespacedKey.fromString(soundName.trim().toLowerCase(Locale.ROOT));
        Sound direct = key != null ? Registry.SOUNDS.get(key) : null;
        if (direct != null) {
            return direct;
        }
        Map<String, Sound> sounds = soundsByName();
        String name = normalize(soundName);
        name = LEGACY_NAMES.getOrDefault(name, name);
        for (String prefix : PREFIXES) {
            Sound sound = sounds.get(name.startsWith(prefix) ? name : prefix + name);
            if (sound != null) {
                return sound;
            }
        }
        return null;
    }

    public static Sound resolve(String soundName, Sound fallback) {
        Sound sound = resolve(soundName);
        return sound != null ? sound : fallback;
    }

    private static Map<String, Sound> soundsByName() {
        Map<String, Sound> sounds = soundsByName;
        if (sounds == null) {
            sounds = new HashMap<>();
            for (Sound sound : Registry.SOUNDS) {
                sounds.put(normalize(sound.getKey().getKey()), sound);
            }
            soundsByName = sounds;
        }
        return sounds;
    }

    private static String normalize(String name) {
        String normalized = name.trim().toUpperCase(Locale.ROOT);
        int colon = normalized.indexOf(':');
        if (colon >= 0) {
            normalized = normalized.substring(colon + 1);
        }
        return normalized.replace('.', '_');
    }
}
