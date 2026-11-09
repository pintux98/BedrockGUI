package it.pintux.life.velocity.platform;

import com.velocitypowered.api.proxy.ProxyServer;
import it.pintux.life.common.platform.PlatformScheduler;

import java.util.concurrent.TimeUnit;

public class VelocityScheduler implements PlatformScheduler {

    private final ProxyServer proxyServer;
    private final Object plugin;

    public VelocityScheduler(ProxyServer proxyServer, Object plugin) {
        this.proxyServer = proxyServer;
        this.plugin = plugin;
    }


    @Override
    public void runLaterSync(long delayMillis, Runnable task) {
        proxyServer.getScheduler()
                .buildTask(plugin, task)
                .delay(delayMillis, TimeUnit.MILLISECONDS)
                .schedule();
    }
}

