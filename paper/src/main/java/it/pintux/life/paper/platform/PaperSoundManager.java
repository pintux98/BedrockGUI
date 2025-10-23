package it.pintux.life.paper.platform;

import it.pintux.life.common.platform.PlatformSoundManager;
import it.pintux.life.common.utils.FormPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;


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
    
    
    private Sound parseSound(String soundName) {
        if (soundName == null || soundName.isEmpty()) {
            return null;
        }
        
        
        Sound legacySound = parseLegacySound(soundName);
        if (legacySound != null) {
            return legacySound;
        }
        
        try {
            
            return Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            
            String normalizedName = soundName.toUpperCase().replace(".", "_");
            
            try {
                return Sound.valueOf(normalizedName);
            } catch (IllegalArgumentException e2) {
                
                if (!normalizedName.startsWith("ENTITY_")) {
                    try {
                        return Sound.valueOf("ENTITY_" + normalizedName);
                    } catch (IllegalArgumentException e3) {
                        
                        if (!normalizedName.startsWith("BLOCK_")) {
                            try {
                                return Sound.valueOf("BLOCK_" + normalizedName);
                            } catch (IllegalArgumentException e4) {
                                
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
    
    
    private Sound parseLegacySound(String soundName) {
        String lowerName = soundName.toLowerCase();
        
        
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
