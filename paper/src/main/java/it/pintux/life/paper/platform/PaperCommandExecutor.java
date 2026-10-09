package it.pintux.life.paper.platform;

import it.pintux.life.common.platform.PlatformCommandExecutor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PaperCommandExecutor implements PlatformCommandExecutor {

    private static final boolean FOLIA = detectFolia();

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Plugin owningPlugin() {
        Plugin p = Bukkit.getPluginManager().getPlugin("BedrockGUI-Paper");
        if (p == null) {
            p = Bukkit.getPluginManager().getPlugin("BedrockGUI");
        }
        return p;
    }

    @Override
    public boolean executeAsConsole(String command) {
        try {
            Plugin plugin = owningPlugin();
            // On Folia, console command dispatch must run on the global region thread.
            // Calling Bukkit.dispatchCommand directly from a region thread trips
            // RegionizedServer.ensureGlobalTickThread() and the command is aborted.
            if (FOLIA && plugin != null) {
                Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
                    try {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    } catch (Exception ignored) {
                    }
                });
                return true;
            }
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean executeAsPlayer(String playerName, String command) {
        try {
            Player player = Bukkit.getPlayerExact(playerName);
            if (player == null || !player.isOnline()) {
                return false;
            }
            Plugin plugin = owningPlugin();
            // On Folia, a player command must run on that player's region thread.
            if (FOLIA && plugin != null) {
                player.getScheduler().execute(plugin, () -> {
                    try {
                        Bukkit.dispatchCommand(player, command);
                    } catch (Exception ignored) {
                    }
                }, null, 1L);
                return true;
            }
            return Bukkit.dispatchCommand(player, command);
        } catch (Exception e) {
            return false;
        }
    }
}
