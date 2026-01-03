package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionSystem;



import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.platform.PlatformEconomyManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.ErrorHandlingUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {

        if (!validateServiceAvailability(
                () -> economyManager.isEconomyAvailable(),
                "Economy",
                player)) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_ECONOMY_NOT_AVAILABLE, null, player)), player);
        }

        ActionSystem.ActionResult validationResult = validateBasicParameters(player, actionValue);
        if (validationResult != null) {
            return validationResult;
        }

        try {

            if (isNewCurlyBraceFormat(actionValue, "economy")) {
                return executeNewFormat(player, actionValue, context);
            }


            if (actionValue.trim().startsWith("[") && actionValue.trim().endsWith("]")) {
                return executeMultipleOperations(player, actionValue, context);
            } else {
                return executeSingleOperation(player, actionValue, context);
            }
        } catch (Exception e) {
            logger.error("Error executing economy action for player " + player.getName() + ": " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR",
                    createReplacements("error", "Error executing economy action: " + e.getMessage()), player);
        }
    }


    private ActionSystem.ActionResult executeNewFormat(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        try {
            List<String> operations = parseNewFormatValues(actionValue);

            if (operations.isEmpty()) {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "No economy operations found in new format"), player);
            }


            List<String> processedOperations = new ArrayList<>();
            for (String operation : operations) {
                String processedOperation = processPlaceholders(operation, context, player);
                processedOperations.add(processedOperation);
            }


            if (processedOperations.size() == 1) {
                return executeSingleOperation(player, processedOperations.get(0), context);
            }


            return executeMultipleOperationsFromList(processedOperations, player, context);

        } catch (Exception e) {
            logger.error("Error executing new format economy action for player " + player.getName() + ": " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Error parsing new economy format: " + e.getMessage()), player);
        }
    }


    private ActionSystem.ActionResult executeMultipleOperationsFromList(List<String> operations, FormPlayer player, ActionSystem.ActionContext context) {
        for (String operation : operations) {
            ActionSystem.ActionResult result = executeSingleOperation(player, operation.trim(), context);
            if (result.isFailure()) {
                return result;
            }


            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logSuccess("economy", "Executed " + operations.size() + " operations", player);
        return createSuccessResult("ACTION_ECONOMY_SUCCESS",
                createReplacements("message", "Executed " + operations.size() + " economy operations"), player);
    }


    private ActionSystem.ActionResult executeSingleOperation(FormPlayer player, String operationData, ActionSystem.ActionContext context) {
        String processedData = processPlaceholders(operationData.trim(), context, player);
        String[] parts = processedData.split(":");

        if (parts.length < 2) {
            return createFailureResult("ACTION_EXECUTION_ERROR",
                    createReplacements("error", "Invalid economy operation format. Expected: operation:amount"), player);
        }

        String operation = parts[0].toLowerCase().trim();

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
                return createFailureResult("ACTION_EXECUTION_ERROR",
                        createReplacements("error", "Unknown economy operation: " + operation), player);
        }
    }


    private ActionSystem.ActionResult executeMultipleOperations(FormPlayer player, String multiValue, ActionSystem.ActionContext context) {

        String listContent = multiValue.trim().substring(1, multiValue.trim().length() - 1);
        String[] operations = listContent.split(",\\s*");

        for (String operation : operations) {
            ActionSystem.ActionResult result = executeSingleOperation(player, operation.trim(), context);
            if (result.isFailure()) {
                return result;
            }


            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logSuccess("economy", "Executed " + operations.length + " operations", player);
        return createSuccessResult("ACTION_ECONOMY_SUCCESS",
                createReplacements("message", "Executed " + operations.length + " economy operations"), player);
    }

    private ActionSystem.ActionResult handleAdd(FormPlayer player, String[] parts) {
        if (parts.length < 2) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Add operation requires amount"), player);
        }

        try {
            BigDecimal amount = new BigDecimal(parts[1]);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Amount must be positive"), player);
            }

            boolean success = economyManager.addMoney(player, amount);
            if (success) {
                logSuccess("economy", "Added " + amount + " to balance", player);
                return createSuccessResult("ACTION_ECONOMY_SUCCESS", createReplacements("amount", amount.toString()), player);
            } else {
                return createFailureResult("ACTION_ECONOMY_FAILED", createReplacements("error", "Failed to add money"), player);
            }
        } catch (NumberFormatException e) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid amount: " + parts[1]), player);
        }
    }

    private ActionSystem.ActionResult handleRemove(FormPlayer player, String[] parts) {
        if (parts.length < 2) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Remove operation requires amount"), player);
        }

        try {
            BigDecimal amount = new BigDecimal(parts[1]);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Amount must be positive"), player);
            }


            BigDecimal currentBalance = economyManager.getBalance(player);
            if (currentBalance.compareTo(amount) < 0) {
                Map<String, Object> replacements = createReplacements("amount", amount.toString());
                replacements.put("balance", currentBalance.toString());
                return createFailureResult("ACTION_ECONOMY_INSUFFICIENT_FUNDS", replacements, player);
            }

            boolean success = economyManager.removeMoney(player, amount);
            if (success) {
                logSuccess("economy", "Removed " + amount + " from balance", player);
                return createSuccessResult("ACTION_ECONOMY_SUCCESS", createReplacements("amount", amount.toString()), player);
            } else {
                return createFailureResult("ACTION_ECONOMY_FAILED", createReplacements("error", "Failed to remove money"), player);
            }
        } catch (NumberFormatException e) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid amount: " + parts[1]), player);
        }
    }

    private ActionSystem.ActionResult handleSet(FormPlayer player, String[] parts) {
        if (parts.length < 2) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Set operation requires amount"), player);
        }

        try {
            BigDecimal amount = new BigDecimal(parts[1]);
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Amount cannot be negative"), player);
            }

            boolean success = economyManager.setBalance(player, amount);
            if (success) {
                logSuccess("economy", "Set balance to " + amount, player);
                return createSuccessResult("ACTION_ECONOMY_SUCCESS", createReplacements("amount", amount.toString()), player);
            } else {
                return createFailureResult("ACTION_ECONOMY_FAILED", createReplacements("error", "Failed to set money"), player);
            }
        } catch (NumberFormatException e) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid amount: " + parts[1]), player);
        }
    }

    private ActionSystem.ActionResult handleCheck(FormPlayer player, String[] parts) {
        if (parts.length < 2) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Check operation requires amount"), player);
        }

        try {
            BigDecimal requiredAmount = new BigDecimal(parts[1]);
            BigDecimal currentBalance = economyManager.getBalance(player);

            if (currentBalance.compareTo(requiredAmount) >= 0) {
                logSuccess("economy", "Check passed: has " + currentBalance + " >= " + requiredAmount, player);
                Map<String, Object> replacements = createReplacements("amount", requiredAmount.toString());
                replacements.put("balance", currentBalance.toString());
                return createSuccessResult("ACTION_ECONOMY_CHECK_SUCCESS", replacements, player);
            } else {
                Map<String, Object> replacements = createReplacements("amount", requiredAmount.toString());
                replacements.put("balance", currentBalance.toString());
                return createFailureResult("ACTION_ECONOMY_CHECK_FAILED", replacements, player);
            }
        } catch (NumberFormatException e) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid amount: " + parts[1]), player);
        }
    }

    private ActionSystem.ActionResult handlePay(FormPlayer player, String[] parts, ActionSystem.ActionContext context) {
        if (parts.length < 3) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Pay operation requires target and amount"), player);
        }

        String targetPlayerName = processPlaceholders(parts[1], context, player);

        try {
            BigDecimal amount = new BigDecimal(parts[2]);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Amount must be positive"), player);
            }


            BigDecimal currentBalance = economyManager.getBalance(player);
            if (currentBalance.compareTo(amount) < 0) {
                Map<String, Object> replacements = createReplacements("amount", amount.toString());
                replacements.put("balance", currentBalance.toString());
                return createFailureResult("ACTION_ECONOMY_INSUFFICIENT_FUNDS", replacements, player);
            }


            boolean success = economyManager.removeMoney(player, amount);
            if (success) {
                logSuccess("economy", "Paid " + amount + " to " + targetPlayerName, player);
                Map<String, Object> replacements = createReplacements("amount", amount.toString());
                replacements.put("target", targetPlayerName);
                return createSuccessResult("ACTION_ECONOMY_PAY_SUCCESS", replacements, player);
            } else {
                return createFailureResult("ACTION_ECONOMY_PAY_FAILED",
                        createReplacements("target", targetPlayerName), player);
            }
        } catch (NumberFormatException e) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid amount: " + parts[2]), player);
        }
    }

    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }

        String trimmed = actionValue.trim();

        // Support new unified format "economy { ... }"
        if (isNewCurlyBraceFormat(trimmed, "economy")) {
            try {
                java.util.List<String> ops = parseNewFormatValues(trimmed);
                for (String op : ops) {
                    if (!isValidSingleOperation(op.trim())) {
                        return false;
                    }
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {

            String listContent = trimmed.substring(1, trimmed.length() - 1);
            if (listContent.trim().isEmpty()) {
                return false;
            }
            String[] operations = listContent.split(",\\s*");
            for (String operation : operations) {
                if (!isValidSingleOperation(operation.trim())) {
                    return false;
                }
            }
            return true;
        } else {
            return isValidSingleOperation(trimmed);
        }
    }

    private boolean isValidSingleOperation(String operationData) {
        if (operationData.isEmpty()) return false;

        String[] parts = operationData.split(":");
        if (parts.length < 2) return false;

        String operation = parts[0].toLowerCase().trim();
        if (!operation.matches("add|remove|set|check|pay")) {
            return false;
        }


        try {
            new BigDecimal(parts[1]);
        } catch (NumberFormatException e) {
            return false;
        }


        if ("pay".equals(operation) && parts.length < 3) {
            return false;
        }

        return true;
    }

    @Override
    public String getDescription() {
        return "Handles economy operations: add, remove, set, check, and pay with support for multiple operations";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{
                "New Format Examples:",
                "economy { - \"add:1000\" }",
                "economy { - \"remove:500\" }",
                "economy { - \"set:10000\" }",
                "economy { - \"check:1000\" }",
                "economy { - \"pay:OtherPlayer:100\" }",
                "economy { - \"add:1000\" - \"check:500\" - \"pay:Friend:250\" }"
        };
    }
}


