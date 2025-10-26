package it.pintux.life.common.platform;

import it.pintux.life.common.utils.FormPlayer;


public interface PlatformPlayerManager {


    Object getPlayer(String playerName);


    Object getOfflinePlayer(String playerName);


    boolean isPlayerOnline(String playerName);


    void sendMessage(String playerName, String message);


    void sendMessage(Object player, String message);


    String getPlayerName(Object player);


    Object getPlayerWorld(Object player);


    Object getPlayerLocation(Object player);


    FormPlayer toFormPlayer(Object player);


    Object fromFormPlayer(FormPlayer formPlayer);


    String getWorldName(Object world);
}

