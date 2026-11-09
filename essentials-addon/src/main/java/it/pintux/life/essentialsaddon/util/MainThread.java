package it.pintux.life.essentialsaddon.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class MainThread {
    private static final boolean FOLIA = detectFolia();
    private static volatile Plugin plugin;

    private MainThread() {
    }

    public static void init(Plugin owner) {
        plugin = owner;
    }

    public static void run(Runnable task) {
        Plugin owner = plugin;
        if (owner == null || !owner.isEnabled() || (!FOLIA && Bukkit.isPrimaryThread())) {
            task.run();
            return;
        }
        if (FOLIA) {
            Bukkit.getGlobalRegionScheduler().run(owner, ignored -> task.run());
            return;
        }
        Bukkit.getScheduler().runTask(owner, task);
    }

    public static void runLater(Runnable task, long delayTicks) {
        Plugin owner = plugin;
        long delay = Math.max(1L, delayTicks);
        if (owner == null || !owner.isEnabled()) {
            task.run();
            return;
        }
        if (FOLIA) {
            Bukkit.getGlobalRegionScheduler().runDelayed(owner, ignored -> task.run(), delay);
            return;
        }
        Bukkit.getScheduler().runTaskLater(owner, task, delay);
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
