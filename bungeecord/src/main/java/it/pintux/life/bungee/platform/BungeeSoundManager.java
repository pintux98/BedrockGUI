package it.pintux.life.bungee.platform;

import it.pintux.life.common.platform.PlatformSoundManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.bungee.utils.BungeePlayer;

public class BungeeSoundManager implements PlatformSoundManager {
    @Override
    public boolean playSound(FormPlayer player, String soundName, float volume, float pitch) {
        if (!(player instanceof BungeePlayer)) return false;
        return true;
    }
    @Override
    public boolean stopAllSounds(FormPlayer player) { return true; }
    @Override
    public boolean soundExists(String soundName) { return true; }
}