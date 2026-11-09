package it.pintux.life.common.platform;

import it.pintux.life.common.utils.FormPlayer;


public interface PlatformPlayerManager {

    Object getPlayer(String playerName);

    java.util.List<String> getOnlinePlayerNames();

    boolean broadcast(FormPlayer source, String audience, String target, String message);

    void sendMessage(String playerName, String message);


    void sendMessage(Object player, String message);


    boolean sendByteArray(FormPlayer player, String channel, byte[] data);
}

