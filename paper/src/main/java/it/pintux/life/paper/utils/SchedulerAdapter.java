package it.pintux.life.paper.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public final class SchedulerAdapter {
    private static final boolean FOLIA = detectFolia();

    private SchedulerAdapter() {}

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static void runSyncLater(Plugin plugin, Runnable task, long delay) {
        if (FOLIA) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduled -> task.run(), Math.max(1L, delay));
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, task, delay);
    }

    public static void runForEntity(Plugin plugin, Entity entity, Runnable task) {
        if (!FOLIA || Bukkit.isOwnedByCurrentRegion(entity)) {
            task.run();
            return;
        }
        entity.getScheduler().execute(plugin, task, null, 1L);
    }
}
