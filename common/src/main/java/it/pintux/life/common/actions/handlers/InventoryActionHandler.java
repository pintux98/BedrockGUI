package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionSystem;



import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.platform.PlatformCommandExecutor;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;

import java.util.HashMap;
import java.util.Map;


public class InventoryActionHandler extends BaseActionHandler {

    private final PlatformCommandExecutor commandExecutor;

    public InventoryActionHandler(PlatformCommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    @Override
    public String getActionType() {
        return "inventory";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        ActionSystem.ActionResult validationResult = validateBasicParameters(player, actionValue);
        if (validationResult != null) {
            return validationResult;
        }

        try {
            // Check if it's the new YAML format with curly braces
            if (actionValue.trim().startsWith("{") && actionValue.trim().endsWith("}")) {
                return executeNewFormatInventoryOperations(player, actionValue, context);
            }
            // Legacy format support
            else if (actionValue.trim().startsWith("[") && actionValue.trim().endsWith("]")) {
                return executeMultipleInventoryOperations(player, actionValue, context);
            } else {
                return executeSingleInventoryOperation(player, actionValue, context);
            }
        } catch (Exception e) {
            logger.error("Error executing inventory action for player " + player.getName() + ": " + e.getMessage());
            return createFailureResult(MessageData.EXECUTION_ERROR,
                    createReplacements("error", "Error executing inventory action: " + e.getMessage()), player);
        }
    }


    private ActionSystem.ActionResult executeNewFormatInventoryOperations(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        try {
            // Parse the new YAML format using parseNewFormatValues
            java.util.List<String> operations = parseNewFormatValues(actionValue);
            
            if (operations.isEmpty()) {
                return createFailureResult(MessageData.EXECUTION_ERROR,
                        createReplacements("error", "No inventory operations found in new format"), player);
            }

            // Execute each operation
            for (String operation : operations) {
                String processedOperation = processPlaceholders(operation, context, player);
                ActionSystem.ActionResult result = executeSingleInventoryOperation(player, processedOperation.trim(), context);
                if (result.isFailure()) {
                    return result;
                }

                // Small delay between operations
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            logSuccess("inventory", "Executed " + operations.size() + " inventory operations (new format)", player);
            return createSuccessResult("ACTION_INVENTORY_SUCCESS",
                    createReplacements("message", "Executed " + operations.size() + " inventory operations"), player);
        } catch (Exception e) {
            logger.error("Error parsing new format inventory action: " + e.getMessage());
            return createFailureResult(MessageData.EXECUTION_ERROR,
                    createReplacements("error", "Error parsing new format: " + e.getMessage()), player);
        }
    }


    private ActionSystem.ActionResult executeSingleInventoryOperation(FormPlayer player, String inventoryData, ActionSystem.ActionContext context) {
        String processedData = processPlaceholders(inventoryData.trim(), context, player);
        String[] parts = processedData.split(":", 3);

        if (parts.length < 2) {
            return createFailureResult(MessageData.EXECUTION_ERROR,
                    createReplacements("error", "Invalid inventory format. Expected: operation:item[:amount]"), player);
        }

        String operation = parts[0].toLowerCase();
        String itemData = parts[1];
        String amount = parts.length > 2 ? parts[2] : "1";

        switch (operation) {
            case "give":
                return handleGiveItem(player, itemData, amount);
            case "remove":
                return handleRemoveItem(player, itemData, amount);
            case "clear":
                return handleClearInventory(player, itemData);
            case "check":
                return handleCheckInventory(player, itemData);
            default:
                return createFailureResult(MessageData.EXECUTION_ERROR,
                        createReplacements("error", "Unknown inventory operation: " + operation), player);
        }
    }


    private ActionSystem.ActionResult executeMultipleInventoryOperations(FormPlayer player, String multiValue, ActionSystem.ActionContext context) {

        String listContent = multiValue.trim().substring(1, multiValue.trim().length() - 1);
        String[] operations = listContent.split(",\\s*");

        for (String operation : operations) {
            ActionSystem.ActionResult result = executeSingleInventoryOperation(player, operation.trim(), context);
            if (result.isFailure()) {
                return result;
            }


            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logSuccess("inventory", "Executed " + operations.length + " inventory operations", player);
        return createSuccessResult("ACTION_INVENTORY_SUCCESS",
                createReplacements("message", "Executed " + operations.length + " inventory operations"), player);
    }

    private ActionSystem.ActionResult handleGiveItem(FormPlayer player, String itemData, String amountStr) {
        try {
            int amount = Integer.parseInt(amountStr);
            String command = "give " + player.getName() + " " + itemData + " " + amount;

            boolean success = executeWithErrorHandling(
                    () -> {
                        commandExecutor.executeAsConsole(command);
                        return true;
                    },
                    "Inventory give command: " + command,
                    player
            );

            if (success) {
                logSuccess("inventory", "Gave " + amount + " " + itemData + " to " + player.getName(), player);
                Map<String, Object> replacements = createReplacements("item", itemData);
                replacements.put("amount", String.valueOf(amount));
                return createSuccessResult("ACTION_INVENTORY_SUCCESS", replacements, player);
            } else {
                return createFailureResult("ACTION_INVENTORY_FAILED",
                        createReplacements("operation", "give " + itemData), player);
            }
        } catch (NumberFormatException e) {
            return createFailureResult(MessageData.EXECUTION_ERROR,
                    createReplacements("error", "Invalid amount: " + amountStr), player);
        }
    }

    private ActionSystem.ActionResult handleRemoveItem(FormPlayer player, String itemData, String amountStr) {
        try {
            int amount = Integer.parseInt(amountStr);
            String command = "clear " + player.getName() + " " + itemData + " " + amount;

            boolean success = executeWithErrorHandling(
                    () -> {
                        commandExecutor.executeAsConsole(command);
                        return true;
                    },
                    "Inventory remove command: " + command,
                    player
            );

            if (success) {
                logSuccess("inventory", "Removed " + amount + " " + itemData + " from " + player.getName(), player);
                Map<String, Object> replacements = createReplacements("item", itemData);
                replacements.put("amount", String.valueOf(amount));
                return createSuccessResult("ACTION_INVENTORY_SUCCESS", replacements, player);
            } else {
                return createFailureResult("ACTION_INVENTORY_FAILED",
                        createReplacements("operation", "remove " + itemData), player);
            }
        } catch (NumberFormatException e) {
            return createFailureResult(MessageData.EXECUTION_ERROR,
                    createReplacements("error", "Invalid amount: " + amountStr), player);
        }
    }

    private ActionSystem.ActionResult handleClearInventory(FormPlayer player, String target) {
        String command;
        if ("all".equals(target) || target.isEmpty()) {
            command = "clear " + player.getName();
        } else {
            command = "clear " + player.getName() + " " + target;
        }

        boolean success = executeWithErrorHandling(
                () -> {
                    commandExecutor.executeAsConsole(command);
                    return true;
                },
                "Inventory clear command: " + command,
                player
        );

        if (success) {
            logSuccess("inventory", "Cleared inventory for " + player.getName(), player);
            return createSuccessResult("ACTION_INVENTORY_SUCCESS",
                    createReplacements("operation", "clear"), player);
        } else {
            return createFailureResult("ACTION_INVENTORY_FAILED",
                    createReplacements("operation", "clear"), player);
        }
    }

    private ActionSystem.ActionResult handleCheckInventory(FormPlayer player, String itemData) {

        String command = "testfor " + player.getName() + " {Inventory:[{id:\"" + itemData + "\"}]}";

        boolean success = executeWithErrorHandling(
                () -> {
                    commandExecutor.executeAsConsole(command);
                    return true;
                },
                "Inventory check command: " + command,
                player
        );

        if (success) {
            logSuccess("inventory", "Checked inventory for " + itemData, player);
            return createSuccessResult("ACTION_INVENTORY_SUCCESS",
                    createReplacements("item", itemData), player);
        } else {
            return createFailureResult("ACTION_INVENTORY_FAILED",
                    createReplacements("operation", "check " + itemData), player);
        }
    }

    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }

        String trimmed = actionValue.trim();

        // Support new unified format "inventory { ... }"
        if (isNewCurlyBraceFormat(trimmed, "inventory")) {
            try {
                java.util.List<String> operations = parseNewFormatValues(trimmed);
                for (String operation : operations) {
                    if (!isValidSingleInventoryOperation(operation.trim())) {
                        return false;
                    }
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        // Legacy format support
        else if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            String listContent = trimmed.substring(1, trimmed.length() - 1);
            String[] operations = listContent.split(",\\s*");
            for (String operation : operations) {
                if (!isValidSingleInventoryOperation(operation.trim())) {
                    return false;
                }
            }
            return true;
        } else {
            return isValidSingleInventoryOperation(trimmed);
        }
    }

    private boolean isValidSingleInventoryOperation(String inventoryData) {
        if (inventoryData.isEmpty()) return false;

        String[] parts = inventoryData.split(":");
        if (parts.length < 2) return false;

        String operation = parts[0].toLowerCase();


        if (!operation.matches("give|remove|clear|check")) {
            return false;
        }


        if (parts.length > 2) {
            try {
                Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String getDescription() {
        return "Manages player inventory operations with support for multiple operations";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{
                "New Format Examples:",
                "inventory { - \"give:diamond:64\" }",
                "inventory { - \"remove:stone:32\" }",
                "inventory { - \"clear:all\" }",
                "inventory { - \"give:diamond:1\" - \"give:emerald:5\" - \"give:gold_ingot:10\" }",
                "inventory { - \"check:diamond\" - \"give:diamond:1\" }"
        };
    }
}

