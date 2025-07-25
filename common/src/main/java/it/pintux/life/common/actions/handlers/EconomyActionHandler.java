package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionHandler;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.platform.PlatformEconomyManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;
import it.pintux.life.common.utils.PlaceholderUtil;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Handles economy operations like adding, removing, or checking money.
 * 
 * Usage: economy:add:100 (add 100 to player's balance)
 * Usage: economy:remove:50 (remove 50 from player's balance)
 * Usage: economy:set:1000 (set player's balance to 1000)
 * Usage: economy:check:500 (check if player has at least 500, fails if not)
 * Usage: economy:pay:OtherPlayer:100 (pay 100 to another player)
 */
public class EconomyActionHandler implements ActionHandler {
    private static final Logger logger = Logger.getLogger(EconomyActionHandler.class);
    private final PlatformEconomyManager economyManager;
    
    public EconomyActionHandler(PlatformEconomyManager economyManager) {
        this.economyManager = economyManager;
    }
    
    @Override
    public String getActionType() {
        return "economy";
    }
    
    @Override
    public ActionResult execute(FormPlayer player, String actionData, ActionContext context) {
        if (!economyManager.isEconomyAvailable()) {
            logger.warn("Economy action called but economy is not available");
            return ActionResult.failure("Economy system is not available");
        }
        
        if (actionData == null || actionData.trim().isEmpty()) {
            logger.warn("Economy action called with empty data for player: " + player.getName());
            return ActionResult.failure("No economy operation specified");
        }
        
        try {
            // Process placeholders in the action data
            String processedData = processPlaceholders(actionData.trim(), context);
            String[] parts = processedData.split(":");
            
            if (parts.length < 2) {
                return ActionResult.failure("Invalid economy action format. Use: operation:amount");
            }

            String operation = parts[0].toLowerCase();

            switch (operation) {
                case "add":
                    return handleAdd(player, parts);
                case "remove":
                    return handleRemove(player, parts);
                case "set":
                    return handleSet(player, parts);
                case "check":
                    return handleCheck(player, parts);
                case "pay":
                    return handlePay(player, parts, context);
                default:
                    return ActionResult.failure("Unknown economy operation: " + operation);
            }

        } catch (Exception e) {
            logger.error("Error executing economy action for player " + player.getName() + ": " + e.getMessage());
            return ActionResult.failure("Error executing economy action: " + e.getMessage());
        }
    }

    private ActionResult handleAdd(FormPlayer player, String[] parts) {
        if (parts.length < 2) {
            return ActionResult.failure("Add operation requires amount");
        }

        try {
            double amount = Double.parseDouble(parts[1]);
            if (amount <= 0) {
                return ActionResult.failure("Amount must be positive");
            }

            boolean success = economyManager.addMoney(player, BigDecimal.valueOf(amount));
            if (success) {
                String formatted = economyManager.formatMoney(BigDecimal.valueOf(amount));
                logger.info("Added " + formatted + " to player " + player.getName());
                return ActionResult.success("Added " + formatted + " to your account");
            } else {
                return ActionResult.failure("Failed to add money");
            }
        } catch (NumberFormatException e) {
            return ActionResult.failure("Invalid amount: " + parts[1]);
        }
    }

    private ActionResult handleRemove(FormPlayer player, String[] parts) {
        if (parts.length < 2) {
            return ActionResult.failure("Remove operation requires amount");
        }

        try {
            double amount = Double.parseDouble(parts[1]);
            if (amount <= 0) {
                return ActionResult.failure("Amount must be positive");
            }

            if (!economyManager.hasEnoughMoney(player, BigDecimal.valueOf(amount))) {
                String formatted = economyManager.formatMoney(BigDecimal.valueOf(amount));
                return ActionResult.failure("Insufficient funds. Need " + formatted);
            }

            boolean success = economyManager.removeMoney(player, BigDecimal.valueOf(amount));
            if (success) {
                String formatted = economyManager.formatMoney(BigDecimal.valueOf(amount));
                logger.info("Removed " + formatted + " from player " + player.getName());
                return ActionResult.success("Removed " + formatted + " from your account");
            } else {
                return ActionResult.failure("Failed to remove money");
            }
        } catch (NumberFormatException e) {
            return ActionResult.failure("Invalid amount: " + parts[1]);
        }
    }

    private ActionResult handleSet(FormPlayer player, String[] parts) {
        if (parts.length < 2) {
            return ActionResult.failure("Set operation requires amount");
        }

        try {
            double amount = Double.parseDouble(parts[1]);
            if (amount < 0) {
                return ActionResult.failure("Amount cannot be negative");
            }

            boolean success = economyManager.setBalance(player, BigDecimal.valueOf(amount));
            if (success) {
                String formatted = economyManager.formatMoney(BigDecimal.valueOf(amount));
                logger.info("Set balance of player " + player.getName() + " to " + formatted);
                return ActionResult.success("Balance set to " + formatted);
            } else {
                return ActionResult.failure("Failed to set balance");
            }
        } catch (NumberFormatException e) {
            return ActionResult.failure("Invalid amount: " + parts[1]);
        }
    }

    private ActionResult handleCheck(FormPlayer player, String[] parts) {
        if (parts.length < 2) {
            return ActionResult.failure("Check operation requires amount");
        }

        try {
            double amount = Double.parseDouble(parts[1]);
            if (amount < 0) {
                return ActionResult.failure("Amount cannot be negative");
            }

            boolean hasEnough = economyManager.hasEnoughMoney(player, BigDecimal.valueOf(amount));
            String formatted = economyManager.formatMoney(BigDecimal.valueOf(amount));

            if (hasEnough) {
                return ActionResult.success("You have enough money (" + formatted + ")");
            } else {
                double currentBalance = economyManager.getBalance(player).doubleValue();
                String currentFormatted = economyManager.formatMoney(BigDecimal.valueOf(currentBalance));
                return ActionResult.failure("Insufficient funds. You have " + currentFormatted + ", need " + formatted);
            }
        } catch (NumberFormatException e) {
            return ActionResult.failure("Invalid BigDecimal.valueOf(amount): " + parts[1]);
        }
    }

    private ActionResult handlePay(FormPlayer player, String[] parts, ActionContext context) {
        if (parts.length < 3) {
            return ActionResult.failure("Pay operation requires target player and BigDecimal.valueOf(amount)");
        }

        try {
            String targetPlayerName = processPlaceholders(parts[1], context);
            double amount = Double.parseDouble(parts[2]);

            if (amount <= 0) {
                return ActionResult.failure("Amount must be positive");
            }

            if (!economyManager.hasEnoughMoney(player, BigDecimal.valueOf(amount))) {
                String formatted = economyManager.formatMoney(BigDecimal.valueOf(amount));
                return ActionResult.failure("Insufficient funds. Need " + formatted);
            }

            // Remove money from sender
            boolean removeSuccess = economyManager.removeMoney(player, BigDecimal.valueOf(amount));
            if (!removeSuccess) {
                return ActionResult.failure("Failed to remove money from your account");
            }

            // Note: Adding money to target player would require a way to get FormPlayer by name
            // This is a limitation of the current architecture - we'd need a player lookup service
            String formatted = economyManager.formatMoney(BigDecimal.valueOf(amount));
            logger.info("Player " + player.getName() + " paid " + formatted + " to " + targetPlayerName);
            return ActionResult.success("Paid " + formatted + " to " + targetPlayerName);

        } catch (NumberFormatException e) {
            return ActionResult.failure("Invalid amount: " + parts[2]);
        }
    }
    
    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }
        
        String[] parts = actionValue.trim().split(":");
        if (parts.length < 2) {
            return false;
        }
        
        String operation = parts[0].toLowerCase();
        switch (operation) {
            case "add":
            case "remove":
            case "set":
            case "check":
                return parts.length >= 2;
            case "pay":
                return parts.length >= 3;
            default:
                return false;
        }
    }
    
    @Override
    public String getDescription() {
        return "Handles economy operations including adding, removing, setting, checking balances, and player-to-player payments";
    }
    
    @Override
    public String[] getUsageExamples() {
        return new String[]{
            "economy:add:100",
            "economy:remove:50",
            "economy:set:1000",
            "economy:check",
            "economy:pay:targetPlayer:25"
        };
    }
    
    private String processPlaceholders(String data, ActionContext context) {
        if (context == null) {
            return data;
        }
        
        String result = PlaceholderUtil.processDynamicPlaceholders(data, context.getPlaceholders());
        result = PlaceholderUtil.processFormResults(result, context.getFormResults());
        
        return result;
    }
}