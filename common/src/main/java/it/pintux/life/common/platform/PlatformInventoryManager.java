package it.pintux.life.common.platform;

import it.pintux.life.common.utils.FormPlayer;

public interface PlatformInventoryManager {

    int countItem(FormPlayer player, String item);

    boolean removeItem(FormPlayer player, String item, int amount);
}
