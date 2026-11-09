package it.pintux.life.velocity.platform;

import it.pintux.life.common.platform.PlatformSoundManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.velocity.utils.VelocityPlayer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

import java.util.Locale;

public class VelocitySoundManager implements PlatformSoundManager {

    @Override
    public boolean playSound(FormPlayer player, String soundName, float volume, float pitch) {
        if (!(player instanceof VelocityPlayer velocityPlayer) || !velocityPlayer.isOnline() || soundName == null) {
            return false;
        }
        String key = soundName.trim().toLowerCase(Locale.ROOT);
        if (!Key.parseable(key)) {
            return false;
        }
        velocityPlayer.getPlayer().playSound(Sound.sound(Key.key(key), Sound.Source.MASTER, volume, pitch), Sound.Emitter.self());
        return true;
    }
}
