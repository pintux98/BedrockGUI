package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.platform.PlatformCommandExecutor;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class PlayerManagementHandler extends BaseActionHandler {
    
    private final PlatformCommandExecutor commandExecutor;
    private static final Set<String> SUPPORTED_ACTIONS = new HashSet<>(Arrays.asList(
        "health", "gamemode", "inventory", "teleport", "potion", "effect"
    ));
    
    public PlayerManagementHandler(PlatformCommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }
    
    @Override
    public String getActionType() {
        return "player";
    }
    
    @Override
    public ActionResult execute(FormPlayer player, String actionValue, ActionContext context) {
        
        ActionResult validationResult = validateActionParameters(player, actionValue, 
            () -> isValidAction(actionValue));
        if (validationResult != null) {
            return validationResult;
        }
        
        try {
            String processedAction = processPlaceholders(actionValue, context, player);
            String[] parts = processedAction.split(":", 2);
            
            if (parts.length < 2) {
                return createFailureResult("ACTION_EXECUTION_ERROR", 
                    createReplacements("error", "Invalid action format. Expected: action_type:parameters"), player);
            }
            
            String actionType = parts[0].toLowerCase();
            String actionData = parts[1];
            
            switch (actionType) {
                case "health":
                    return handleHealthAction(player, actionData, context);
                case "gamemode":
                    return handleGameModeAction(player, actionData, context);
                case "inventory":
                    return handleInventoryAction(player, actionData, context);
                case "teleport":
                    return handleTeleportAction(player, actionData, context);
                case "potion":
                case "effect":
                    return handlePotionAction(player, actionData, context);
                default:
                    return createFailureResult("ACTION_EXECUTION_ERROR", 
                        createReplacements("error", "Unsupported player action: " + actionType), player);
            }
            
        } catch (Exception e) {
            logError("player management", actionValue, player, e);
            return createFailureResult("ACTION_EXECUTION_ERROR", 
                createReplacements("error", e.getMessage()), player, e);
        }
    }
    
    
    private ActionResult handleHealthAction(FormPlayer player, String actionData, ActionContext context) {
        String[] parts = actionData.split(":", 3);
        if (parts.length < 3) {
            return createFailureResult("ACTION_EXECUTION_ERROR", 
                createReplacements("error", "Invalid health action format. Expected: operation:target:value"), player);
        }
        
        String operation = parts[0].toLowerCase();
        String targetPlayer = parts[1];
        String value = parts[2];
        
        String command = buildHealthCommand(operation, targetPlayer, value);
        boolean success = executeWithErrorHandling(
            () -> commandExecutor.executeAsConsole(command),
            "Health action: " + command,
            player
        );
        
        if (success) {
            logSuccess("health", actionData, player);
            return createSuccessResult("ACTION_SUCCESS", 
                createReplacements("message", "Health action executed successfully"), player);
        } else {
            return createFailureResult("ACTION_EXECUTION_ERROR", 
                createReplacements("error", "Failed to execute health action"), player);
        }
    }
    
    
    private ActionResult handleGameModeAction(FormPlayer player, String actionData, ActionContext context) {
        String[] parts = actionData.split(":", 3);
        if (parts.length < 2) {
            return createFailureResult("ACTION_EXECUTION_ERROR", 
                createReplacements("error", "Invalid gamemode action format. Expected: operation:target[:mode]"), player);
        }
        
        String operation = parts[0].toLowerCase();
        String targetPlayer = parts[1];
        String gameMode = parts.length > 2 ? parts[2] : "survival";
        
        String command = buildGameModeCommand(operation, targetPlayer, gameMode);
        boolean success = executeWithErrorHandling(
            () -> commandExecutor.executeAsConsole(command),
            "GameMode action: " + command,
            player
        );
        
        if (success) {
            logSuccess("gamemode", actionData, player);
            return createSuccessResult("ACTION_SUCCESS", 
                createReplacements("message", "GameMode action executed successfully"), player);
        } else {
            return createFailureResult("ACTION_EXECUTION_ERROR", 
                createReplacements("error", "Failed to execute gamemode action"), player);
        }
    }
    
    
    private ActionResult handleInventoryAction(FormPlayer player, String actionData, ActionContext context) {
        String[] parts = actionData.split(":", 4);
        if (parts.length < 3) {
            return createFailureResult("ACTION_EXECUTION_ERROR", 
                createReplacements("error", "Invalid inventory action format. Expected: operation:target:item[:amount]"), player);
        }
        
        String operation = parts[0].toLowerCase();
        String targetPlayer = parts[1];
        String item = parts[2];
        String amount = parts.length > 3 ? parts[3] : "1";
        
        String command = buildInventoryCommand(operation, targetPlayer, item, amount);
        boolean success = executeWithErrorHandling(
            () -> commandExecutor.executeAsConsole(command),
            "Inventory action: " + command,
            player
        );
        
        if (success) {
            logSuccess("inventory", actionData, player);
            return createSuccessResult("ACTION_SUCCESS", 
                createReplacements("message", "Inventory action executed successfully"), player);
        } else {
            return createFailureResult("ACTION_EXECUTION_ERROR", 
                createReplacements("error", "Failed to execute inventory action"), player);
        }
    }
    
    
    private ActionResult handleTeleportAction(FormPlayer player, String actionData, ActionContext context) {
        String[] parts = actionData.split(":");
        if (parts.length < 4) {
            return createFailureResult("ACTION_EXECUTION_ERROR", 
                createReplacements("error", "Invalid teleport action format. Expected: target:x:y:z[:world]"), player);
        }
        
        String targetPlayer = parts[0];
        String x = parts[1];
        String y = parts[2];
        String z = parts[3];
        String world = parts.length > 4 ? parts[4] : "";
        
        String command = buildTeleportCommand(targetPlayer, x, y, z, world);
        boolean success = executeWithErrorHandling(
            () -> commandExecutor.executeAsConsole(command),
            "Teleport action: " + command,
            player
        );
        
        if (success) {
            logSuccess("teleport", actionData, player);
            return createSuccessResult("ACTION_SUCCESS", 
                createReplacements("message", "Teleport action executed successfully"), player);
        } else {
            return createFailureResult("ACTION_EXECUTION_ERROR", 
                createReplacements("error", "Failed to execute teleport action"), player);
        }
    }
    
    
    private ActionResult handlePotionAction(FormPlayer player, String actionData, ActionContext context) {
        String[] parts = actionData.split(":", 5);
        if (parts.length < 3) {
            return createFailureResult("ACTION_EXECUTION_ERROR", 
                createReplacements("error", "Invalid potion action format. Expected: operation:target:effect[:duration:amplifier]"), player);
        }
        
        String operation = parts[0].toLowerCase();
        String targetPlayer = parts[1];
        String effect = parts[2];
        String duration = parts.length > 3 ? parts[3] : "30";
        String amplifier = parts.length > 4 ? parts[4] : "0";
        
        String command = buildPotionCommand(operation, targetPlayer, effect, duration, amplifier);
        boolean success = executeWithErrorHandling(
            () -> commandExecutor.executeAsConsole(command),
            "Potion action: " + command,
            player
        );
        
        if (success) {
            logSuccess("potion", actionData, player);
            return createSuccessResult("ACTION_SUCCESS", 
                createReplacements("message", "Potion action executed successfully"), player);
        } else {
            return createFailureResult("ACTION_EXECUTION_ERROR", 
                createReplacements("error", "Failed to execute potion action"), player);
        }
    }
    
    
    private String buildHealthCommand(String operation, String target, String value) {
        switch (operation) {
            case "set":
                return "health " + target + " " + value;
            case "add":
                return "heal " + target + " " + value;
            case "remove":
                return "damage " + target + " " + value;
            default:
                return "health " + target + " " + value;
        }
    }
    
    private String buildGameModeCommand(String operation, String target, String mode) {
        return "gamemode " + normalizeGameMode(mode) + " " + target;
    }
    
    private String buildInventoryCommand(String operation, String target, String item, String amount) {
        switch (operation) {
            case "give":
                return "give " + target + " " + item + " " + amount;
            case "take":
                return "clear " + target + " " + item + " " + amount;
            case "clear":
                return "clear " + target;
            default:
                return "give " + target + " " + item + " " + amount;
        }
    }
    
    private String buildTeleportCommand(String target, String x, String y, String z, String world) {
        if (world.isEmpty()) {
            return "tp " + target + " " + x + " " + y + " " + z;
        } else {
            return "execute in " + world + " run tp " + target + " " + x + " " + y + " " + z;
        }
    }
    
    private String buildPotionCommand(String operation, String target, String effect, String duration, String amplifier) {
        switch (operation) {
            case "give":
            case "add":
                return "effect give " + target + " " + effect + " " + duration + " " + amplifier;
            case "remove":
            case "clear":
                return "effect clear " + target + " " + effect;
            default:
                return "effect give " + target + " " + effect + " " + duration + " " + amplifier;
        }
    }
    
    private String normalizeGameMode(String gameMode) {
        switch (gameMode.toLowerCase()) {
            case "0":
            case "s":
            case "survival":
                return "survival";
            case "1":
            case "c":
            case "creative":
                return "creative";
            case "2":
            case "a":
            case "adventure":
                return "adventure";
            case "3":
            case "sp":
            case "spectator":
                return "spectator";
            default:
                return "survival";
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
        
        String actionType = parts[0].toLowerCase();
        return SUPPORTED_ACTIONS.contains(actionType);
    }
    
    @Override
    public String getDescription() {
        return "Composite handler for player management actions including health, gamemode, inventory, teleport, and potion effects. " +
               "Consolidates multiple player-related operations into a single unified interface.";
    }
    
    @Override
    public String[] getUsageExamples() {
        return new String[]{
                "player:health:set:{player}:20",
                "player:gamemode:set:{player}:creative",
                "player:inventory:give:{player}:diamond:5",
                "player:teleport:{player}:0:100:0",
                "player:potion:give:{player}:speed:60:1",
                "player:effect:remove:{player}:slowness"
        };
    }
}
