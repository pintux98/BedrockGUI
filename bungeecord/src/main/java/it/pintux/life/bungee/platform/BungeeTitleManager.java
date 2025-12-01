package it.pintux.life.bungee.platform;

import it.pintux.life.common.platform.PlatformTitleManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.bungee.utils.BungeePlayer;

public class BungeeTitleManager implements PlatformTitleManager {
    @Override
    public boolean sendTitle(FormPlayer player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (!(player instanceof BungeePlayer)) return false;
        return true;
    }

    @Override
    public boolean sendActionBar(FormPlayer player, String message) {
        if (!(player instanceof BungeePlayer)) return false;
        return true;
    }

    @Override
    public void clearTitle(FormPlayer player) {
    }

    @Override
    public void resetTitle(FormPlayer player) {
    }

    @Override
    public boolean isSupported() {
        return true;
    }
}