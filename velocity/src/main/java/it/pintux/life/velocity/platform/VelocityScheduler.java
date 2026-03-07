package it.pintux.life.velocity.platform;

import com.velocitypowered.api.proxy.ProxyServer;
import it.pintux.life.common.platform.PlatformScheduler;

import java.util.concurrent.TimeUnit;

public class VelocityScheduler implements PlatformScheduler {

    private final ProxyServer proxyServer;

    public VelocityScheduler(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }


    @Override
    public void runLaterSync(long delayMillis, Runnable task) {
        proxyServer.getScheduler()
                .buildTask(this, task)
                .delay(delayMillis, TimeUnit.MILLISECONDS)
                .schedule();
    }
}

