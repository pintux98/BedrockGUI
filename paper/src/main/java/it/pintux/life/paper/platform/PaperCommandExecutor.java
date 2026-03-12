package it.pintux.life.paper.platform;

import it.pintux.life.common.platform.PlatformCommandExecutor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;


public class PaperCommandExecutor implements PlatformCommandExecutor {

    @Override
    public boolean executeAsConsole(String command) {
        try {
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean executeAsPlayer(String playerName, String command) {
        try {
            Player player = Bukkit.getPlayerExact(playerName);
            if (player != null && player.isOnline()) {
                return Bukkit.dispatchCommand(player, command);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
