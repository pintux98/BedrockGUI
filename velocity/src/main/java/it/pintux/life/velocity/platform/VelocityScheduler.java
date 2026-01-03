package it.pintux.life.velocity.platform;

import it.pintux.life.common.platform.PlatformScheduler;

public class VelocityScheduler implements PlatformScheduler {
    @Override
    public void runLaterSync(long delayMillis, Runnable task) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMillis);
                task.run();
            } catch (InterruptedException ignored) {
            }
        }, "BedrockGUI-VelocityScheduler").start();
    }
}

