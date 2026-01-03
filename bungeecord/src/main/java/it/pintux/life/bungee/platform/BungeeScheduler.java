package it.pintux.life.bungee.platform;

import it.pintux.life.common.platform.PlatformScheduler;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public class BungeeScheduler implements PlatformScheduler {
    private final Plugin plugin;

    public BungeeScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runLaterSync(long delayMillis, Runnable task) {
        ProxyServer.getInstance().getScheduler().schedule(plugin, task, delayMillis, TimeUnit.MILLISECONDS);
    }
}

