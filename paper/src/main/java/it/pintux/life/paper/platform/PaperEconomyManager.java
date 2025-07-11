package it.pintux.life.paper.platform;

import it.pintux.life.common.platform.PlatformEconomyManager;
import it.pintux.life.common.utils.FormPlayer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Paper implementation of PlatformEconomyManager using Vault API.
 */
public class PaperEconomyManager implements PlatformEconomyManager {
    
    private Economy economy;
    
    public PaperEconomyManager() {
        setupEconomy();
    }
    
    private void setupEconomy() {
        try {
            if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
                return;
            }
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                return;
            }
            economy = rsp.getProvider();
        } catch (Exception e) {
            economy = null;
        }
    }
    
    @Override
    public boolean isEconomyAvailable() {
        return economy != null;
    }
    
    @Override
    public double getBalance(FormPlayer player) {
        if (!isEconomyAvailable()) {
            return 0.0;
        }
        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player.getUniqueId());
            return economy.getBalance(offlinePlayer);
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    @Override
    public boolean addMoney(FormPlayer player, double amount) {
        if (!isEconomyAvailable() || amount <= 0) {
            return false;
        }
        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player.getUniqueId());
            return economy.depositPlayer(offlinePlayer, amount).transactionSuccess();
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean removeMoney(FormPlayer player, double amount) {
        if (!isEconomyAvailable() || amount <= 0) {
            return false;
        }
        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player.getUniqueId());
            return economy.withdrawPlayer(offlinePlayer, amount).transactionSuccess();
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean hasEnoughMoney(FormPlayer player, double amount) {
        if (!isEconomyAvailable()) {
            return false;
        }
        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player.getUniqueId());
            return economy.has(offlinePlayer, amount);
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean setBalance(FormPlayer player, double amount) {
        if (!isEconomyAvailable() || amount < 0) {
            return false;
        }
        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player.getUniqueId());
            double currentBalance = economy.getBalance(offlinePlayer);
            if (currentBalance > amount) {
                return economy.withdrawPlayer(offlinePlayer, currentBalance - amount).transactionSuccess();
            } else if (currentBalance < amount) {
                return economy.depositPlayer(offlinePlayer, amount - currentBalance).transactionSuccess();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String getCurrencySymbol() {
        if (!isEconomyAvailable()) {
            return "$";
        }
        try {
            return economy.currencyNameSingular();
        } catch (Exception e) {
            return "$";
        }
    }
    
    @Override
    public String formatMoney(double amount) {
        if (!isEconomyAvailable()) {
            return String.format("%.2f", amount);
        }
        try {
            return economy.format(amount);
        } catch (Exception e) {
            return String.format("%.2f", amount);
        }
    }
}