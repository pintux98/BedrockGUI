package it.pintux.life.paper.platform;

import it.pintux.life.common.platform.PlatformScheduler;
import it.pintux.life.paper.utils.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class PaperScheduler implements PlatformScheduler {
    private final Plugin plugin;

    public PaperScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runLaterSync(long delayMillis, Runnable task) {
        long ticks = Math.max(1, delayMillis / 50);
        SchedulerAdapter.runSyncLater(plugin, task, ticks);
    }
}

