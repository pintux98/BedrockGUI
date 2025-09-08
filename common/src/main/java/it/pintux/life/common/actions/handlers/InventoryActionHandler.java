package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.platform.PlatformCommandExecutor;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;

import java.util.HashMap;
import java.util.Map;

/**
 * Action handler for managing player inventory operations
 * Supports giving items, removing items, clearing inventory, and checking inventory space
 */
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
    public ActionResult execute(FormPlayer player, String actionData, ActionContext context) {
        ActionResult validationResult = validateBasicParameters(player, actionData);
        if (validationResult != null) {
            return validationResult;
        }
        
        try {
            // Process placeholders in the action data
            String processedData = processPlaceholders(actionData.trim(), context, player);
            String[] parts = processedData.split(":", 3);
            
            if (parts.length < 2) {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_INVALID_FORMAT, null, player)), player);
            }
            
            String operation = parts[0].toLowerCase();
            String itemData = parts[1];
            
            switch (operation) {
                case "give":
                    return handleGiveItem(player, itemData, parts.length > 2 ? parts[2] : "1");
                    
                case "remove":
                    return handleRemoveItem(player, itemData, parts.length > 2 ? parts[2] : "1");
                    
                case "clear":
                    return handleClearInventory(player, itemData);
                    
                case "check":
                    return handleCheckInventory(player, itemData);
                    
                case "slot":
                    return handleSlotOperation(player, itemData, parts.length > 2 ? parts[2] : "");
                    
                default:
                    logger.warn("Unknown inventory operation: " + operation + " for player: " + player.getName());
                    MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                    Map<String, Object> replacements = new HashMap<>();
                    replacements.put("operation", operation);
                    return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, replacements, player)), player);
            }
            
        } catch (Exception e) {
            logger.error("Error executing inventory action for player " + player.getName(), e);
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("error", e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_EXECUTION_ERROR, replacements, player)), player, e);
        }
    }
    
    private ActionResult handleGiveItem(FormPlayer player, String itemData, String amountStr) {
        try {
            int amount = Integer.parseInt(amountStr);
            if (amount <= 0) {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Amount must be greater than 0"), player);
            }
            
            // Use Minecraft give command
            String command = "give " + player.getName() + " " + itemData + " " + amount;
            boolean success = commandExecutor.executeAsConsole(command);
            
            if (success) {
                logger.debug("Successfully gave " + amount + " " + itemData + " to player " + player.getName());
                return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Successfully gave " + amount + " " + itemData + " to " + player.getName()), player);
            } else {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to give item to player"), player);
            }
            
        } catch (NumberFormatException e) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid amount: " + amountStr), player);
        }
    }
    
    private ActionResult handleRemoveItem(FormPlayer player, String itemData, String amountStr) {
        try {
            int amount = Integer.parseInt(amountStr);
            if (amount <= 0) {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Amount must be greater than 0"), player);
            }
            
            // Use Minecraft clear command to remove specific items
            String command = "clear " + player.getName() + " " + itemData + " " + amount;
            boolean success = commandExecutor.executeAsConsole(command);
            
            if (success) {
                logger.debug("Successfully removed " + amount + " " + itemData + " from player " + player.getName());
                return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Successfully removed " + amount + " " + itemData + " from " + player.getName()), player);
            } else {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to remove item from player"), player);
            }
            
        } catch (NumberFormatException e) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid amount: " + amountStr), player);
        }
    }
    
    private ActionResult handleClearInventory(FormPlayer player, String target) {
        String command;
        
        switch (target.toLowerCase()) {
            case "all":
                command = "clear " + player.getName();
                break;
            case "hotbar":
                // Clear hotbar slots (0-8)
                command = "clear " + player.getName() + " * * 9";
                break;
            case "armor":
                // Clear armor slots
                command = "replaceitem entity " + player.getName() + " slot.armor.head air";
                commandExecutor.executeAsConsole(command);
                command = "replaceitem entity " + player.getName() + " slot.armor.chest air";
                commandExecutor.executeAsConsole(command);
                command = "replaceitem entity " + player.getName() + " slot.armor.legs air";
                commandExecutor.executeAsConsole(command);
                command = "replaceitem entity " + player.getName() + " slot.armor.feet air";
                break;
            case "offhand":
                command = "replaceitem entity " + player.getName() + " slot.weapon.offhand air";
                break;
            default:
                // Clear specific item type
                command = "clear " + player.getName() + " " + target;
                break;
        }
        
        boolean success = commandExecutor.executeAsConsole(command);
        
        if (success) {
            logger.debug("Successfully cleared " + target + " for player " + player.getName());
            return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Successfully cleared " + target + " for " + player.getName()), player);
        } else {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to clear " + target + " for player"), player);
        }
    }
    
    private ActionResult handleCheckInventory(FormPlayer player, String itemData) {
        // This would typically require platform-specific implementation
        // For now, we'll use a testfor command approach
        String command = "testfor " + player.getName() + " {Inventory:[{id:" + itemData + "\"}]}";
        boolean hasItem = commandExecutor.executeAsConsole(command);
        
        String message = hasItem ? 
            "Player " + player.getName() + " has " + itemData + " in inventory" :
            "Player " + player.getName() + " does not have " + itemData + " in inventory";
            
        return createSuccessResult("ACTION_SUCCESS", createReplacements("message", message), player);
    }
    
    private ActionResult handleSlotOperation(FormPlayer player, String slotData, String itemData) {
        try {
            int slot = Integer.parseInt(slotData);
            if (slot < 0 || slot > 35) {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid slot number. Must be between 0-35"), player);
            }
            
            String command;
            if (itemData.isEmpty() || itemData.equalsIgnoreCase("air")) {
                // Clear the slot
                command = "replaceitem entity " + player.getName() + " slot.inventory " + slot + " air";
            } else {
                // Set item in slot
                command = "replaceitem entity " + player.getName() + " slot.inventory " + slot + " " + itemData;
            }
            
            boolean success = commandExecutor.executeAsConsole(command);
            
            if (success) {
                logger.debug("Successfully modified slot " + slot + " for player " + player.getName());
                return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Successfully modified slot " + slot + " for " + player.getName()), player);
            } else {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to modify slot for player"), player);
            }
            
        } catch (NumberFormatException e) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid slot number: " + slotData), player);
        }
    }
    

    
    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }
        
        String[] parts = actionValue.split(":", 2);
        if (parts.length < 2) {
            return false;
        }
        
        String operation = parts[0].toLowerCase();
        return operation.equals("give") || operation.equals("remove") || 
               operation.equals("clear") || operation.equals("check") || 
               operation.equals("slot");
    }
    
    @Override
    public String getDescription() {
        return "Manages player inventory operations including giving items, removing items, clearing inventory, and checking inventory contents";
    }
    
    @Override
    public String[] getUsageExamples() {
        return new String[]{
            "inventory:give:diamond:5 - Give 5 diamonds to the player",
            "inventory:remove:stone:10 - Remove 10 stone from the player",
            "inventory:clear:all - Clear the entire inventory",
            "inventory:clear:armor - Clear only armor slots",
            "inventory:check:diamond_sword - Check if player has a diamond sword",
            "inventory:slot:0:diamond - Put a diamond in hotbar slot 0",
            "inventory:slot:9:air - Clear inventory slot 9"
        };
    }
}