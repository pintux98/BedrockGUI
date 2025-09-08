package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.platform.PlatformCommandExecutor;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.PlaceholderUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Action handler for managing player health, hunger, and experience
 * Supports setting, adding, removing health/hunger/experience points
 */
public class HealthActionHandler extends BaseActionHandler {
    
    private final PlatformCommandExecutor commandExecutor;
    
    public HealthActionHandler(PlatformCommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }
    
    @Override
    public String getActionType() {
        return "health";
    }
    
    @Override
    public ActionResult execute(FormPlayer player, String actionData, ActionContext context) {
        if (actionData == null || actionData.trim().isEmpty()) {
            logger.warn("Health action called with empty data for player: " + player.getName());
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, null, player)), player);
        }
        
        try {
            // Process placeholders in the action data
            String processedData = processPlaceholders(actionData.trim(), context, player);
            String[] parts = processedData.split(":", 4);
            
            if (parts.length < 3) {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_INVALID_FORMAT, null, player));
            }
            
            String operation = parts[0].toLowerCase();
            String targetPlayer = parts[1];
            String attribute = parts[2].toLowerCase();
            String value = parts.length > 3 ? parts[3] : "1";
            
            // Replace %player_name% placeholder if used
            if (targetPlayer.equals("%player_name%") || targetPlayer.equals("@s")) {
                targetPlayer = player.getName();
            }
            
            switch (operation) {
                case "set":
                    return handleSetAttribute(player, targetPlayer, attribute, value);
                    
                case "add":
                case "give":
                    return handleAddAttribute(player, targetPlayer, attribute, value);
                    
                case "remove":
                case "take":
                    return handleRemoveAttribute(player, targetPlayer, attribute, value);
                    
                case "heal":
                case "restore":
                    return handleHealPlayer(player, targetPlayer, attribute);
                    
                case "damage":
                case "hurt":
                    return handleDamagePlayer(player, targetPlayer, value);
                    
                case "max":
                case "full":
                    return handleMaxAttribute(player, targetPlayer, attribute);
                    
                case "check":
                case "get":
                    return handleCheckAttribute(player, targetPlayer, attribute);
                    
                default:
                    logger.warn("Unknown health operation: " + operation + " for player: " + player.getName());
                    MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                    Map<String, Object> replacements = new HashMap<>();
                    replacements.put("operation", operation);
                    return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, replacements, player)), player);
            }
            
        } catch (Exception e) {
            logger.error("Error executing health action for player " + player.getName(), e);
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("error", e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_EXECUTION_ERROR, replacements, player)), player, e);
        }
    }
    
    private ActionResult handleSetAttribute(FormPlayer player, String targetPlayer, String attribute, String value) {
        String command = buildAttributeCommand("set", targetPlayer, attribute, value);
        if (command == null) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid attribute: " + attribute + ". Valid attributes: health, hunger, experience, level"), player);
        }
        
        boolean success = commandExecutor.executeAsConsole(command);
        
        if (success) {
            String message = String.format("Successfully set %s's %s to %s", targetPlayer, attribute, value);
            logger.debug(message);
            return createSuccessResult("ACTION_SUCCESS", createReplacements("message", message), player);
        } else {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to set " + attribute + ". Command execution failed."), player);
        }
    }
    
    private ActionResult handleAddAttribute(FormPlayer player, String targetPlayer, String attribute, String value) {
        String command = buildAttributeCommand("add", targetPlayer, attribute, value);
        if (command == null) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid attribute: " + attribute + ". Valid attributes: health, hunger, experience, level"), player);
        }
        
        boolean success = commandExecutor.executeAsConsole(command);
        
        if (success) {
            String message = String.format("Successfully added %s %s to %s", value, attribute, targetPlayer);
            logger.debug(message);
            return createSuccessResult("ACTION_SUCCESS", createReplacements("message", message), player);
        } else {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to add " + attribute + ". Command execution failed."), player);
        }
    }
    
    private ActionResult handleRemoveAttribute(FormPlayer player, String targetPlayer, String attribute, String value) {
        // For removing, we need to use negative values or specific commands
        String command;
        
        switch (attribute) {
            case "health":
                // Use damage command for removing health
                command = String.format("damage %s %s", targetPlayer, value);
                break;
                
            case "hunger":
            case "food":
                // Set hunger to current - value (this would need platform-specific implementation)
                command = String.format("effect give %s minecraft:hunger 1 %s", targetPlayer, value);
                break;
                
            case "experience":
            case "xp":
                // Use negative experience
                try {
                    int expValue = Integer.parseInt(value);
                    command = String.format("experience add %s -%d", targetPlayer, expValue);
                } catch (NumberFormatException e) {
                    return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid experience value: " + value), player);
                }
                break;
                
            case "level":
            case "levels":
                // Use negative levels
                try {
                    int levelValue = Integer.parseInt(value);
                    command = String.format("experience add %s -%dL", targetPlayer, levelValue);
                } catch (NumberFormatException e) {
                    return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid level value: " + value), player);
                }
                break;
                
            default:
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid attribute: " + attribute), player);
        }
        
        boolean success = commandExecutor.executeAsConsole(command);
        
        if (success) {
            String message = String.format("Successfully removed %s %s from %s", value, attribute, targetPlayer);
            logger.debug(message);
            return createSuccessResult("ACTION_SUCCESS", createReplacements("message", message), player);
        } else {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to remove " + attribute + ". Command execution failed."), player);
        }
    }
    
    private ActionResult handleHealPlayer(FormPlayer player, String targetPlayer, String attribute) {
        String command;
        
        switch (attribute) {
            case "health":
            case "all":
                // Heal to full health
                command = String.format("effect give %s minecraft:instant_health 1 10", targetPlayer);
                break;
                
            case "hunger":
            case "food":
                // Restore hunger
                command = String.format("effect give %s minecraft:saturation 1 10", targetPlayer);
                break;
                
            default:
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Cannot heal attribute: " + attribute + ". Use: health, hunger, or all"), player);
        }
        
        boolean success = commandExecutor.executeAsConsole(command);
        
        if (success) {
            String message = String.format("Successfully healed %s's %s", targetPlayer, attribute);
            logger.debug(message);
            return createSuccessResult("ACTION_SUCCESS", createReplacements("message", message), player);
        } else {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to heal " + attribute + ". Command execution failed."), player);
        }
    }
    
    private ActionResult handleDamagePlayer(FormPlayer player, String targetPlayer, String damageValue) {
        try {
            float damage = Float.parseFloat(damageValue);
            if (damage < 0) {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Damage value must be positive"), player);
            }
            
            String command = String.format("damage %s %.1f", targetPlayer, damage);
            boolean success = commandExecutor.executeAsConsole(command);
            
            if (success) {
                String message = String.format("Successfully dealt %.1f damage to %s", damage, targetPlayer);
                logger.debug(message);
                return createSuccessResult("ACTION_SUCCESS", createReplacements("message", message), player);
            } else {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to damage player. Command execution failed."), player);
            }
        } catch (NumberFormatException e) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid damage value: " + damageValue), player);
        }
    }
    
    private ActionResult handleMaxAttribute(FormPlayer player, String targetPlayer, String attribute) {
        String command;
        
        switch (attribute) {
            case "health":
                // Set to maximum health (20 hearts = 40 health points)
                command = String.format("attribute %s minecraft:generic.max_health base set 20", targetPlayer);
                break;
                
            case "hunger":
            case "food":
                // Set hunger to maximum (20 hunger points)
                command = String.format("effect give %s minecraft:saturation 1 20", targetPlayer);
                break;
                
            default:
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Cannot maximize attribute: " + attribute + ". Use: health or hunger"), player);
        }
        
        boolean success = commandExecutor.executeAsConsole(command);
        
        if (success) {
            String message = String.format("Successfully maximized %s's %s", targetPlayer, attribute);
            logger.debug(message);
            return createSuccessResult("ACTION_SUCCESS", createReplacements("message", message), player);
        } else {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to maximize " + attribute + ". Command execution failed."), player);
        }
    }
    
    private ActionResult handleCheckAttribute(FormPlayer player, String targetPlayer, String attribute) {
        String command;
        
        switch (attribute) {
            case "health":
                command = String.format("data get entity %s Health", targetPlayer);
                break;
                
            case "hunger":
            case "food":
                command = String.format("data get entity %s foodLevel", targetPlayer);
                break;
                
            case "experience":
            case "xp":
                command = String.format("data get entity %s XpTotal", targetPlayer);
                break;
                
            case "level":
            case "levels":
                command = String.format("data get entity %s XpLevel", targetPlayer);
                break;
                
            default:
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid attribute: " + attribute), player);
        }
        
        boolean success = commandExecutor.executeAsConsole(command);
        
        if (success) {
            return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Checked " + attribute + " for " + targetPlayer), player);
        } else {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to check " + attribute + ". Command execution failed."), player);
        }
    }
    
    private String buildAttributeCommand(String operation, String targetPlayer, String attribute, String value) {
        try {
            switch (attribute) {
                case "health":
                    if (operation.equals("set")) {
                        float healthValue = Float.parseFloat(value);
                        if (healthValue < 0 || healthValue > 40) { // 40 = 20 hearts * 2
                            return null;
                        }
                        return String.format("attribute %s minecraft:generic.max_health base set %.1f", targetPlayer, healthValue / 2);
                    } else if (operation.equals("add")) {
                        // Use instant health effect for adding health
                        return String.format("effect give %s minecraft:instant_health 1 1", targetPlayer);
                    }
                    break;
                    
                case "hunger":
                case "food":
                    if (operation.equals("set")) {
                        int hungerValue = Integer.parseInt(value);
                        if (hungerValue < 0 || hungerValue > 20) {
                            return null;
                        }
                        // Use saturation effect to restore hunger
                        return String.format("effect give %s minecraft:saturation 1 %d", targetPlayer, hungerValue);
                    } else if (operation.equals("add")) {
                        return String.format("effect give %s minecraft:saturation 1 %s", targetPlayer, value);
                    }
                    break;
                    
                case "experience":
                case "xp":
                    int expValue = Integer.parseInt(value);
                    if (operation.equals("set")) {
                        return String.format("experience set %s %d", targetPlayer, expValue);
                    } else if (operation.equals("add")) {
                        return String.format("experience add %s %d", targetPlayer, expValue);
                    }
                    break;
                    
                case "level":
                case "levels":
                    int levelValue = Integer.parseInt(value);
                    if (operation.equals("set")) {
                        return String.format("experience set %s %dL", targetPlayer, levelValue);
                    } else if (operation.equals("add")) {
                        return String.format("experience add %s %dL", targetPlayer, levelValue);
                    }
                    break;
                    
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        
        return null;
    }
    

    
    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }
        
        String[] parts = actionValue.split(":", 3);
        if (parts.length < 3) {
            return false;
        }
        
        String operation = parts[0].toLowerCase();
        String attribute = parts[2].toLowerCase();
        
        boolean validOperation = operation.equals("set") || operation.equals("add") || operation.equals("give") ||
                                operation.equals("remove") || operation.equals("take") ||
                                operation.equals("heal") || operation.equals("restore") ||
                                operation.equals("damage") || operation.equals("hurt") ||
                                operation.equals("max") || operation.equals("full") ||
                                operation.equals("check") || operation.equals("get");
        
        boolean validAttribute = attribute.equals("health") || attribute.equals("hunger") || attribute.equals("food") ||
                                attribute.equals("experience") || attribute.equals("xp") ||
                                attribute.equals("level") || attribute.equals("levels") || attribute.equals("all");
        
        return validOperation && validAttribute;
    }
    
    @Override
    public String getDescription() {
        return "Manages player health, hunger, and experience including setting, adding, removing, and checking these attributes. Supports healing and damage operations.";
    }
    
    @Override
    public String[] getUsageExamples() {
        return new String[]{
            "health:set:%player_name%:health:20 - Set current player's health to 20 (10 hearts)",
            "health:add:PlayerName:experience:100 - Give 100 experience points to player",
            "health:remove:%player_name%:hunger:5 - Remove 5 hunger points from current player",
            "health:heal:%player_name%:all - Fully heal current player (health and hunger)",
            "health:damage:PlayerName:5 - Deal 5 damage to specific player",
            "health:max:%player_name%:health - Set current player's health to maximum",
            "health:set:@a:level:10 - Set all players to level 10",
            "health:check:%player_name%:experience - Check current player's experience"
        };
    }
}