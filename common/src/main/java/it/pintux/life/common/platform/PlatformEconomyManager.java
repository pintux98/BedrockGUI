package it.pintux.life.common.platform;

import it.pintux.life.common.utils.FormPlayer;

import java.math.BigDecimal;


public interface PlatformEconomyManager {
    
    
    boolean isEconomyAvailable();
    
    
    BigDecimal getBalance(FormPlayer player);
    
    
    boolean addMoney(FormPlayer player, BigDecimal amount);
    
    
    boolean removeMoney(FormPlayer player, BigDecimal amount);
    
    
    boolean hasEnoughMoney(FormPlayer player, BigDecimal amount);
    
    
    boolean setBalance(FormPlayer player, BigDecimal amount);
    
    
    String getCurrencySymbol();
    
    
    String formatMoney(BigDecimal amount);
}
