package it.pintux.life.common.platform;

import it.pintux.life.common.utils.FormPlayer;

import java.math.BigDecimal;

/**
 * Platform abstraction for economy operations.
 * This interface allows the common module to handle economy
 * without depending on platform-specific economy plugins.
 */
public interface PlatformEconomyManager {
    
    /**
     * Check if economy is available on this platform.
     * 
     * @return true if economy is available, false otherwise
     */
    boolean isEconomyAvailable();
    
    /**
     * Get the balance of a player.
     * 
     * @param player The player to get the balance for
     * @return The player's balance, or 0.0 if economy is not available
     */
    BigDecimal getBalance(FormPlayer player);
    
    /**
     * Add money to a player's account.
     * 
     * @param player The player to add money to
     * @param amount The amount to add
     * @return true if the operation was successful, false otherwise
     */
    boolean addMoney(FormPlayer player, BigDecimal amount);
    
    /**
     * Remove money from a player's account.
     * 
     * @param player The player to remove money from
     * @param amount The amount to remove
     * @return true if the operation was successful, false otherwise
     */
    boolean removeMoney(FormPlayer player, BigDecimal amount);
    
    /**
     * Check if a player has enough money.
     * 
     * @param player The player to check
     * @param amount The amount to check for
     * @return true if the player has enough money, false otherwise
     */
    boolean hasEnoughMoney(FormPlayer player, BigDecimal amount);
    
    /**
     * Set a player's balance.
     * 
     * @param player The player to set the balance for
     * @param amount The new balance
     * @return true if the operation was successful, false otherwise
     */
    boolean setBalance(FormPlayer player, BigDecimal amount);
    
    /**
     * Get the currency symbol or name.
     * 
     * @return The currency symbol (e.g., "$", "coins", "gems")
     */
    String getCurrencySymbol();
    
    /**
     * Format a money amount according to the economy plugin's formatting.
     * 
     * @param amount The amount to format
     * @return The formatted amount string
     */
    String formatMoney(BigDecimal amount);
}