package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.platform.PlatformCommandExecutor;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.PlaceholderUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
            List<String> operations = parseActionData(actionData, context, player);
            
            if (operations.isEmpty()) {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_INVALID_FORMAT, null, player)), player);
            }
            
            
            if (operations.size() == 1) {
                return executeSinglePotionOperation(operations.get(0), player);
            }
            
            
            return executeMultiplePotionOperations(operations, player);
            
        } catch (Exception e) {
            logger.error("Error executing potion action for player " + player.getName(), e);
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("error", e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_EXECUTION_ERROR, replacements, player)), player);
        }
    }
    
    private ActionResult executeSinglePotionOperation(String operation, FormPlayer player) {
        String[] parts = operation.split(":", 6);
        
        if (parts.length < 3) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_INVALID_FORMAT, null, player)), player);
        }
        
        String operationType = parts[0].toLowerCase();
        String targetPlayer = parts[1];
        String effectName = parts[2];
        String duration = parts.length > 3 ? parts[3] : "30";
        String amplifier = parts.length > 4 ? parts[4] : "0";
        String hideParticles = parts.length > 5 ? parts[5] : "false";
        
        
        if (targetPlayer.equals("%player_name%") || targetPlayer.equals("@s")) {
            targetPlayer = player.getName();
        }
        
        switch (operationType) {
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
                logger.warn("Unknown potion operation: " + operationType + " for player: " + player.getName());
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                Map<String, Object> replacements = new HashMap<>();
                replacements.put("operation", operationType);
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, replacements, player)), player);
        }
    }
    
    private ActionResult executeMultiplePotionOperations(List<String> operations, FormPlayer player) {
        int successCount = 0;
        int totalCount = operations.size();
        StringBuilder results = new StringBuilder();
        
        for (int i = 0; i < operations.size(); i++) {
            String operation = operations.get(i);
            
            try {
                ActionResult result = executeSinglePotionOperation(operation, player);
                
                if (result.isSuccess()) {
                    successCount++;
                    results.append("âś“ Operation ").append(i + 1).append(": ").append(operation).append(" - Success");
                } else {
                    results.append("âś— Operation ").append(i + 1).append(": ").append(operation).append(" - Failed: ").append(result.getMessage());
                }
                
                if (i < operations.size() - 1) {
                    results.append("\n");
                    
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
            } catch (Exception e) {
                results.append("âś— Operation ").append(i + 1).append(": ").append(operation).append(" - Error: ").append(e.getMessage());
                if (i < operations.size() - 1) {
                    results.append("\n");
                }
            }
        }
        
        String finalMessage = String.format("Executed %d/%d potion operations successfully:\n%s", 
            successCount, totalCount, results.toString());
        
        Map<String, Object> replacements = new HashMap<>();
        replacements.put("message", finalMessage);
        replacements.put("success_count", successCount);
        replacements.put("total_count", totalCount);
        
        if (successCount == totalCount) {
            return createSuccessResult("ACTION_SUCCESS", replacements, player);
        } else if (successCount > 0) {
            return createSuccessResult("ACTION_PARTIAL_SUCCESS", replacements, player);
        } else {
            return createFailureResult("ACTION_EXECUTION_ERROR", replacements, player);
        }
    }
    
    private ActionResult handleGiveEffect(String targetPlayer, String effectName, String duration, String amplifier, String hideParticles, FormPlayer player) {
        
        String normalizedEffect = normalizeEffectName(effectName);
        if (normalizedEffect == null) {
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("error", "Invalid potion effect: " + effectName);
            return createFailureResult("ACTION_EXECUTION_ERROR", replacements, player);
        }
        
        
        int durationSeconds;
        try {
            durationSeconds = parseDuration(duration);
            if (durationSeconds <= 0) {
                Map<String, Object> replacements = new HashMap<>();
                replacements.put("error", "Duration must be a positive number");
                return createFailureResult("ACTION_EXECUTION_ERROR", replacements, player);
            }
        } catch (NumberFormatException e) {
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("error", "Invalid duration format: " + duration + ". Use format like: 30s, 5m, 1h, or just seconds");
            return createFailureResult("ACTION_EXECUTION_ERROR", replacements, player);
        }
        
        
        int amplifierLevel;
        try {
            amplifierLevel = Integer.parseInt(amplifier);
            if (amplifierLevel < 0 || amplifierLevel > 255) {
                Map<String, Object> replacements = new HashMap<>();
                replacements.put("error", "Amplifier must be between 0 and 255");
                return createFailureResult("ACTION_EXECUTION_ERROR", replacements, player);
            }
        } catch (NumberFormatException e) {
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("error", "Invalid amplifier: " + amplifier + ". Must be a number between 0 and 255");
            return createFailureResult("ACTION_EXECUTION_ERROR", replacements, player);
        }
        
        
        boolean hideParticlesBool = Boolean.parseBoolean(hideParticles);
        
        
        String command = String.format("effect give %s %s %d %d %s", 
            targetPlayer, normalizedEffect, durationSeconds, amplifierLevel, hideParticlesBool);
        
        boolean success = commandExecutor.executeAsConsole(command);
        
        if (success) {
            String effectDisplayName = getEffectDisplayName(normalizedEffect);
            String message = String.format("Successfully gave %s effect %s (Level %d) for %s seconds to %s", 
                effectDisplayName, normalizedEffect, amplifierLevel + 1, durationSeconds, targetPlayer);
            logger.debug(message);
            
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("message", message);
            return createSuccessResult("ACTION_SUCCESS", replacements, player);
        } else {
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("error", "Failed to give potion effect. Command execution failed.");
            return createFailureResult("ACTION_EXECUTION_ERROR", replacements, player);
        }
    }
    
    private ActionResult handleRemoveEffect(String targetPlayer, String effectName, FormPlayer player) {
        
        String normalizedEffect = normalizeEffectName(effectName);
        if (normalizedEffect == null) {
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("error", "Invalid potion effect: " + effectName);
            return createFailureResult("ACTION_EXECUTION_ERROR", replacements, player);
        }
        
        String command = String.format("effect clear %s %s", targetPlayer, normalizedEffect);
        boolean success = commandExecutor.executeAsConsole(command);
        
        if (success) {
            String effectDisplayName = getEffectDisplayName(normalizedEffect);
            String message = String.format("Successfully removed %s effect from %s", effectDisplayName, targetPlayer);
            logger.debug(message);
            
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("message", message);
            return createSuccessResult("ACTION_SUCCESS", replacements, player);
        } else {
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("error", "Failed to remove potion effect. Command execution failed.");
            return createFailureResult("ACTION_EXECUTION_ERROR", replacements, player);
        }
    }
    
    private ActionResult handleClearAllEffects(String targetPlayer, FormPlayer player) {
        String command = String.format("effect clear %s", targetPlayer);
        boolean success = commandExecutor.executeAsConsole(command);
        
        if (success) {
            String message = String.format("Successfully cleared all potion effects from %s", targetPlayer);
            logger.debug(message);
            
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("message", message);
            return createSuccessResult("ACTION_SUCCESS", replacements, player);
        } else {
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("error", "Failed to clear all potion effects. Command execution failed.");
            return createFailureResult("ACTION_EXECUTION_ERROR", replacements, player);
        }
    }
    
    private ActionResult handleListEffects(String targetPlayer, FormPlayer player) {
        
        
        String command = String.format("data get entity %s ActiveEffects", targetPlayer);
        boolean success = commandExecutor.executeAsConsole(command);
        
        if (success) {
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("message", "Listed active potion effects for " + targetPlayer);
            return createSuccessResult("ACTION_SUCCESS", replacements, player);
        } else {
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("error", "Failed to list potion effects. Command execution failed.");
            return createFailureResult("ACTION_EXECUTION_ERROR", replacements, player);
        }
    }
    
    private String normalizeEffectName(String effectName) {
        if (effectName == null || effectName.trim().isEmpty()) {
            return null;
        }
        
        String normalized = effectName.toLowerCase().trim();
        
        
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
        
        
        effectAliases.put("regen", "minecraft:regeneration");
        effectAliases.put("invis", "minecraft:invisibility");
        effectAliases.put("nv", "minecraft:night_vision");
        effectAliases.put("fr", "minecraft:fire_resistance");
        effectAliases.put("wb", "minecraft:water_breathing");
        effectAliases.put("heal", "minecraft:instant_health");
        effectAliases.put("damage", "minecraft:instant_damage");
        effectAliases.put("harm", "minecraft:instant_damage");
        
        
        if (effectAliases.containsKey(normalized)) {
            return effectAliases.get(normalized);
        }
        
        
        if (normalized.startsWith("minecraft:")) {
            return normalized;
        }
        
        
        String withPrefix = "minecraft:" + normalized;
        if (isValidMinecraftEffect(normalized)) {
            return withPrefix;
        }
        
        return null;
    }
    
    private boolean isValidMinecraftEffect(String effectName) {
        
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
            String name = effectName.substring(10); 
            return name.substring(0, 1).toUpperCase() + name.substring(1).replace("_", " ");
        }
        return effectName;
    }
    
    private int parseDuration(String duration) throws NumberFormatException {
        if (duration == null || duration.isEmpty()) {
            return 30; 
        }
        
        
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
            
            return Integer.parseInt(duration);
        }
    }
    
    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }
        
        
        List<String> operations = parseActionDataForValidation(actionValue);
        
        for (String operation : operations) {
            String[] parts = operation.split(":", 3);
            if (parts.length < 3) {
                return false;
            }
            
            String operationType = parts[0].toLowerCase();
            if (!isValidOperation(operationType)) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean isValidOperation(String operation) {
        return operation.equals("give") || operation.equals("add") || operation.equals("apply") ||
               operation.equals("remove") || operation.equals("clear") ||
               operation.equals("clearall") || operation.equals("removeall") ||
               operation.equals("check") || operation.equals("list");
    }
    
    @Override
    public String getDescription() {
        return "Manages potion effects for players including giving, removing, and clearing effects. Supports all Minecraft potion effects with customizable duration and amplifier. Can execute single or multiple potion operations with sequential processing.";
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
            "potion:check:%player_name%:any - List all active effects on current player",
            
            
            "[\"potion:give:%player_name%:speed:60:1\", \"potion:give:%player_name%:strength:60:0\"] - Give speed and strength",
            "[\"potion:clearall:%player_name%\", \"potion:give:%player_name%:regeneration:30:1\"] - Clear all effects then give regeneration",
            "[\"potion:give:Player1:speed:30\", \"potion:give:Player2:jump_boost:30\", \"potion:give:Player3:strength:30\"] - Give different effects to multiple players"
        };
    }
}
