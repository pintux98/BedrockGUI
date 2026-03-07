package it.pintux.life.common.platform;

import it.pintux.life.common.utils.FormPlayer;


public interface PlatformSoundManager {
    boolean playSound(FormPlayer player, String soundName, float volume, float pitch);
}

