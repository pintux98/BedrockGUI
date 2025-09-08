package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.actions.ActionExecutor;
import it.pintux.life.common.utils.FormPlayer;


import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Handles delays in action execution.
 * Useful for creating timed sequences or adding pauses between actions.
 * Runs asynchronously to avoid blocking the main server thread.
 * 
 * Usage: delay:1000 (delay for 1000 milliseconds = 1 second)
 * Usage: delay:5000 (delay for 5 seconds)
 * Usage: delay:500 (delay for 0.5 seconds)
 */
public class DelayActionHandler extends BaseActionHandler {

    private static final long MAX_DELAY_MS = 30000; // Maximum 30 seconds delay
    private static final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "DelayActionHandler-" + System.currentTimeMillis());
        thread.setDaemon(true);
        return thread;
    });
    
    private final ActionExecutor actionExecutor;
    
    public DelayActionHandler(ActionExecutor actionExecutor) {
        this.actionExecutor = actionExecutor;
    }

    /**
     * Shuts down the executor service to prevent resource leaks
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    @Override
    public String getActionType() {
        return "delay";
    }
    
    @Override
    public ActionResult execute(FormPlayer player, String actionData, ActionContext context) {
        if (actionData == null || actionData.trim().isEmpty()) {
            logger.warn("Delay action called with empty delay time for player: " + player.getName());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "ACTION_INVALID_PARAMETERS"), player);
        }
        
        try {
            // Process placeholders in the action data
            String processedData = processPlaceholders(actionData.trim(), context);
            
            // Parse delay time and optional chained action
            String[] parts = processedData.split(":", 2);
            long delayMs = Long.parseLong(parts[0]);
            String chainedAction = parts.length > 1 ? parts[1] : null;
            
            if (delayMs < 0) {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Delay time cannot be negative"), player);
            }
            
            if (delayMs > MAX_DELAY_MS) {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Delay time cannot exceed " + MAX_DELAY_MS + "ms (30 seconds)"), player);
            }
            
            if (delayMs == 0) {
                // Execute chained action immediately if delay is 0
                if (chainedAction != null && !chainedAction.trim().isEmpty()) {
                    return executeChainedAction(player, chainedAction, context);
                }
                return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "No delay applied"), player);
            }
            
            logger.info("Applying delay of " + delayMs + "ms for player " + player.getName());
            
            // Schedule the delay and optional chained action asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(delayMs);
                    
                    // Execute chained action after delay if present
                    if (chainedAction != null && !chainedAction.trim().isEmpty()) {
                        executeChainedAction(player, chainedAction, context);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Delay was interrupted for player " + player.getName());
                } catch (Exception e) {
                    logger.error("Error executing chained action after delay for player " + player.getName() + ": " + e.getMessage());
                }
            }, executorService);
            
            // Return immediately - don't block the main thread
            String message = "Delay of " + delayMs + "ms scheduled";
            if (chainedAction != null && !chainedAction.trim().isEmpty()) {
                message += " with chained action";
            }
            return createSuccessResult("ACTION_SUCCESS", createReplacements("message", message), player);
            
        } catch (NumberFormatException e) {
            logger.warn("Invalid delay time format for player " + player.getName() + ": " + actionData);
            return createFailureResult("ACTION_INVALID_PARAMETERS", createReplacements("error", "Invalid delay time format: " + actionData), player);
        } catch (Exception e) {
            logger.error("Error executing delay action for player " + player.getName() + ": " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Error executing delay: " + e.getMessage()), player);
        }
    }
    
    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }
        
        try {
            long delayMs = Long.parseLong(actionValue.trim());
            return delayMs >= 0 && delayMs <= MAX_DELAY_MS;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    @Override
    public String getDescription() {
        return "Adds delays between actions or creates timed sequences (max 30 seconds)";
    }
    
    @Override
    public String[] getUsageExamples() {
        return new String[]{
            "delay:1000",
            "delay:5000",
            "delay:500",
            "delay:2500"
        };
    }
    
    private String processPlaceholders(String data, ActionContext context) {
        if (context == null) {
            return data;
        }
        
        String result = data;
        Map<String, String> placeholders = context.getPlaceholders();
        
        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                String value = entry.getValue() != null ? entry.getValue() : "";
                result = result.replace(placeholder, value);
            }
        }
        
        // Process form results as placeholders
        Map<String, Object> formResults = context.getFormResults();
        if (formResults != null && !formResults.isEmpty()) {
            for (Map.Entry<String, Object> entry : formResults.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                result = result.replace(placeholder, value);
            }
        }
        
        return result;
    }
    
    /**
     * Executes a chained action after a delay
     * @param player The player to execute the action for
     * @param chainedAction The action string to execute
     * @param context The action context
     * @return The result of the chained action execution
     */
    private ActionResult executeChainedAction(FormPlayer player, String chainedAction, ActionContext context) {
        try {
            // Parse the chained action
            ActionExecutor.Action action = actionExecutor.parseAction(chainedAction);
            if (action == null) {
                logger.warn("Failed to parse chained action: " + chainedAction);
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid chained action format: " + chainedAction), player);
            }
            
            // Execute the chained action
            ActionResult result = actionExecutor.executeAction(player, action.getType(), action.getValue(), context);
            
            if (result.isSuccess()) {
                logger.debug("Successfully executed chained action for player " + player.getName() + ": " + chainedAction);
            } else {
                logger.warn("Chained action failed for player " + player.getName() + ": " + result.getMessage());
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error executing chained action for player " + player.getName() + ": " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Error executing chained action: " + e.getMessage()), player);
        }
    }
}