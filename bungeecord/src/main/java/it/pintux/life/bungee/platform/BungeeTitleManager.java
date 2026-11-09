package it.pintux.life.bungee.platform;

import it.pintux.life.common.platform.PlatformTitleManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.bungee.utils.BungeePlayer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.Title;

public class BungeeTitleManager implements PlatformTitleManager {
    @Override
    public boolean sendTitle(FormPlayer player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (!(player instanceof BungeePlayer bungeePlayer) || !bungeePlayer.isOnline()) return false;
        Title title1 = ProxyServer.getInstance().createTitle();
        title1.title(BungeePlayer.text(title));
        title1.subTitle(BungeePlayer.text(subtitle));
        title1.fadeIn(fadeIn);
        title1.stay(stay);
        title1.fadeOut(fadeOut);
        bungeePlayer.getPlayer().sendTitle(title1);
        return true;
    }

    @Override
    public boolean sendActionBar(FormPlayer player, String message) {
        if (!(player instanceof BungeePlayer bungeePlayer) || !bungeePlayer.isOnline()) return false;
        bungeePlayer.getPlayer().sendMessage(ChatMessageType.ACTION_BAR, BungeePlayer.text(message));
        return true;
    }

    @Override
    public boolean isSupported() {
        return true;
    }
}
