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
 * Action handler for managing potion effects
 * Supports giving, removing, and clearing potion effects from players
 */
public class PotionActionHandler extends BaseActionHandler {
    
    private final PlatformCommandExecutor commandExecutor;
    
    public PotionActionHandler(PlatformCommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }
    
    @Override
    public String getActionType() {
        return "potion";
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
            String[] parts = processedData.split(":", 6);
            
            if (parts.length < 3) {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_INVALID_FORMAT, null, player)), player);
            }
            
            String operation = parts[0].toLowerCase();
            String targetPlayer = parts[1];
            String effectName = parts[2];
            String duration = parts.length > 3 ? parts[3] : "30";
            String amplifier = parts.length > 4 ? parts[4] : "0";
            String hideParticles = parts.length > 5 ? parts[5] : "false";
            
            // Replace %player_name% placeholder if used
            if (targetPlayer.equals("%player_name%") || targetPlayer.equals("@s")) {
                targetPlayer = player.getName();
            }
            
            switch (operation) {
                case "give":
                case "add":
                case "apply":
                    return handleGiveEffect(targetPlayer, effectName, duration, amplifier, hideParticles, player);
                    
                case "remove":
                case "clear":
                    return handleRemoveEffect(targetPlayer, effectName, player);
                    
                case "clearall":
                case "removeall":
                    return handleClearAllEffects(targetPlayer, player);
                    
                case "check":
                case "list":
                    return handleListEffects(targetPlayer, player);
                    
                default:
                    logger.warn("Unknown potion operation: " + operation + " for player: " + player.getName());
                    MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                    Map<String, Object> replacements = new HashMap<>();
                    replacements.put("operation", operation);
                    return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, replacements, player)), player);
            }
            
        } catch (Exception e) {
            logger.error("Error executing potion action for player " + player.getName(), e);
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("error", e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_EXECUTION_ERROR, replacements, player)), player);
        }
    }
    
    private ActionResult handleGiveEffect(String targetPlayer, String effectName, String duration, String amplifier, String hideParticles, FormPlayer player) {
        // Normalize effect name
        String normalizedEffect = normalizeEffectName(effectName);
        if (normalizedEffect == null) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid potion effect: " + effectName), player);
        }
        
        // Validate and parse duration (in seconds)
        int durationSeconds;
        try {
            durationSeconds = parseDuration(duration);
            if (durationSeconds <= 0) {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Duration must be a positive number"), player);
            }
        } catch (NumberFormatException e) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid duration format: " + duration + ". Use format like: 30s, 5m, 1h, or just seconds"), player);
        }
        
        // Validate and parse amplifier (0-255)
        int amplifierLevel;
        try {
            amplifierLevel = Integer.parseInt(amplifier);
            if (amplifierLevel < 0 || amplifierLevel > 255) {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Amplifier must be between 0 and 255"), player);
            }
        } catch (NumberFormatException e) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid amplifier: " + amplifier + ". Must be a number between 0 and 255"), player);
        }
        
        // Parse hide particles flag
        boolean hideParticlesBool = Boolean.parseBoolean(hideParticles);
        
        // Build the effect command
        String command = String.format("effect give %s %s %d %d %s", 
            targetPlayer, normalizedEffect, durationSeconds, amplifierLevel, hideParticlesBool);
        
        boolean success = commandExecutor.executeAsConsole(command);
        
        if (success) {
            String effectDisplayName = getEffectDisplayName(normalizedEffect);
            String message = String.format("Successfully gave %s effect %s (Level %d) for %s seconds to %s", 
                effectDisplayName, normalizedEffect, amplifierLevel + 1, durationSeconds, targetPlayer);
            logger.debug(message);
            return createSuccessResult("ACTION_SUCCESS", createReplacements("message", message), player);
        } else {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to give potion effect. Command execution failed."), player);
        }
    }
    
    private ActionResult handleRemoveEffect(String targetPlayer, String effectName, FormPlayer player) {
        // Normalize effect name
        String normalizedEffect = normalizeEffectName(effectName);
        if (normalizedEffect == null) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid potion effect: " + effectName), player);
        }
        
        String command = String.format("effect clear %s %s", targetPlayer, normalizedEffect);
        boolean success = commandExecutor.executeAsConsole(command);
        
        if (success) {
            String effectDisplayName = getEffectDisplayName(normalizedEffect);
            String message = String.format("Successfully removed %s effect from %s", effectDisplayName, targetPlayer);
            logger.debug(message);
            return createSuccessResult("ACTION_SUCCESS", createReplacements("message", message), player);
        } else {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to remove potion effect. Command execution failed."), player);
        }
    }
    
    private ActionResult handleClearAllEffects(String targetPlayer, FormPlayer player) {
        String command = String.format("effect clear %s", targetPlayer);
        boolean success = commandExecutor.executeAsConsole(command);
        
        if (success) {
            String message = String.format("Successfully cleared all potion effects from %s", targetPlayer);
            logger.debug(message);
            return createSuccessResult("ACTION_SUCCESS", createReplacements("message", message), player);
        } else {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to clear all potion effects. Command execution failed."), player);
        }
    }
    
    private ActionResult handleListEffects(String targetPlayer, FormPlayer player) {
        // This would typically require platform-specific implementation to get active effects
        // For now, we'll use a command that shows the player's status
        String command = String.format("data get entity %s ActiveEffects", targetPlayer);
        boolean success = commandExecutor.executeAsConsole(command);
        
        if (success) {
            return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Listed active potion effects for " + targetPlayer), player);
        } else {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to list potion effects. Command execution failed."), player);
        }
    }
    
    private String normalizeEffectName(String effectName) {
        if (effectName == null || effectName.trim().isEmpty()) {
            return null;
        }
        
        String normalized = effectName.toLowerCase().trim();
        
        // Handle common aliases and variations
        Map<String, String> effectAliases = new HashMap<>();
        effectAliases.put("speed", "minecraft:speed");
        effectAliases.put("slowness", "minecraft:slowness");
        effectAliases.put("haste", "minecraft:haste");
        effectAliases.put("mining_fatigue", "minecraft:mining_fatigue");
        effectAliases.put("strength", "minecraft:strength");
        effectAliases.put("instant_health", "minecraft:instant_health");
        effectAliases.put("instant_damage", "minecraft:instant_damage");
        effectAliases.put("jump_boost", "minecraft:jump_boost");
        effectAliases.put("nausea", "minecraft:nausea");
        effectAliases.put("regeneration", "minecraft:regeneration");
        effectAliases.put("resistance", "minecraft:resistance");
        effectAliases.put("fire_resistance", "minecraft:fire_resistance");
        effectAliases.put("water_breathing", "minecraft:water_breathing");
        effectAliases.put("invisibility", "minecraft:invisibility");
        effectAliases.put("blindness", "minecraft:blindness");
        effectAliases.put("night_vision", "minecraft:night_vision");
        effectAliases.put("hunger", "minecraft:hunger");
        effectAliases.put("weakness", "minecraft:weakness");
        effectAliases.put("poison", "minecraft:poison");
        effectAliases.put("wither", "minecraft:wither");
        effectAliases.put("health_boost", "minecraft:health_boost");
        effectAliases.put("absorption", "minecraft:absorption");
        effectAliases.put("saturation", "minecraft:saturation");
        effectAliases.put("glowing", "minecraft:glowing");
        effectAliases.put("levitation", "minecraft:levitation");
        effectAliases.put("luck", "minecraft:luck");
        effectAliases.put("bad_luck", "minecraft:unluck");
        effectAliases.put("unluck", "minecraft:unluck");
        effectAliases.put("slow_falling", "minecraft:slow_falling");
        effectAliases.put("conduit_power", "minecraft:conduit_power");
        effectAliases.put("dolphins_grace", "minecraft:dolphins_grace");
        effectAliases.put("bad_omen", "minecraft:bad_omen");
        effectAliases.put("hero_of_the_village", "minecraft:hero_of_the_village");
        
        // Common short names
        effectAliases.put("regen", "minecraft:regeneration");
        effectAliases.put("invis", "minecraft:invisibility");
        effectAliases.put("nv", "minecraft:night_vision");
        effectAliases.put("fr", "minecraft:fire_resistance");
        effectAliases.put("wb", "minecraft:water_breathing");
        effectAliases.put("heal", "minecraft:instant_health");
        effectAliases.put("damage", "minecraft:instant_damage");
        effectAliases.put("harm", "minecraft:instant_damage");
        
        // Check if it's an alias
        if (effectAliases.containsKey(normalized)) {
            return effectAliases.get(normalized);
        }
        
        // If it already has minecraft: prefix, return as is
        if (normalized.startsWith("minecraft:")) {
            return normalized;
        }
        
        // Add minecraft: prefix if it's a valid effect name
        String withPrefix = "minecraft:" + normalized;
        if (isValidMinecraftEffect(normalized)) {
            return withPrefix;
        }
        
        return null;
    }
    
    private boolean isValidMinecraftEffect(String effectName) {
        // List of valid Minecraft effect names (without minecraft: prefix)
        String[] validEffects = {
            "speed", "slowness", "haste", "mining_fatigue", "strength", "instant_health",
            "instant_damage", "jump_boost", "nausea", "regeneration", "resistance",
            "fire_resistance", "water_breathing", "invisibility", "blindness", "night_vision",
            "hunger", "weakness", "poison", "wither", "health_boost", "absorption",
            "saturation", "glowing", "levitation", "luck", "unluck", "slow_falling",
            "conduit_power", "dolphins_grace", "bad_omen", "hero_of_the_village"
        };
        
        for (String validEffect : validEffects) {
            if (validEffect.equals(effectName)) {
                return true;
            }
        }
        return false;
    }
    
    private String getEffectDisplayName(String effectName) {
        if (effectName.startsWith("minecraft:")) {
            String name = effectName.substring(10); // Remove "minecraft:" prefix
            return name.substring(0, 1).toUpperCase() + name.substring(1).replace("_", " ");
        }
        return effectName;
    }
    
    private int parseDuration(String duration) throws NumberFormatException {
        if (duration == null || duration.isEmpty()) {
            return 30; // Default 30 seconds
        }
        
        // Check if it has a time unit suffix
        if (duration.matches(".*[smhd]$")) {
            String timeUnit = duration.substring(duration.length() - 1).toLowerCase();
            int value = Integer.parseInt(duration.substring(0, duration.length() - 1));
            
            switch (timeUnit) {
                case "s": return value;
                case "m": return value * 60;
                case "h": return value * 60 * 60;
                case "d": return value * 24 * 60 * 60;
                default: return Integer.parseInt(duration);
            }
        } else {
            // Assume seconds if no unit
            return Integer.parseInt(duration);
        }
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
        return operation.equals("give") || operation.equals("add") || operation.equals("apply") ||
               operation.equals("remove") || operation.equals("clear") ||
               operation.equals("clearall") || operation.equals("removeall") ||
               operation.equals("check") || operation.equals("list");
    }
    
    @Override
    public String getDescription() {
        return "Manages potion effects for players including giving, removing, and clearing effects. Supports all Minecraft potion effects with customizable duration and amplifier.";
    }
    
    @Override
    public String[] getUsageExamples() {
        return new String[]{
            "potion:give:%player_name%:speed:60:1 - Give Speed II for 60 seconds",
            "potion:give:PlayerName:regeneration:5m:0:true - Give Regeneration I for 5 minutes with hidden particles",
            "potion:remove:%player_name%:poison - Remove poison effect from current player",
            "potion:clearall:%player_name% - Clear all potion effects from current player",
            "potion:give:@a:night_vision:10m - Give night vision to all players for 10 minutes",
            "potion:give:%player_name%:strength:2h:2 - Give Strength III for 2 hours",
            "potion:check:%player_name%:any - List all active effects on current player"
        };
    }
}