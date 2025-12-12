package it.pintux.life.paper.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

public final class SchedulerAdapter {
    private static final boolean FOLIA = detectFolia();

    private SchedulerAdapter() {}

    private static boolean detectFolia() {
        try {
            if (Class.forName("io.papermc.paper.threadedregions.RegionizedServer") != null) {
                return true;
            }
        } catch (Throwable ignored) {}
        try {
            if (Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler") != null) {
                return true;
            }
        } catch (Throwable ignored) {}
        try {
            return Bukkit.class.getMethod("getGlobalRegionScheduler") != null;
        } catch (Throwable ignored) {}
        return false;
        }

    public static void runSync(Plugin plugin, Runnable task) {
        if (FOLIA) {
            try {
                Object grs = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Consumer<Object> consumer = st -> task.run();
                grs.getClass().getMethod("run", Plugin.class, Consumer.class).invoke(grs, plugin, consumer);
                return;
            } catch (Throwable ignored) {}
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    public static void runAsync(Plugin plugin, Runnable task) {
        if (FOLIA) {
            try {
                Object as = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                Consumer<Object> consumer = st -> task.run();
                as.getClass().getMethod("runNow", Plugin.class, Consumer.class).invoke(as, plugin, consumer);
                return;
            } catch (Throwable ignored) {}
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }
}
