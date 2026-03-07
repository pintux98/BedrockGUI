package it.pintux.life.common.platform;

import it.pintux.life.common.utils.FormPlayer;


public interface PlatformPlayerManager {

    Object getPlayer(String playerName);

    void sendMessage(String playerName, String message);


    void sendMessage(Object player, String message);


    void sendByteArray(FormPlayer player, String channel, byte[] data);
}

