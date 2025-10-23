package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.platform.PlatformCommandExecutor;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.ValidationUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class PermissionActionHandler extends BaseActionHandler {
    
    private final PlatformCommandExecutor commandExecutor;
    private final Map<String, Long> temporaryPermissions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    public PermissionActionHandler(PlatformCommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }
    
    @Override
    public String getActionType() {
        return "permission";
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
                Map<String, Object> errorReplacements = createReplacements("error", "No valid permission operations found");
                return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
            }
            
            
            if (operations.size() == 1) {
                return executeSinglePermissionOperation(operations.get(0), player);
            }
            
            
            return executeMultiplePermissionOperations(operations, player);
            
        } catch (Exception e) {
            logError("permission operation", actionData, player, e);
            Map<String, Object> errorReplacements = createReplacements("error", "Error executing permission operation: " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player, e);
        }
    }
    
    private ActionResult executeSinglePermissionOperation(String operationData, FormPlayer player) {
        try {
            logger.info("Executing permission operation: " + operationData + " for player " + player.getName());
            
            Map<String, Object> permissionData = parsePermissionData(operationData);
            
            if (permissionData.isEmpty()) {
                Map<String, Object> errorReplacements = createReplacements("error", "No valid permission data found");
                return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
            }
            
            
            boolean success = executeWithErrorHandling(
                () -> performPermissionOperation(permissionData, player),
                "Permission operation: " + permissionData.get("operation"),
                player
            );
            
            if (success) {
                logSuccess("permission operation", String.valueOf(permissionData.get("operation")), player);
                Map<String, Object> replacements = new HashMap<>();
                replacements.put("message", "Permission operation completed successfully");
                return createSuccessResult("ACTION_SUCCESS", replacements, player);
            } else {
                Map<String, Object> errorReplacements = createReplacements("error", "Failed to execute permission operation");
                return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
            }
            
        } catch (Exception e) {
            logError("permission operation", operationData, player, e);
            Map<String, Object> errorReplacements = createReplacements("error", "Error executing permission operation: " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player, e);
        }
    }
    
    private ActionResult executeMultiplePermissionOperations(List<String> operations, FormPlayer player) {
        int successCount = 0;
        int totalCount = operations.size();
        StringBuilder results = new StringBuilder();
        
        for (int i = 0; i < operations.size(); i++) {
            String operationData = operations.get(i);
            
            try {
                logger.info("Executing permission operation " + (i + 1) + "/" + totalCount + ": " + operationData + " for player " + player.getName());
                
                Map<String, Object> permissionData = parsePermissionData(operationData);
                
                if (permissionData.isEmpty()) {
                    results.append("âś— Operation ").append(i + 1).append(": ").append(operationData).append(" - Invalid data");
                    continue;
                }
                
                boolean success = executeWithErrorHandling(
                    () -> performPermissionOperation(permissionData, player),
                    "Permission operation: " + permissionData.get("operation"),
                    player
                );
                
                if (success) {
                    successCount++;
                    results.append("âś“ Operation ").append(i + 1).append(": ").append(permissionData.get("operation")).append(" - Success");
                    logSuccess("permission operation", String.valueOf(permissionData.get("operation")), player);
                } else {
                    results.append("âś— Operation ").append(i + 1).append(": ").append(permissionData.get("operation")).append(" - Failed");
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
                results.append("âś— Operation ").append(i + 1).append(": ").append(operationData).append(" - Error: ").append(e.getMessage());
                logError("permission operation", operationData, player, e);
                if (i < operations.size() - 1) {
                    results.append("\n");
                }
            }
        }
        
        String finalMessage = String.format("Executed %d/%d permission operations successfully:\n%s", 
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
    
    private boolean performPermissionOperation(Map<String, Object> permissionData, FormPlayer player) {
        String operation = getStringValue(permissionData, "operation", "grant");
        String targetPlayer = getStringValue(permissionData, "player", player.getName());
        String permission = getStringValue(permissionData, "permission", "");
        String duration = getStringValue(permissionData, "duration", null);
        String group = getStringValue(permissionData, "group", null);
        
        if (permission.isEmpty() && group == null) {
            return false;
        }
        
        
        if (targetPlayer.equals("%player_name%") || targetPlayer.equals("@s")) {
            targetPlayer = player.getName();
        }
        
        
        if (group != null) {
            return handleGroupOperation(targetPlayer, group, operation, player);
        }
        
        return executePermissionOperation(operation, targetPlayer, permission, duration, player);
    }
    
    private boolean handleGroupOperation(String targetPlayer, String group, String operation, FormPlayer player) {
        try {
            String command;
            switch (operation.toLowerCase()) {
                case "grant":
                case "give":
                case "add":
                    command = "lp user " + targetPlayer + " parent add " + group;
                    break;
                case "remove":
                case "revoke":
                case "take":
                    command = "lp user " + targetPlayer + " parent remove " + group;
                    break;
                case "check":
                case "test":
                    command = "lp user " + targetPlayer + " parent info";
                    break;
                default:
                    logger.warn("Unknown group operation: " + operation);
                    return false;
            }
            
            commandExecutor.executeAsConsole(command);
            return true;
        } catch (Exception e) {
            logger.error("Error executing group operation: " + e.getMessage(), e);
            return false;
        }
    }
    
    private boolean executePermissionOperation(String operation, String targetPlayer, String permission, String duration, FormPlayer player) {
        try {
            String command;
            switch (operation.toLowerCase()) {
                case "grant":
                case "give":
                case "add":
                    if (duration != null) {
                        command = "lp user " + targetPlayer + " permission settemp " + permission + " true " + duration;
                        schedulePermissionRemoval(targetPlayer, permission, parseDuration(duration));
                    } else {
                        command = "lp user " + targetPlayer + " permission set " + permission + " true";
                    }
                    break;
                case "remove":
                case "revoke":
                case "take":
                    command = "lp user " + targetPlayer + " permission unset " + permission;
                    break;
                case "check":
                case "test":
                    command = "lp user " + targetPlayer + " permission check " + permission;
                    break;
                case "temp":
                case "temporary":
                    if (duration == null) {
                        duration = "1h"; 
                    }
                    command = "lp user " + targetPlayer + " permission settemp " + permission + " true " + duration;
                    schedulePermissionRemoval(targetPlayer, permission, parseDuration(duration));
                    break;
                default:
                    logger.warn("Unknown permission operation: " + operation);
                    return false;
            }
            
            commandExecutor.executeAsConsole(command);
            return true;
        } catch (Exception e) {
            logger.error("Error executing permission operation: " + e.getMessage(), e);
            return false;
        }
    }
    
    private void schedulePermissionRemoval(String player, String permission, long durationMillis) {
        String key = player + ":" + permission;
        temporaryPermissions.put(key, System.currentTimeMillis() + durationMillis);
        
        scheduler.schedule(() -> {
            temporaryPermissions.remove(key);
            try {
                String command = "lp user " + player + " permission unset " + permission;
                commandExecutor.executeAsConsole(command);
                logger.info("Removed temporary permission " + permission + " from player " + player);
            } catch (Exception e) {
                logger.error("Error removing temporary permission: " + e.getMessage(), e);
            }
        }, durationMillis, TimeUnit.MILLISECONDS);
    }
    
    private long parseDuration(String duration) {
        if (duration == null || duration.isEmpty()) {
            return 3600000; 
        }
        
        try {
            String unit = duration.substring(duration.length() - 1).toLowerCase();
            int value = Integer.parseInt(duration.substring(0, duration.length() - 1));
            
            switch (unit) {
                case "s": return value * 1000L;
                case "m": return value * 60000L;
                case "h": return value * 3600000L;
                case "d": return value * 86400000L;
                default: return Long.parseLong(duration) * 1000L; 
            }
        } catch (Exception e) {
            logger.warn("Invalid duration format: " + duration + ", using default 1 hour");
            return 3600000;
        }
    }
    
    private Map<String, Object> parsePermissionData(String operationData) {
        Map<String, Object> data = new HashMap<>();
        
        if (operationData == null || operationData.trim().isEmpty()) {
            return data;
        }
        
        
        String[] parts = operationData.split(":", 4);
        if (parts.length >= 3) {
            data.put("operation", parts[0]);
            data.put("player", parts[1]);
            data.put("permission", parts[2]);
            if (parts.length > 3) {
                data.put("duration", parts[3]);
            }
        }
        
        return data;
    }
    
    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }
        
        
        List<String> operations = parseActionDataForValidation(actionValue);
        
        for (String operation : operations) {
            if (!isValidPermissionOperation(operation)) {
                return false;
            }
        }
        
        return !operations.isEmpty();
    }
    
    private boolean isValidPermissionOperation(String operation) {
        if (operation == null || operation.trim().isEmpty()) {
            return false;
        }
        
        try {
            Map<String, Object> permissionData = parsePermissionData(operation);
            if (permissionData.isEmpty() || !permissionData.containsKey("operation")) {
                return false;
            }
            
            String op = permissionData.get("operation").toString().toLowerCase();
            return op.equals("grant") || op.equals("give") || op.equals("add") ||
                   op.equals("remove") || op.equals("revoke") || op.equals("take") ||
                   op.equals("check") || op.equals("test") ||
                   op.equals("temp") || op.equals("temporary") ||
                   op.equals("group");
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String getDescription() {
        return "Handles permission operations including grant, remove, check, temporary permissions, and group management. Supports multiple operations with sequential execution and enhanced error handling.";
    }
    
    @Override
    public String[] getUsageExamples() {
        return new String[]{
            
            "grant:player:permission.node - Grant permission to player",
            "remove:player:permission.node - Remove permission from player",
            "check:player:permission.node - Check if player has permission",
            "temp:player:permission.node:1h - Grant temporary permission for 1 hour",
            "group:player:vip:grant - Add player to VIP group",
            "group:player:admin:remove - Remove player from admin group",
            
            
            "[\"grant:player:perm1\", \"grant:player:perm2\"] - Grant multiple permissions",
            "[\"remove:player:oldperm\", \"grant:player:newperm\"] - Replace permission",
            "[\"group:player:member:remove\", \"group:player:vip:grant\"] - Change group membership",
            "[\"temp:player:fly:30m\", \"temp:player:speed:30m\"] - Multiple temporary permissions"
        };
    }
    
    
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

