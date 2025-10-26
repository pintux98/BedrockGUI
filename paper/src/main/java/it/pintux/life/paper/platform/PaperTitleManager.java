package it.pintux.life.paper.platform;

import it.pintux.life.common.platform.PlatformTitleManager;
import it.pintux.life.common.utils.FormPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;


public class PaperTitleManager implements PlatformTitleManager {
    @Override
    public boolean sendTitle(FormPlayer player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        try {
            Player bukkitPlayer = Bukkit.getPlayer(player.getUniqueId());
            if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
                return false;
            }
            String t = title != null ? translate(title) : null;
            String st = subtitle != null ? translate(subtitle) : null;
            bukkitPlayer.sendTitle(t, st, Math.max(0, fadeIn), Math.max(0, stay), Math.max(0, fadeOut));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean sendActionBar(FormPlayer player, String message) {
        try {
            Player bukkitPlayer = Bukkit.getPlayer(player.getUniqueId());
            if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
                return false;
            }
            String msg = translate(message);

            bukkitPlayer.sendActionBar(msg);
            return true;
        } catch (NoSuchMethodError err) {

            try {
                Player bukkitPlayer = Bukkit.getPlayer(player.getUniqueId());
                if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
                    return false;
                }
                bukkitPlayer.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(translate(message)));
                return true;
            } catch (Throwable t) {
                return false;
            }
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void clearTitle(FormPlayer player) {
        Player bukkitPlayer = Bukkit.getPlayer(player.getUniqueId());
        if (bukkitPlayer != null && bukkitPlayer.isOnline()) {
            bukkitPlayer.resetTitle();
        }
    }

    @Override
    public void resetTitle(FormPlayer player) {
        Player bukkitPlayer = Bukkit.getPlayer(player.getUniqueId());
        if (bukkitPlayer != null && bukkitPlayer.isOnline()) {
            bukkitPlayer.resetTitle();
        }
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    private String translate(String msg) {
        if (msg == null) return null;
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', msg);
    }
}
