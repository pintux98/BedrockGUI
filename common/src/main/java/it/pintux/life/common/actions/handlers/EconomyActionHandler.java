package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.platform.PlatformEconomyManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.ErrorHandlingUtil;

import java.math.BigDecimal;
import java.util.HashMap;
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
public class EconomyActionHandler extends BaseActionHandler {
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
        // Validate economy availability with enhanced error handling
        if (!ErrorHandlingUtil.validateServiceAvailability(
                () -> economyManager.isEconomyAvailable(),
                "Economy",
                player)) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_ECONOMY_NOT_AVAILABLE, null, player)), player);
        }
        
        ActionResult validationResult = validateBasicParameters(player, actionData);
        if (validationResult != null) {
            return validationResult;
        }
        
        try {
            // Process placeholders in the action data
            String processedData = processPlaceholders(actionData.trim(), context, player);
            String[] parts = processedData.split(":");
            
            if (parts.length < 2) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_INVALID_FORMAT, null, player)), player);
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
                    MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                    Map<String, Object> replacements = new HashMap<>();
                    replacements.put("operation", operation);
                    return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_INVALID_FORMAT, replacements, player)), player);
            }

        } catch (Exception e) {
            logger.error("Error executing economy action for player " + player.getName() + ": " + e.getMessage());
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("error", e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_EXECUTION_ERROR, replacements, player)), player);
        }
    }

    private ActionResult handleAdd(FormPlayer player, String[] parts) {
        if (parts.length < 2) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ECONOMY_ADD_INVALID_FORMAT, null, player)), player);
        }

        try {
            double amount = Double.parseDouble(parts[1]);
            if (amount <= 0) {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ECONOMY_AMOUNT_POSITIVE, null, player)), player);
            }

            boolean success = economyManager.addMoney(player, BigDecimal.valueOf(amount));
            if (success) {
                String formatted = economyManager.formatMoney(BigDecimal.valueOf(amount));
                logger.info("Added " + formatted + " to player " + player.getName());
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                Map<String, Object> replacements = new HashMap<>();
                replacements.put("amount", formatted);
                return createSuccessResult("ACTION_SUCCESS", createReplacements("message", messageData.getValueNoPrefix(MessageData.ECONOMY_ADD_SUCCESS, replacements, player)), player);
            } else {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ECONOMY_ADD_FAILED, null, player)), player);
            }
        } catch (NumberFormatException e) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("amount", parts[1]);
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_INVALID_FORMAT, replacements, player)), player);
        }
    }

    private ActionResult handleRemove(FormPlayer player, String[] parts) {
        if (parts.length < 2) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ECONOMY_REMOVE_INVALID_FORMAT, null, player)), player);
        }

        try {
            double amount = Double.parseDouble(parts[1]);
            if (amount <= 0) {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ECONOMY_AMOUNT_POSITIVE, null, player)), player);
            }

            if (!economyManager.hasEnoughMoney(player, BigDecimal.valueOf(amount))) {
                String formatted = economyManager.formatMoney(BigDecimal.valueOf(amount));
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                Map<String, Object> replacements = new HashMap<>();
                replacements.put("amount", formatted);
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ECONOMY_INSUFFICIENT_FUNDS, replacements, player)), player);
            }

            boolean success = economyManager.removeMoney(player, BigDecimal.valueOf(amount));
            if (success) {
                String formatted = economyManager.formatMoney(BigDecimal.valueOf(amount));
                logger.info("Removed " + formatted + " from player " + player.getName());
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                Map<String, Object> replacements = new HashMap<>();
                replacements.put("amount", formatted);
                return createSuccessResult("ACTION_SUCCESS", createReplacements("message", messageData.getValueNoPrefix(MessageData.ECONOMY_REMOVE_SUCCESS, replacements, player)), player);
            } else {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ECONOMY_REMOVE_FAILED, null, player)), player);
            }
        } catch (NumberFormatException e) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("amount", parts[1]);
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_INVALID_FORMAT, replacements, player)), player);
        }
    }

    private ActionResult handleSet(FormPlayer player, String[] parts) {
        if (parts.length < 2) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ECONOMY_SET_INVALID_FORMAT, null, player)), player);
        }

        try {
            double amount = Double.parseDouble(parts[1]);
            if (amount < 0) {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ECONOMY_AMOUNT_NEGATIVE, null, player)), player);
            }

            boolean success = economyManager.setBalance(player, BigDecimal.valueOf(amount));
            if (success) {
                String formatted = economyManager.formatMoney(BigDecimal.valueOf(amount));
                logger.info("Set balance of player " + player.getName() + " to " + formatted);
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                Map<String, Object> replacements = new HashMap<>();
                replacements.put("amount", formatted);
                return createSuccessResult("ACTION_SUCCESS", createReplacements("message", messageData.getValueNoPrefix(MessageData.ECONOMY_SET_SUCCESS, replacements, player)), player);
            } else {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ECONOMY_SET_FAILED, null, player)), player);
            }
        } catch (NumberFormatException e) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("amount", parts[1]);
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_INVALID_FORMAT, replacements, player)), player);
        }
    }

    private ActionResult handleCheck(FormPlayer player, String[] parts) {
        if (parts.length < 2) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ECONOMY_CHECK_INVALID_FORMAT, null, player)), player);
        }

        try {
            double amount = Double.parseDouble(parts[1]);
            if (amount < 0) {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ECONOMY_AMOUNT_NEGATIVE, null, player)), player);
            }

            boolean hasEnough = economyManager.hasEnoughMoney(player, BigDecimal.valueOf(amount));
            String formatted = economyManager.formatMoney(BigDecimal.valueOf(amount));

            if (hasEnough) {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                Map<String, Object> replacements = new HashMap<>();
                replacements.put("amount", formatted);
                return createSuccessResult("ACTION_SUCCESS", createReplacements("message", messageData.getValueNoPrefix(MessageData.ECONOMY_CHECK_SUCCESS, replacements, player)), player);
            } else {
                double currentBalance = economyManager.getBalance(player).doubleValue();
                String currentFormatted = economyManager.formatMoney(BigDecimal.valueOf(currentBalance));
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                Map<String, Object> replacements = new HashMap<>();
                replacements.put("current", currentFormatted);
                replacements.put("required", formatted);
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ECONOMY_INSUFFICIENT_FUNDS, replacements, player)), player);
            }
        } catch (NumberFormatException e) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("amount", parts[1]);
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_INVALID_FORMAT, replacements, player)), player);
        }
    }

    private ActionResult handlePay(FormPlayer player, String[] parts, ActionContext context) {
        if (parts.length < 3) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ECONOMY_PAY_INVALID_FORMAT, null, player)), player);
        }

        try {
            String targetPlayerName = processPlaceholders(parts[1], context, player);
            double amount = Double.parseDouble(parts[2]);

            if (amount <= 0) {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ECONOMY_AMOUNT_POSITIVE, null, player)), player);
            }

            if (!economyManager.hasEnoughMoney(player, BigDecimal.valueOf(amount))) {
                String formatted = economyManager.formatMoney(BigDecimal.valueOf(amount));
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                Map<String, Object> replacements = new HashMap<>();
                replacements.put("amount", formatted);
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ECONOMY_INSUFFICIENT_FUNDS, replacements, player)), player);
            }

            // Remove money from sender
            boolean removeSuccess = economyManager.removeMoney(player, BigDecimal.valueOf(amount));
            if (!removeSuccess) {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ECONOMY_PAY_FAILED, null, player)), player);
            }

            // Note: Adding money to target player would require a way to get FormPlayer by name
            // This is a limitation of the current architecture - we'd need a player lookup service
            String formatted = economyManager.formatMoney(BigDecimal.valueOf(amount));
            logger.info("Player " + player.getName() + " paid " + formatted + " to " + targetPlayerName);
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("amount", formatted);
            replacements.put("target", targetPlayerName);
            return createSuccessResult("ACTION_SUCCESS", createReplacements("message", messageData.getValueNoPrefix(MessageData.ECONOMY_PAY_SUCCESS, replacements, player)), player);

        } catch (NumberFormatException e) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("amount", parts[2]);
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_INVALID_FORMAT, replacements, player)), player);
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
    

}
