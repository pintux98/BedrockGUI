package it.pintux.life.velocity.platform;

import it.pintux.life.common.platform.PlatformEconomyManager;
import it.pintux.life.common.utils.FormPlayer;

import java.math.BigDecimal;

public class VelocityEconomyManager implements PlatformEconomyManager {

    @Override
    public boolean isEconomyAvailable() {
        // Economy is typically not available at the proxy level
        // This would require a proxy-level economy plugin or communication with backend servers
        return false;
    }

    @Override
    public BigDecimal getBalance(FormPlayer player) {
        // Return zero balance as economy is not available on proxy
        return BigDecimal.ZERO;
    }

    @Override
    public boolean addMoney(FormPlayer player, BigDecimal amount) {
        // Cannot add money on proxy level
        return false;
    }

    @Override
    public boolean removeMoney(FormPlayer player, BigDecimal amount) {
        // Cannot remove money on proxy level
        return false;
    }

    @Override
    public boolean hasEnoughMoney(FormPlayer player, BigDecimal amount) {
        // Player has no money on proxy level
        return false;
    }

    @Override
    public boolean setBalance(FormPlayer player, BigDecimal amount) {
        // Cannot set balance on proxy level
        return false;
    }

    @Override
    public String getCurrencySymbol() {
        return "";
    }

    @Override
    public String formatMoney(BigDecimal amount) {
        return amount.toString();
    }
}