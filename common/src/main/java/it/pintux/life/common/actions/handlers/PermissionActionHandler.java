package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.platform.PlatformCommandExecutor;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.PlaceholderUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Action handler for managing player permissions
 * Supports granting, removing, and temporarily managing permissions
 */
public class PermissionActionHandler extends BaseActionHandler {
    private final PlatformCommandExecutor commandExecutor;
    private final ScheduledExecutorService scheduler;
    private final Map<String, ScheduledTask> temporaryPermissions;
    
    public PermissionActionHandler(PlatformCommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread thread = new Thread(r, "PermissionHandler-" + System.currentTimeMillis());
            thread.setDaemon(true);
            return thread;
        });
        this.temporaryPermissions = new ConcurrentHashMap<>();
    }
    
    @Override
    public String getActionType() {
        return "permission";
    }
    
    @Override
    public ActionResult execute(FormPlayer player, String actionData, ActionContext context) {
        ActionResult validation = validateBasicParameters(player, actionData);
        if (validation != null) {
            return validation;
        }
        
        try {
            // Process placeholders in the action data
            String processedData = processPlaceholders(actionData.trim(), context, player);
            String[] parts = processedData.split(":", 4);
            
            if (parts.length < 3) {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid permission action format. Expected: operation:player:permission[:duration]"), player);
            }
            
            String operation = parts[0].toLowerCase();
            String targetPlayer = parts[1];
            String permission = parts[2];
            String duration = parts.length > 3 ? parts[3] : null;
            
            // Replace %player_name% placeholder if used
            if (targetPlayer.equals("%player_name%") || targetPlayer.equals("@s")) {
                targetPlayer = player.getName();
            }
            
            switch (operation) {
                case "grant":
                case "give":
                case "add":
                    return handleGrantPermission(targetPlayer, permission, duration, player);
                    
                case "remove":
                case "revoke":
                case "take":
                    return handleRemovePermission(targetPlayer, permission, player);
                    
                case "check":
                case "test":
                    return handleCheckPermission(targetPlayer, permission, player);
                    
                case "temp":
                case "temporary":
                    if (duration == null) {
                        return ActionResult.failure("Duration is required for temporary permissions");
                    }
                    return handleTemporaryPermission(targetPlayer, permission, duration, player);
                    
                case "group":
                    return handleGroupOperation(targetPlayer, permission, duration, player);
                    
                default:
                    logger.warn("Unknown permission operation: " + operation + " for player: " + player.getName());
                    return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Unknown permission operation: " + operation), player);
            }
            
        } catch (Exception e) {
            logger.error("Error executing permission action for player " + player.getName(), e);
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Error executing permission action: " + e.getMessage()), player);
        }
    }
    
    private ActionResult handleGrantPermission(String targetPlayer, String permission, String duration, FormPlayer player) {
        String command;
        
        // Try LuckPerms first, then fallback to other permission plugins
        if (duration != null) {
            // Temporary permission with LuckPerms
            command = "lp user " + targetPlayer + " permission settemp " + permission + " true " + duration;
        } else {
            // Permanent permission with LuckPerms
            command = "lp user " + targetPlayer + " permission set " + permission + " true";
        }
        
        boolean success = commandExecutor.executeAsConsole(command);
        
        if (!success) {
            // Fallback to other permission plugins
            command = "pex user " + targetPlayer + " add " + permission;
            success = commandExecutor.executeAsConsole(command);
            
            if (!success) {
                // Fallback to GroupManager
                command = "manuadd " + targetPlayer + " " + permission;
                success = commandExecutor.executeAsConsole(command);
            }
        }
        
        if (success) {
            logger.debug("Successfully granted permission " + permission + " to player " + targetPlayer);
            String message = duration != null ? 
                "Successfully granted temporary permission " + permission + " to " + targetPlayer + " for " + duration :
                "Successfully granted permission " + permission + " to " + targetPlayer;
            return createSuccessResult("ACTION_SUCCESS", createReplacements("message", message), player);
        } else {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to grant permission. No compatible permission plugin found."), player);
        }
    }
    
    private ActionResult handleRemovePermission(String targetPlayer, String permission, FormPlayer player) {
        String command;
        
        // Try LuckPerms first
        command = "lp user " + targetPlayer + " permission unset " + permission;
        boolean success = commandExecutor.executeAsConsole(command);
        
        if (!success) {
            // Fallback to PermissionsEx
            command = "pex user " + targetPlayer + " remove " + permission;
            success = commandExecutor.executeAsConsole(command);
            
            if (!success) {
                // Fallback to GroupManager
                command = "manudel " + targetPlayer + " " + permission;
                success = commandExecutor.executeAsConsole(command);
            }
        }
        
        if (success) {
            logger.debug("Successfully removed permission " + permission + " from player " + targetPlayer);
            return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Successfully removed permission " + permission + " from " + targetPlayer), player);
        } else {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to remove permission. No compatible permission plugin found."), player);
        }
    }
    
    private ActionResult handleCheckPermission(String targetPlayer, String permission, FormPlayer player) {
        // This would typically require platform-specific implementation
        // For now, we'll use LuckPerms check command
        String command = "lp user " + targetPlayer + " permission check " + permission;
        boolean hasPermission = commandExecutor.executeAsConsole(command);
        
        String message = hasPermission ? 
            "Player " + targetPlayer + " has permission " + permission :
            "Player " + targetPlayer + " does not have permission " + permission;
            
        return createSuccessResult("ACTION_SUCCESS", createReplacements("message", message), player);
    }
    
    private ActionResult handleTemporaryPermission(String targetPlayer, String permission, String duration, FormPlayer player) {
        // Parse duration (e.g., "30s", "5m", "1h", "1d")
        long durationMillis = parseDuration(duration);
        if (durationMillis <= 0) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid duration format. Use format like: 30s, 5m, 1h, 1d"), player);
        }
        
        // Grant the permission first
        ActionResult grantResult = handleGrantPermission(targetPlayer, permission, duration, player);
        if (grantResult.isFailure()) {
            return grantResult;
        }
        
        // Schedule removal
        String taskKey = targetPlayer + ":" + permission;
        
        // Cancel existing task if any
        ScheduledTask existingTask = temporaryPermissions.get(taskKey);
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        // Schedule new removal task
        ScheduledTask task = new ScheduledTask(
            scheduler.schedule(() -> {
                handleRemovePermission(targetPlayer, permission, player);
                temporaryPermissions.remove(taskKey);
                logger.info("Automatically removed temporary permission " + permission + " from " + targetPlayer);
            }, durationMillis, TimeUnit.MILLISECONDS)
        );
        
        temporaryPermissions.put(taskKey, task);
        
        return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Successfully granted temporary permission " + permission + " to " + targetPlayer + " for " + duration), player);
    }
    
    private ActionResult handleGroupOperation(String targetPlayer, String group, String operation, FormPlayer player) {
        String command;
        
        if (operation == null || operation.equalsIgnoreCase("add") || operation.equalsIgnoreCase("join")) {
            // Add to group
            command = "lp user " + targetPlayer + " parent add " + group;
        } else if (operation.equalsIgnoreCase("remove") || operation.equalsIgnoreCase("leave")) {
            // Remove from group
            command = "lp user " + targetPlayer + " parent remove " + group;
        } else {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid group operation. Use 'add' or 'remove'"), player);
        }
        
        boolean success = commandExecutor.executeAsConsole(command);
        
        if (!success) {
            // Fallback to other permission plugins
            if (operation == null || operation.equalsIgnoreCase("add")) {
                command = "pex user " + targetPlayer + " group add " + group;
            } else {
                command = "pex user " + targetPlayer + " group remove " + group;
            }
            success = commandExecutor.executeAsConsole(command);
        }
        
        if (success) {
            String action = (operation == null || operation.equalsIgnoreCase("add")) ? "added to" : "removed from";
            return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Successfully " + action + " group " + group + " for player " + targetPlayer), player);
        } else {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to modify group membership. No compatible permission plugin found."), player);
        }
    }
    
    private long parseDuration(String duration) {
        if (duration == null || duration.isEmpty()) {
            return 0;
        }
        
        try {
            String timeUnit = duration.substring(duration.length() - 1).toLowerCase();
            long value = Long.parseLong(duration.substring(0, duration.length() - 1));
            
            switch (timeUnit) {
                case "s": return value * 1000;
                case "m": return value * 60 * 1000;
                case "h": return value * 60 * 60 * 1000;
                case "d": return value * 24 * 60 * 60 * 1000;
                default: return Long.parseLong(duration) * 1000; // Assume seconds if no unit
            }
        } catch (NumberFormatException e) {
            return 0;
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
        return operation.equals("grant") || operation.equals("give") || operation.equals("add") ||
               operation.equals("remove") || operation.equals("revoke") || operation.equals("take") ||
               operation.equals("check") || operation.equals("test") ||
               operation.equals("temp") || operation.equals("temporary") ||
               operation.equals("group");
    }
    
    @Override
    public String getDescription() {
        return "Manages player permissions including granting, removing, checking, and temporary permissions. Supports LuckPerms, PermissionsEx, and GroupManager.";
    }
    
    @Override
    public String[] getUsageExamples() {
        return new String[]{
            "permission:grant:%player_name%:bedrockgui.admin - Grant admin permission to current player",
            "permission:remove:PlayerName:some.permission - Remove permission from specific player",
            "permission:check:%player_name%:bedrockgui.use - Check if current player has permission",
            "permission:temp:%player_name%:fly.use:1h - Grant temporary fly permission for 1 hour",
            "permission:group:%player_name%:vip:add - Add player to VIP group",
            "permission:group:%player_name%:default:remove - Remove player from default group"
        };
    }
    
    /**
     * Cleanup method to cancel all scheduled tasks
     */
    public void shutdown() {
        temporaryPermissions.values().forEach(ScheduledTask::cancel);
        temporaryPermissions.clear();
        scheduler.shutdown();
    }
    
    private static class ScheduledTask {
        private final java.util.concurrent.ScheduledFuture<?> future;
        
        public ScheduledTask(java.util.concurrent.ScheduledFuture<?> future) {
            this.future = future;
        }
        
        public void cancel() {
            if (future != null && !future.isDone()) {
                future.cancel(false);
            }
        }
    }
}
