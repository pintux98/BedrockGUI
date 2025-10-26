package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionSystem;



import it.pintux.life.common.actions.ActionExecutor;
import it.pintux.life.common.utils.FormPlayer;


import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class DelayActionHandler extends BaseActionHandler {

    private static final long MAX_DELAY_MS = 30000;
    private static final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "DelayActionHandler-" + System.currentTimeMillis());
        thread.setDaemon(true);
        return thread;
    });

    private final ActionExecutor actionExecutor;

    public DelayActionHandler(ActionExecutor actionExecutor) {
        this.actionExecutor = actionExecutor;
    }


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
    public ActionSystem.ActionResult execute(FormPlayer player, String actionData, ActionSystem.ActionContext context) {
        if (actionData == null || actionData.trim().isEmpty()) {
            logger.warn("Delay action called with empty delay time for player: " + player.getName());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "ACTION_INVALID_PARAMETERS"), player);
        }

        try {
            // Check if it's the new YAML format with curly braces
            if (actionData.trim().startsWith("{") && actionData.trim().endsWith("}")) {
                return executeNewFormatDelay(player, actionData, context);
            }
            
            // Legacy format support
            String processedData = processPlaceholders(actionData.trim(), context, player);

            String[] parts = processedData.split(":", 2);
            long delayMs = Long.parseLong(parts[0]);
            String chainedAction = parts.length > 1 ? parts[1] : null;

            return executeDelayWithChain(player, delayMs, chainedAction, context);

        } catch (NumberFormatException e) {
            logger.warn("Invalid delay time format for player " + player.getName() + ": " + actionData);
            return createFailureResult("ACTION_INVALID_PARAMETERS", createReplacements("error", "Invalid delay time format: " + actionData), player);
        } catch (Exception e) {
            logger.error("Error executing delay action for player " + player.getName() + ": " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Error executing delay: " + e.getMessage()), player);
        }
    }

    private ActionSystem.ActionResult executeNewFormatDelay(FormPlayer player, String actionData, ActionSystem.ActionContext context) {
        try {
            // Parse the new YAML format using parseNewFormatValues
            java.util.List<String> values = parseNewFormatValues(actionData);
            
            // Process placeholders for each value
            java.util.List<String> processedValues = new java.util.ArrayList<>();
            for (String value : values) {
                String processedValue = processPlaceholders(value, context, player);
                processedValues.add(processedValue);
            }
            values = processedValues;
            
            if (values.isEmpty()) {
                return createFailureResult("ACTION_EXECUTION_ERROR",
                        createReplacements("error", "No delay values found in new format"), player);
            }

            // First value should be the delay time
            String delayValue = values.get(0);
            long delayMs = Long.parseLong(delayValue);
            
            // Optional chained action (if more values exist)
            String chainedAction = values.size() > 1 ? values.get(1) : null;
            
            return executeDelayWithChain(player, delayMs, chainedAction, context);
            
        } catch (NumberFormatException e) {
            logger.warn("Invalid delay time format in new format for player " + player.getName() + ": " + actionData);
            return createFailureResult("ACTION_INVALID_PARAMETERS", 
                    createReplacements("error", "Invalid delay time format in new format: " + actionData), player);
        } catch (Exception e) {
            logger.error("Error parsing new format delay action: " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR",
                    createReplacements("error", "Error parsing new format: " + e.getMessage()), player);
        }
    }

    private ActionSystem.ActionResult executeDelayWithChain(FormPlayer player, long delayMs, String chainedAction, ActionSystem.ActionContext context) {
        if (delayMs < 0) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Delay time cannot be negative"), player);
        }

        if (delayMs > MAX_DELAY_MS) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Delay time cannot exceed " + MAX_DELAY_MS + "ms (30 seconds)"), player);
        }

        if (delayMs == 0) {
            if (chainedAction != null && !chainedAction.trim().isEmpty()) {
                return executeChainedAction(player, chainedAction, context);
            }
            return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "No delay applied"), player);
        }

        logger.info("Applying delay of " + delayMs + "ms for player " + player.getName());

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(delayMs);

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

        String message = "Delay of " + delayMs + "ms scheduled";
        if (chainedAction != null && !chainedAction.trim().isEmpty()) {
            message += " with chained action";
        }
        return createSuccessResult("ACTION_SUCCESS", createReplacements("message", message), player);
    }

    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }

        String trimmed = actionValue.trim();
        
        // Support new YAML format
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                java.util.List<String> values = parseNewFormatValues(trimmed);
                if (values.isEmpty()) return false;
                
                // First value should be a valid delay time
                long delayMs = Long.parseLong(values.get(0));
                return delayMs >= 0 && delayMs <= MAX_DELAY_MS;
            } catch (Exception e) {
                return false;
            }
        }
        
        // Legacy format support
        try {
            long delayMs = Long.parseLong(trimmed);
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
                "New Format Examples:",
                "delay { - \"1000\" }",
                "delay { - \"5000\" }",
                "delay { - \"500\" - \"message:Hello after delay!\" }",
                "Legacy Format Examples:",
                "delay:1000",
                "delay:5000",
                "delay:500",
                "delay:2500"
        };
    }


    private ActionSystem.ActionResult executeChainedAction(FormPlayer player, String chainedAction, ActionSystem.ActionContext context) {
        try {

            ActionSystem.Action action = actionExecutor.parseAction(chainedAction);
            if (action == null) {
                logger.warn("Failed to parse chained action: " + chainedAction);
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Invalid chained action format: " + chainedAction), player);
            }


            ActionSystem.ActionResult result = actionExecutor.executeAction(player, action.getActionDefinition(), context);

            if (result.isSuccess()) {
                logger.debug("Successfully executed chained action for player " + player.getName() + ": " + chainedAction);
            } else {
                logger.warn("Chained action failed for player " + player.getName() + ": " + result.message());
            }

            return result;

        } catch (Exception e) {
            logger.error("Error executing chained action for player " + player.getName() + ": " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Error executing chained action: " + e.getMessage()), player);
        }
    }
}

