package it.pintux.life.bungee.platform;

import it.pintux.life.common.platform.PlatformTitleManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.bungee.utils.BungeePlayer;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public class BungeeTitleManager implements PlatformTitleManager {
    @Override
    public boolean sendTitle(FormPlayer player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (!(player instanceof BungeePlayer bungeePlayer)) return false;
        Title title1 = ProxyServer.getInstance().createTitle();
        title1.title(TextComponent.fromLegacy(title));
        title1.subTitle(TextComponent.fromLegacy(subtitle));
        title1.fadeIn(fadeIn);
        title1.stay(stay);
        title1.fadeOut(fadeOut);
        ProxyServer.getInstance().getPlayer(bungeePlayer.getUniqueId()).sendTitle(title1);
        return true;
    }

    @Override
    public boolean sendActionBar(FormPlayer player, String message) {
        return false; //Not available ?
    }

    @Override
    public boolean isSupported() {
        return true;
    }
}