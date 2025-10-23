package it.pintux.life.common.platform;

import it.pintux.life.common.utils.FormPlayer;


public interface PlatformTitleManager {
    
    boolean sendTitle(FormPlayer player, String title, String subtitle, int fadeIn, int stay, int fadeOut);

    
    boolean sendActionBar(FormPlayer player, String message);

    
    void clearTitle(FormPlayer player);

    
    void resetTitle(FormPlayer player);

    
    boolean isSupported();
}
