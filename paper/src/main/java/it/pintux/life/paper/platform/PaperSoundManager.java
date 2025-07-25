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
        if (soundName == null || soundName.isEmpty()) {
            return null;
        }
        
        // Handle legacy sound names first
        Sound legacySound = parseLegacySound(soundName);
        if (legacySound != null) {
            return legacySound;
        }
        
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
    
    /**
     * Parse legacy sound names to modern Bukkit Sound enums.
     * 
     * @param soundName The legacy sound name
     * @return The Sound enum, or null if not a known legacy sound
     */
    private Sound parseLegacySound(String soundName) {
        String lowerName = soundName.toLowerCase();
        
        // Common legacy sound mappings
        switch (lowerName) {
            case "random.levelup":
            case "entity.player.levelup":
                return Sound.ENTITY_PLAYER_LEVELUP;
            case "random.click":
            case "ui.button.click":
                return Sound.UI_BUTTON_CLICK;
            case "random.pop":
            case "entity.item.pickup":
                return Sound.ENTITY_ITEM_PICKUP;
            case "random.orb":
            case "entity.experience_orb.pickup":
                return Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
            case "mob.villager.yes":
            case "entity.villager.yes":
                return Sound.ENTITY_VILLAGER_YES;
            case "mob.villager.no":
            case "entity.villager.no":
                return Sound.ENTITY_VILLAGER_NO;
            case "note.pling":
            case "block.note_block.pling":
                return Sound.BLOCK_NOTE_BLOCK_PLING;
            case "note.harp":
            case "block.note_block.harp":
                return Sound.BLOCK_NOTE_BLOCK_HARP;
            case "random.break":
            case "entity.item.break":
                return Sound.ENTITY_ITEM_BREAK;
            case "dig.stone":
            case "block.stone.break":
                return Sound.BLOCK_STONE_BREAK;
            case "step.stone":
            case "block.stone.step":
                return Sound.BLOCK_STONE_STEP;
            default:
                return null;
        }
    }
}