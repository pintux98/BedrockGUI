package it.pintux.life.common.platform;

import it.pintux.life.common.utils.FormPlayer;


public interface PlatformSoundManager {


    boolean playSound(FormPlayer player, String soundName, float volume, float pitch);


    default boolean playSound(FormPlayer player, String soundName) {
        return playSound(player, soundName, 1.0f, 1.0f);
    }


    boolean stopAllSounds(FormPlayer player);


    boolean soundExists(String soundName);
}

