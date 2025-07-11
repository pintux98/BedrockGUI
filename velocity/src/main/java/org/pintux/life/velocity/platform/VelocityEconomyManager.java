package org.pintux.life.velocity.platform;

import it.pintux.life.common.platform.PlatformEconomyManager;
import it.pintux.life.common.utils.FormPlayer;

/**
 * Velocity implementation of PlatformEconomyManager.
 * Note: Velocity doesn't have native economy support, so this is a basic implementation.
 */
public class VelocityEconomyManager implements PlatformEconomyManager {
    
    @Override
    public boolean isEconomyAvailable() {
        // Velocity doesn't have native economy support
        // This could be extended to support proxy-wide economy plugins
        return false;
    }
    
    @Override
    public double getBalance(FormPlayer player) {
        // No economy available on Velocity by default
        return 0.0;
    }
    
    @Override
    public boolean addMoney(FormPlayer player, double amount) {
        // No economy available on Velocity by default
        return false;
    }
    
    @Override
    public boolean removeMoney(FormPlayer player, double amount) {
        // No economy available on Velocity by default
        return false;
    }
    
    @Override
    public boolean hasEnoughMoney(FormPlayer player, double amount) {
        // No economy available on Velocity by default
        return false;
    }
    
    @Override
    public boolean setBalance(FormPlayer player, double amount) {
        // No economy available on Velocity by default
        return false;
    }
    
    @Override
    public String getCurrencySymbol() {
        return "$";
    }
    
    @Override
    public String formatMoney(double amount) {
        return String.format("%.2f", amount);
    }
}