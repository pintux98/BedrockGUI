package it.pintux.life.common.platform;

public interface PlatformScheduler {
    void runLaterSync(long delayMillis, Runnable task);
}

