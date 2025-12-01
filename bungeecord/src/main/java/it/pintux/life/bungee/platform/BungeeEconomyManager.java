package it.pintux.life.bungee.platform;

import it.pintux.life.common.platform.PlatformEconomyManager;
import it.pintux.life.common.utils.FormPlayer;

import java.math.BigDecimal;

public class BungeeEconomyManager implements PlatformEconomyManager {
    @Override
    public boolean isEconomyAvailable() { return false; }
    @Override
    public BigDecimal getBalance(FormPlayer player) { return BigDecimal.ZERO; }
    @Override
    public boolean addMoney(FormPlayer player, BigDecimal amount) { return false; }
    @Override
    public boolean removeMoney(FormPlayer player, BigDecimal amount) { return false; }
    @Override
    public boolean hasEnoughMoney(FormPlayer player, BigDecimal amount) { return false; }
    @Override
    public boolean setBalance(FormPlayer player, BigDecimal amount) { return false; }
    @Override
    public String getCurrencySymbol() { return ""; }
    @Override
    public String formatMoney(BigDecimal amount) { return amount.toString(); }
}