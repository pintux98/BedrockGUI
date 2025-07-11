package it.pintux.life.paper.platform;

import it.pintux.life.common.platform.PlatformSoundManager;
import it.pintux.life.common.utils.FormPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Paper implementation of PlatformSoundManager using Bukkit API.
 */
public class PaperSoundManager implements PlatformSoundManager {
    
    @Override
    public boolean playSound(FormPlayer player, String soundName, float volume, float pitch) {
        try {
            Player bukkitPlayer = Bukkit.getPlayer(player.getUniqueId());
            if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
                return false;
            }
            
            Sound sound = parseSound(soundName);
            if (sound == null) {
                return false;
            }
            
            bukkitPlayer.playSound(bukkitPlayer.getLocation(), sound, volume, pitch);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean stopAllSounds(FormPlayer player) {
        try {
            Player bukkitPlayer = Bukkit.getPlayer(player.getUniqueId());
            if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
                return false;
            }
            
            bukkitPlayer.stopAllSounds();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean soundExists(String soundName) {
        return parseSound(soundName) != null;
    }
    
    /**
     * Parse a sound name to a Bukkit Sound enum.
     * 
     * @param soundName The sound name to parse
     * @return The Sound enum, or null if not found
     */
    private Sound parseSound(String soundName) {
        try {
            // Try to parse as enum name first
            return Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Try common sound name mappings
            String normalizedName = soundName.toUpperCase().replace(".", "_");
            
            try {
                return Sound.valueOf(normalizedName);
            } catch (IllegalArgumentException e2) {
                // Try with ENTITY_ prefix for entity sounds
                if (!normalizedName.startsWith("ENTITY_")) {
                    try {
                        return Sound.valueOf("ENTITY_" + normalizedName);
                    } catch (IllegalArgumentException e3) {
                        // Try with BLOCK_ prefix for block sounds
                        if (!normalizedName.startsWith("BLOCK_")) {
                            try {
                                return Sound.valueOf("BLOCK_" + normalizedName);
                            } catch (IllegalArgumentException e4) {
                                // Try with UI_ prefix for UI sounds
                                if (!normalizedName.startsWith("UI_")) {
                                    try {
                                        return Sound.valueOf("UI_" + normalizedName);
                                    } catch (IllegalArgumentException e5) {
                                        return null;
                                    }
                                }
                            }
                        }
                    }
                }
                return null;
            }
        }
    }
}