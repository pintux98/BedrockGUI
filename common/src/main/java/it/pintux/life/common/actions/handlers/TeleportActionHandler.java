package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.ValidationUtils;
import it.pintux.life.common.utils.PlaceholderUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


public class TeleportActionHandler extends BaseActionHandler {
    
    private static final Pattern COORDINATE_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?$");
    private static final double MAX_COORDINATE = 30000000;
    private static final double MIN_Y = -64;
    private static final double MAX_Y = 320;
    
    @Override
    public String getActionType() {
        return "teleport";
    }
    
    @Override
    public ActionResult execute(FormPlayer player, String actionData, ActionContext context) {
        
        ActionResult validationResult = validateBasicParameters(player, actionData);
        if (validationResult != null) {
            return validationResult;
        }
        
        try {
            
            if (isNewCurlyBraceFormat(actionData, "teleport")) {
                return executeNewFormat(player, actionData, context);
            }
            
            
            List<String> teleportTargets = parseActionData(actionData, context, player);
            
            if (teleportTargets.isEmpty()) {
                Map<String, Object> errorReplacements = createReplacements("error", "No valid teleport targets found");
                return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
            }
            
            
            if (teleportTargets.size() == 1) {
                return executeSingleTeleport(teleportTargets.get(0), player);
            }
            
            
            return executeMultipleTeleports(teleportTargets, player);
            
        } catch (Exception e) {
            logError("teleport execution", actionData, player, e);
            Map<String, Object> errorReplacements = createReplacements("error", "Error executing teleport: " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player, e);
        }
    }
    
    
    private ActionResult executeNewFormat(FormPlayer player, String actionData, ActionContext context) {
        try {
            List<String> teleportTargets = parseNewFormatValues(actionData);
            
            if (teleportTargets.isEmpty()) {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "No teleport targets found in new format"), player);
            }
            
            
            List<String> processedTargets = new ArrayList<>();
            for (String target : teleportTargets) {
                String processedTarget = processPlaceholders(target, context, player);
                processedTargets.add(processedTarget);
            }
            
            
            if (processedTargets.size() == 1) {
                return executeSingleTeleport(processedTargets.get(0), player);
            }
            
            
            return executeMultipleTeleports(processedTargets, player);
            
        } catch (Exception e) {
            logger.error("Error executing new format teleport action for player " + player.getName() + ": " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Error parsing new teleport format: " + e.getMessage()), player);
        }
    }
    
    private ActionResult executeSingleTeleport(String target, FormPlayer player) {
        try {
            logger.info("Executing teleport: " + target + " for player " + player.getName());
            
            TeleportInfo teleportInfo = parseTeleportTarget(target);
            if (teleportInfo == null) {
                Map<String, Object> errorReplacements = createReplacements("error", "Invalid teleport target: " + target);
                return createFailureResult("ACTION_INVALID_FORMAT", errorReplacements, player);
            }
            
            
            boolean success = executeWithErrorHandling(
                () -> performTeleport(player, teleportInfo),
                "Teleport to: " + target,
                player
            );
            
            if (success) {
                logSuccess("teleport", teleportInfo.getDescription(), player);
                Map<String, Object> replacements = new HashMap<>();
                replacements.put("message", "Teleported successfully to " + teleportInfo.getDescription());
                replacements.putAll(teleportInfo.getReplacements());
                return createSuccessResult("ACTION_SUCCESS", replacements, player);
            } else {
                Map<String, Object> errorReplacements = new HashMap<>();
                errorReplacements.put("error", "Failed to teleport to " + teleportInfo.getDescription());
                return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
            }
            
        } catch (Exception e) {
            logError("teleport execution", target, player, e);
            Map<String, Object> errorReplacements = new HashMap<>();
            errorReplacements.put("error", "Error executing teleport: " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player, e);
        }
    }
    
    private ActionResult executeMultipleTeleports(List<String> targets, FormPlayer player) {
        int successCount = 0;
        int totalCount = targets.size();
        StringBuilder results = new StringBuilder();
        
        for (int i = 0; i < targets.size(); i++) {
            String target = targets.get(i);
            
            try {
                logger.info("Executing teleport " + (i + 1) + "/" + totalCount + ": " + target + " for player " + player.getName());
                
                TeleportInfo teleportInfo = parseTeleportTarget(target);
                if (teleportInfo == null) {
                    results.append("âś— Teleport ").append(i + 1).append(": ").append(target).append(" - Invalid target");
                    continue;
                }
                
                boolean success = executeWithErrorHandling(
                    () -> performTeleport(player, teleportInfo),
                    "Teleport to: " + target,
                    player
                );
                
                if (success) {
                    successCount++;
                    results.append("âś“ Teleport ").append(i + 1).append(": ").append(teleportInfo.getDescription()).append(" - Success");
                    logSuccess("teleport", teleportInfo.getDescription(), player);
                } else {
                    results.append("âś— Teleport ").append(i + 1).append(": ").append(teleportInfo.getDescription()).append(" - Failed");
                }
                
                if (i < targets.size() - 1) {
                    results.append("\n");
                    
                    try {
                        Thread.sleep(1000); 
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
            } catch (Exception e) {
                results.append("âś— Teleport ").append(i + 1).append(": ").append(target).append(" - Error: ").append(e.getMessage());
                logError("teleport execution", target, player, e);
                if (i < targets.size() - 1) {
                    results.append("\n");
                }
            }
        }
        
        String finalMessage = String.format("Executed %d/%d teleports successfully:\n%s", 
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
    
    private TeleportInfo parseTeleportTarget(String target) {
        target = target.trim();
        
        
        if (!target.contains(" ")) {
            if (target.equalsIgnoreCase("spawn")) {
                return new TeleportInfo("spawn", "spawn location", null);
            } else if (target.equalsIgnoreCase("home")) {
                return new TeleportInfo("home", "home location", null);
            } else if (target.startsWith("warp:")) {
                String warpName = target.substring(5);
                return new TeleportInfo("warp " + warpName, "warp " + warpName, Map.of("warp", warpName));
            }
            return null;
        }
        
        
        String[] parts = target.split("\\s+");
        if (parts.length != 3) {
            return null;
        }
        
        try {
            
            for (String coord : parts) {
                if (!COORDINATE_PATTERN.matcher(coord).matches()) {
                    return null;
                }
            }
            
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            
            
            if (Math.abs(x) > MAX_COORDINATE || Math.abs(z) > MAX_COORDINATE) {
                return null;
            }
            
            if (y < MIN_Y || y > MAX_Y) {
                return null;
            }
            
            String description = String.format("%.2f, %.2f, %.2f", x, y, z);
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("x", String.valueOf(x));
            replacements.put("y", String.valueOf(y));
            replacements.put("z", String.valueOf(z));
            
            return new TeleportInfo(String.format("%.2f %.2f %.2f", x, y, z), description, replacements);
            
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private boolean performTeleport(FormPlayer player, TeleportInfo teleportInfo) {
        try {
            String command = teleportInfo.getCommand();
            if (command.startsWith("spawn") || command.startsWith("home") || command.startsWith("warp")) {
                
                player.executeAction("/" + command);
            } else {
                
                String teleportCommand = String.format("tp %s %s", player.getName(), command);
                player.executeAction("/" + teleportCommand);
            }
            return true; 
        } catch (Exception e) {
            logger.error("Failed to perform teleport: " + e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }
        
        
        List<String> targets = parseActionDataForValidation(actionValue);
        
        for (String target : targets) {
            if (!isValidTeleportTarget(target)) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean isValidTeleportTarget(String target) {
        if (ValidationUtils.isNullOrEmpty(target)) {
            return false;
        }
        
        String trimmed = target.trim();
        
        
        if (containsDynamicPlaceholders(trimmed)) {
            return true;
        }
        
        
        if (!trimmed.contains(" ")) {
            return trimmed.equalsIgnoreCase("spawn") || 
                   trimmed.equalsIgnoreCase("home") || 
                   trimmed.startsWith("warp:");
        }
        
        
        String[] parts = trimmed.split("\\s+");
        if (parts.length != 3) {
            return false;
        }
        
        for (String part : parts) {
            if (!COORDINATE_PATTERN.matcher(part).matches()) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        return "Teleports the player to specified coordinates or named locations. Supports multiple teleports with sequential execution and delays between operations.";
    }
    
    @Override
    public String[] getUsageExamples() {
        return new String[]{
            
            "0 100 0 - Teleport to coordinates",
            "100.5 64 -200.3 - Teleport to precise coordinates",
            "spawn - Teleport to spawn location",
            "home - Teleport to home location",
            "warp:pvp - Teleport to PvP warp",
            "{spawn_x} {spawn_y} {spawn_z} - Dynamic coordinates",
            
            
            "[\"spawn\", \"0 100 0\"] - Spawn then coordinates",
            "[\"home\", \"warp:shop\", \"warp:pvp\"] - Multiple named locations",
            "[\"0 100 0\", \"100 64 100\", \"spawn\"] - Coordinate sequence with spawn",
            "[\"warp:lobby\", \"{selected_x} 100 {selected_z}\"] - Mixed locations"
        };
    }
    
    
    private static class TeleportInfo {
        private final String command;
        private final String description;
        private final Map<String, Object> replacements;
        
        public TeleportInfo(String command, String description, Map<String, Object> replacements) {
            this.command = command;
            this.description = description;
            this.replacements = replacements != null ? replacements : new HashMap<>();
        }
        
        public String getCommand() {
            return command;
        }
        
        public String getDescription() {
            return description;
        }
        
        public Map<String, Object> getReplacements() {
            return replacements;
        }
    }
}
