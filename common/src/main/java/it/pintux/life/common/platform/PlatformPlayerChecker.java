package it.pintux.life.common.platform;

import java.util.UUID;


public interface PlatformPlayerChecker {


    boolean isBedrockPlayer(UUID playerUuid);


    boolean isJavaPlayer(UUID playerUuid);


    boolean isFloodgateAvailable();


    String getPlayerPlatform(UUID playerUuid);
}

