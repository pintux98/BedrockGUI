package it.pintux.life.velocity.platform;

import com.velocitypowered.api.proxy.Player;
import it.pintux.life.common.platform.PlatformTitleManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.velocity.utils.VelocityPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

import java.time.Duration;

public class VelocityTitleManager implements PlatformTitleManager {

    @Override
    public boolean sendTitle(FormPlayer player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (!(player instanceof VelocityPlayer velocityPlayer)) {
            return false;
        }

        if (!velocityPlayer.isOnline()) {
            return false;
        }

        Player p = velocityPlayer.getPlayer();

        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeIn * 50),
                Duration.ofMillis(stay * 50),
                Duration.ofMillis(fadeOut * 50)
        );

        Title adventureTitle = Title.title(
                Component.text(title),
                Component.text(subtitle),
                times
        );

        p.showTitle(adventureTitle);

        return true;
    }

    @Override
    public boolean sendActionBar(FormPlayer player, String message) {
        if (!(player instanceof VelocityPlayer)) {
            return false;
        }

        VelocityPlayer velocityPlayer = (VelocityPlayer) player;
        if (!velocityPlayer.isOnline()) {
            return false;
        }

        velocityPlayer.getPlayer().sendActionBar(Component.text(message));

        return true;
    }

    @Override
    public boolean isSupported() {
        return true;
    }
}