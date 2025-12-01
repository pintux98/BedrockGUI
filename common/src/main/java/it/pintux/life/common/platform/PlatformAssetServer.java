package it.pintux.life.common.platform;

public interface PlatformAssetServer {
    boolean isAvailable();
    String getAssetUrl(String filename);
    void shutdown();
}