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
    public ActionResult execute(FormPlayer player, String actionValue, ActionContext context) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            logger.warn("Health action called with empty data for player: " + player.getName());
            return createFailureResult("ACTION_EXECUTION_ERROR", 
                createReplacements("error", "No health operation specified"), player);
        }
        
        try {
            
            if (actionValue.trim().startsWith("[") && actionValue.trim().endsWith("]")) {
                return executeMultipleHealthOperations(player, actionValue, context);
            } else {
                return executeSingleHealthOperation(player, actionValue, context);
            }
        } catch (Exception e) {
            logger.error("Error executing health action for player " + player.getName() + ": " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", 
                createReplacements("error", "Error executing health action: " + e.getMessage()), player);
        }
    }
    
    
    private ActionResult executeSingleHealthOperation(FormPlayer player, String healthData, ActionContext context) {
        
        String processedData = processPlaceholders(healthData.trim(), context, player);
        String[] parts = processedData.split(":", 4);
        
        if (parts.length < 3) {
            return createFailureResult("ACTION_EXECUTION_ERROR", 
                createReplacements("error", "Invalid health format. Expected: operation:target:attribute[:value]"), player);
        }
        
        String operation = parts[0].toLowerCase();
        String targetPlayer = parts[1];
        String attribute = parts[2].toLowerCase();
        String value = parts.length > 3 ? parts[3] : "1";
        
        
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
            default:
                return createFailureResult("ACTION_EXECUTION_ERROR", 
                    createReplacements("error", "Unknown health operation: " + operation), player);
        }
    }
    
    
    private ActionResult executeMultipleHealthOperations(FormPlayer player, String multiValue, ActionContext context) {
        
        String listContent = multiValue.trim().substring(1, multiValue.trim().length() - 1);
        String[] operations = listContent.split(",\\s*");
        
        for (String operation : operations) {
            ActionResult result = executeSingleHealthOperation(player, operation.trim(), context);
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
        
        logSuccess("health", "Executed " + operations.length + " health operations", player);
        return createSuccessResult("ACTION_HEALTH_SUCCESS", 
            createReplacements("message", "Executed " + operations.length + " health operations"), player);
    }
    
    private ActionResult handleSetAttribute(FormPlayer player, String targetPlayer, String attribute, String value) {
        try {
            int intValue = Integer.parseInt(value);
            String command = buildAttributeCommand("set", targetPlayer, attribute, String.valueOf(intValue));
            
            boolean success = executeWithErrorHandling(
                () -> {
                    commandExecutor.executeAsConsole(command);
                    return true;
                },
                "Health set command: " + command,
                player
            );
            
            if (success) {
                logSuccess("health", "Set " + targetPlayer + "'s " + attribute + " to " + intValue, player);
                Map<String, Object> replacements = createReplacements("player", targetPlayer);
                replacements.put("attribute", attribute);
                replacements.put("value", String.valueOf(intValue));
                return createSuccessResult("ACTION_HEALTH_SUCCESS", replacements, player);
            } else {
                return createFailureResult("ACTION_HEALTH_FAILED", 
                    createReplacements("operation", "set " + attribute), player);
            }
        } catch (NumberFormatException e) {
            return createFailureResult("ACTION_EXECUTION_ERROR", 
                createReplacements("error", "Invalid number: " + value), player);
        }
    }
    
    private ActionResult handleAddAttribute(FormPlayer player, String targetPlayer, String attribute, String value) {
        try {
            int intValue = Integer.parseInt(value);
            String command = buildAttributeCommand("add", targetPlayer, attribute, String.valueOf(intValue));
            
            boolean success = executeWithErrorHandling(
                () -> {
                    commandExecutor.executeAsConsole(command);
                    return true;
                },
                "Health add command: " + command,
                player
            );
            
            if (success) {
                logSuccess("health", "Added " + intValue + " " + attribute + " to " + targetPlayer, player);
                Map<String, Object> replacements = createReplacements("player", targetPlayer);
                replacements.put("attribute", attribute);
                replacements.put("value", String.valueOf(intValue));
                return createSuccessResult("ACTION_HEALTH_SUCCESS", replacements, player);
            } else {
                return createFailureResult("ACTION_HEALTH_FAILED", 
                    createReplacements("operation", "add " + attribute), player);
            }
        } catch (NumberFormatException e) {
            return createFailureResult("ACTION_EXECUTION_ERROR", 
                createReplacements("error", "Invalid number: " + value), player);
        }
    }
    
    private ActionResult handleRemoveAttribute(FormPlayer player, String targetPlayer, String attribute, String value) {
        try {
            int intValue = Integer.parseInt(value);
            String command = buildAttributeCommand("remove", targetPlayer, attribute, String.valueOf(intValue));
            
            boolean success = executeWithErrorHandling(
                () -> {
                    commandExecutor.executeAsConsole(command);
                    return true;
                },
                "Health remove command: " + command,
                player
            );
            
            if (success) {
                logSuccess("health", "Removed " + intValue + " " + attribute + " from " + targetPlayer, player);
                Map<String, Object> replacements = createReplacements("player", targetPlayer);
                replacements.put("attribute", attribute);
                replacements.put("value", String.valueOf(intValue));
                return createSuccessResult("ACTION_HEALTH_SUCCESS", replacements, player);
            } else {
                return createFailureResult("ACTION_HEALTH_FAILED", 
                    createReplacements("operation", "remove " + attribute), player);
            }
        } catch (NumberFormatException e) {
            return createFailureResult("ACTION_EXECUTION_ERROR", 
                createReplacements("error", "Invalid number: " + value), player);
        }
    }
    
    private ActionResult handleHealPlayer(FormPlayer player, String targetPlayer, String attribute) {
        String command;
        if ("health".equals(attribute)) {
            command = "effect give " + targetPlayer + " instant_health 1 255";
        } else if ("hunger".equals(attribute) || "food".equals(attribute)) {
            command = "effect give " + targetPlayer + " saturation 1 255";
        } else {
            command = buildAttributeCommand("set", targetPlayer, attribute, "20");
        }
        
        boolean success = executeWithErrorHandling(
            () -> {
                commandExecutor.executeAsConsole(command);
                return true;
            },
            "Health heal command: " + command,
            player
        );
        
        if (success) {
            logSuccess("health", "Healed " + targetPlayer + "'s " + attribute, player);
            Map<String, Object> replacements = createReplacements("player", targetPlayer);
            replacements.put("attribute", attribute);
            return createSuccessResult("ACTION_HEALTH_SUCCESS", replacements, player);
        } else {
            return createFailureResult("ACTION_HEALTH_FAILED", 
                createReplacements("operation", "heal " + attribute), player);
        }
    }
    
    private ActionResult handleDamagePlayer(FormPlayer player, String targetPlayer, String damageValue) {
        try {
            int damage = Integer.parseInt(damageValue);
            String command = "effect give " + targetPlayer + " instant_damage 1 " + (damage - 1);
            
            boolean success = executeWithErrorHandling(
                () -> {
                    commandExecutor.executeAsConsole(command);
                    return true;
                },
                "Health damage command: " + command,
                player
            );
            
            if (success) {
                logSuccess("health", "Damaged " + targetPlayer + " for " + damage + " points", player);
                Map<String, Object> replacements = createReplacements("player", targetPlayer);
                replacements.put("damage", String.valueOf(damage));
                return createSuccessResult("ACTION_HEALTH_SUCCESS", replacements, player);
            } else {
                return createFailureResult("ACTION_HEALTH_FAILED", 
                    createReplacements("operation", "damage"), player);
            }
        } catch (NumberFormatException e) {
            return createFailureResult("ACTION_EXECUTION_ERROR", 
                createReplacements("error", "Invalid damage value: " + damageValue), player);
        }
    }
    
    private ActionResult handleMaxAttribute(FormPlayer player, String targetPlayer, String attribute) {
        String maxValue;
        switch (attribute) {
            case "health":
                maxValue = "20";
                break;
            case "hunger":
            case "food":
                maxValue = "20";
                break;
            case "experience":
            case "xp":
                maxValue = "1000";
                break;
            default:
                maxValue = "20";
        }
        
        return handleSetAttribute(player, targetPlayer, attribute, maxValue);
    }
    
    private String buildAttributeCommand(String operation, String targetPlayer, String attribute, String value) {
        switch (attribute) {
            case "health":
            case "hp":
                if ("set".equals(operation)) {
                    return "attribute " + targetPlayer + " minecraft:generic.max_health base set " + value;
                } else if ("add".equals(operation)) {
                    return "effect give " + targetPlayer + " instant_health 1 " + (Integer.parseInt(value) - 1);
                } else {
                    return "effect give " + targetPlayer + " instant_damage 1 " + (Integer.parseInt(value) - 1);
                }
                
            case "hunger":
            case "food":
                if ("set".equals(operation)) {
                    return "effect give " + targetPlayer + " saturation 1 255";
                } else {
                    return "effect give " + targetPlayer + " hunger 10 " + value;
                }
                
            case "experience":
            case "xp":
                if ("set".equals(operation)) {
                    return "xp set " + targetPlayer + " " + value;
                } else if ("add".equals(operation)) {
                    return "xp add " + targetPlayer + " " + value;
                } else {
                    return "xp add " + targetPlayer + " -" + value;
                }
                
            default:
                return "effect give " + targetPlayer + " regeneration 10 1";
        }
    }

    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = actionValue.trim();
        
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            
            String listContent = trimmed.substring(1, trimmed.length() - 1);
            String[] operations = listContent.split(",\\s*");
            for (String operation : operations) {
                if (!isValidSingleHealthOperation(operation.trim())) {
                    return false;
                }
            }
            return true;
        } else {
            return isValidSingleHealthOperation(trimmed);
        }
    }
    
    private boolean isValidSingleHealthOperation(String healthData) {
        if (healthData.isEmpty()) return false;
        
        String[] parts = healthData.split(":");
        if (parts.length < 3) return false;
        
        String operation = parts[0].toLowerCase();
        String attribute = parts[2].toLowerCase();
        
        
        if (!operation.matches("set|add|give|remove|take|heal|restore|damage|hurt|max|full")) {
            return false;
        }
        
        
        if (!attribute.matches("health|hp|hunger|food|experience|xp")) {
            return false;
        }
        
        
        if (parts.length > 3) {
            try {
                Integer.parseInt(parts[3]);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public String getDescription() {
        return "Manages player health, hunger, and experience with support for multiple operations";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{
            "New Format Examples:",
            "health { - \"set:PlayerName:health:20\" }",
            "health { - \"add:PlayerName:hunger:5\" }",
            "health { - \"heal:PlayerName:health\" }",
            "health { - \"damage:PlayerName:5\" }",
            "health { - \"set:PlayerName:health:20\" - \"add:PlayerName:xp:100\" - \"heal:PlayerName:hunger\" }"
        };
    }
}
