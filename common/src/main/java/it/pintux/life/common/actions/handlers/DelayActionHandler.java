package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionHandler;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;

import java.util.Map;

/**
 * Handles delays in action execution.
 * Useful for creating timed sequences or adding pauses between actions.
 * 
 * Usage: delay:1000 (delay for 1000 milliseconds = 1 second)
 * Usage: delay:5000 (delay for 5 seconds)
 * Usage: delay:500 (delay for 0.5 seconds)
 */
public class DelayActionHandler implements ActionHandler {
    private static final Logger logger = Logger.getLogger(DelayActionHandler.class);
    private static final long MAX_DELAY_MS = 30000; // Maximum 30 seconds delay
    
    @Override
    public String getActionType() {
        return "delay";
    }
    
    @Override
    public ActionResult execute(FormPlayer player, String actionData, ActionContext context) {
        if (actionData == null || actionData.trim().isEmpty()) {
            logger.warn("Delay action called with empty delay time for player: " + player.getName());
            return ActionResult.failure("No delay time specified");
        }
        
        try {
            // Process placeholders in the action data
            String processedData = processPlaceholders(actionData.trim(), context);
            
            long delayMs = Long.parseLong(processedData);
            
            if (delayMs < 0) {
                return ActionResult.failure("Delay time cannot be negative");
            }
            
            if (delayMs > MAX_DELAY_MS) {
                return ActionResult.failure("Delay time cannot exceed " + MAX_DELAY_MS + "ms (30 seconds)");
            }
            
            if (delayMs == 0) {
                return ActionResult.success("No delay applied");
            }
            
            logger.info("Applying delay of " + delayMs + "ms for player " + player.getName());
            
            // Perform the delay
            Thread.sleep(delayMs);
            
            return ActionResult.success("Delayed for " + delayMs + "ms");
            
        } catch (NumberFormatException e) {
            logger.warn("Invalid delay time format for player " + player.getName() + ": " + actionData);
            return ActionResult.failure("Invalid delay time format: " + actionData);
        } catch (InterruptedException e) {
            logger.warn("Delay interrupted for player " + player.getName());
            Thread.currentThread().interrupt(); // Restore interrupted status
            return ActionResult.failure("Delay was interrupted");
        } catch (Exception e) {
            logger.error("Error executing delay action for player " + player.getName() + ": " + e.getMessage());
            return ActionResult.failure("Error executing delay: " + e.getMessage());
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
}