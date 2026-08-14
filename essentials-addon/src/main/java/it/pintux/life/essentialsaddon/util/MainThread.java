package it.pintux.life.essentialsaddon.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

/**
 * Floodgate hands form responses to us on a Netty thread, so every button callback in this
 * addon starts off the main server thread. Bukkit mutations made from there are rejected by
 * Paper ("Asynchronous entity teleport!") and silently swallowed by the providers' catch
 * blocks, which is why a warp button used to do nothing. Bounce that work back on-thread.
 */
public final class MainThread {
    private static final boolean FOLIA = detectFolia();
    private static volatile Plugin plugin;

    private MainThread() {
    }

    public static void init(Plugin owner) {
        plugin = owner;
    }

    /** Runs now when already on the main thread, otherwise on the next tick. */
    public static void run(Runnable task) {
        Plugin owner = plugin;
        if (owner == null || !owner.isEnabled()) {
            task.run();
            return;
        }
        if (!FOLIA && Bukkit.isPrimaryThread()) {
            task.run();
            return;
        }
        if (FOLIA && runOnFolia(owner, task, 0L)) {
            return;
        }
        Bukkit.getScheduler().runTask(owner, task);
    }

    /** Always defers, even when called from the main thread. */
    public static void runLater(Runnable task, long delayTicks) {
        Plugin owner = plugin;
        long delay = Math.max(1L, delayTicks);
        if (owner == null || !owner.isEnabled()) {
            task.run();
            return;
        }
        if (FOLIA && runOnFolia(owner, task, delay)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(owner, task, delay);
    }

    private static boolean runOnFolia(Plugin owner, Runnable task, long delayTicks) {
        try {
            Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
            Consumer<Object> consumer = ignored -> task.run();
            if (delayTicks <= 0L) {
                scheduler.getClass().getMethod("run", Plugin.class, Consumer.class)
                        .invoke(scheduler, owner, consumer);
            } else {
                scheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, long.class)
                        .invoke(scheduler, owner, consumer, delayTicks);
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (Throwable ignored) {
        }
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            return true;
        } catch (Throwable ignored) {
        }
        return false;
    }
}
