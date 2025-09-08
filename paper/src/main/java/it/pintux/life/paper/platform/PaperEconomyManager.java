package it.pintux.life.paper.platform;

import it.pintux.life.common.platform.PlatformEconomyManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.paper.BedrockGUI;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.math.BigDecimal;

/**
 * Paper implementation of PlatformEconomyManager using Vault API.
 */
public class PaperEconomyManager implements PlatformEconomyManager {

    private Economy economy;
    private BedrockGUI plugin;

    public PaperEconomyManager(BedrockGUI plugin) {
        this.plugin = plugin;
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
    public BigDecimal getBalance(FormPlayer player) {
        if (!isEconomyAvailable()) {
            return BigDecimal.valueOf(0.0);
        }
        try {
            return BigDecimal.valueOf(economy.getBalance(player.getName()));
        } catch (Exception e) {
            return BigDecimal.valueOf(0.0);
        }
    }

    @Override
    public boolean addMoney(FormPlayer player, BigDecimal amount) {
        if (!isEconomyAvailable() || amount.doubleValue() <= 0) {
            return false;
        }
        try {
            return economy.depositPlayer(player.getName(), amount.doubleValue()).transactionSuccess();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean removeMoney(FormPlayer player, BigDecimal amount) {
        if (!isEconomyAvailable() || amount.doubleValue() <= 0) {
            return false;
        }
        try {
            return economy.withdrawPlayer(player.getName(), amount.doubleValue()).transactionSuccess();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean hasEnoughMoney(FormPlayer player, BigDecimal amount) {
        if (!isEconomyAvailable()) {
            return false;
        }
        try {
            return economy.has(plugin.getName(), amount.doubleValue());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean setBalance(FormPlayer player, BigDecimal amount) {
        if (!isEconomyAvailable() || amount.doubleValue() < 0) {
            return false;
        }
        try {
            BigDecimal currentBalance = BigDecimal.valueOf(economy.getBalance(player.getName()));
            if (currentBalance.doubleValue() > amount.doubleValue()) {
                return removeMoney(player, BigDecimal.valueOf(currentBalance.doubleValue() - amount.doubleValue()));
            } else if (currentBalance.doubleValue() < amount.doubleValue()) {
                return addMoney(player, BigDecimal.valueOf(amount.doubleValue() - currentBalance.doubleValue()));
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
    public String formatMoney(BigDecimal amount) {
        if (!isEconomyAvailable()) {
            return String.format("%.2f", amount);
        }
        try {
            return economy.format(amount.doubleValue());
        } catch (Exception e) {
            return String.format("%.2f", amount);
        }
    }
}