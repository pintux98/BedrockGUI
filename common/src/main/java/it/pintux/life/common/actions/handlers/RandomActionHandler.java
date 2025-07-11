package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionHandler;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.actions.ActionExecutor;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Handles random action execution from a list of possible actions.
 * Useful for creating variety in rewards, responses, or outcomes.
 * 
 * Usage: random:command:give {player} diamond|command:give {player} emerald|message:You got lucky!
 * Usage: random:sound:ui.button.click|sound:entity.experience_orb.pickup|sound:block.note_block.harp
 * Usage: random:economy:add:100|economy:add:200|economy:add:500
 * 
 * Format: random:action1|action2|action3|...
 * Each action should be in the format: action_type:action_data
 */
public class RandomActionHandler implements ActionHandler {
    private static final Logger logger = Logger.getLogger(RandomActionHandler.class);
    private final ActionExecutor actionExecutor;
    private final Random random;
    
    public RandomActionHandler(ActionExecutor actionExecutor) {
        this.actionExecutor = actionExecutor;
        this.random = new Random();
    }
    
    @Override
    public String getActionType() {
        return "random";
    }
    
    @Override
    public ActionResult execute(FormPlayer player, String actionData, ActionContext context) {
        if (actionData == null || actionData.trim().isEmpty()) {
            logger.warn("Random action called with empty data for player: " + player.getName());
            return ActionResult.failure("No random actions specified");
        }
        
        try {
            // Process placeholders in the action data
            String processedData = processPlaceholders(actionData.trim(), context);
            
            // Split by pipe character to get individual actions
            String[] actionOptions = processedData.split("\\|");
            
            if (actionOptions.length == 0) {
                return ActionResult.failure("No valid actions found in random action list");
            }
            
            // Filter out empty actions
            List<String> validActions = new ArrayList<>();
            for (String action : actionOptions) {
                String trimmedAction = action.trim();
                if (!trimmedAction.isEmpty()) {
                    validActions.add(trimmedAction);
                }
            }
            
            if (validActions.isEmpty()) {
                return ActionResult.failure("No valid actions found after filtering");
            }
            
            // Select a random action
            int randomIndex = random.nextInt(validActions.size());
            String selectedAction = validActions.get(randomIndex);
            
            logger.info("Selected random action for player " + player.getName() + ": " + selectedAction + " (" + (randomIndex + 1) + "/" + validActions.size() + ")");
            
            // Parse and execute the selected action
            ActionExecutor.Action parsedAction = actionExecutor.parseAction(selectedAction);
            if (parsedAction == null) {
                return ActionResult.failure("Failed to parse selected action: " + selectedAction);
            }
            
            ActionResult result = actionExecutor.executeAction(player, parsedAction.getType(), parsedAction.getValue(), context);
            
            if (result.isSuccess()) {
                return ActionResult.success("Random action executed (" + (randomIndex + 1) + "/" + validActions.size() + "): " + result.getMessage());
            } else {
                return ActionResult.failure("Random action failed: " + result.getMessage());
            }
            
        } catch (Exception e) {
            logger.error("Error executing random action for player " + player.getName() + ": " + e.getMessage());
            return ActionResult.failure("Error executing random action: " + e.getMessage());
        }
    }
    
    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }
        
        // Split by pipe character to get individual actions
        String[] actionOptions = actionValue.trim().split("\\|");
        
        if (actionOptions.length == 0) {
            return false;
        }
        
        // Check that at least one action is valid
        for (String action : actionOptions) {
            String trimmedAction = action.trim();
            if (!trimmedAction.isEmpty() && trimmedAction.contains(":")) {
                return true; // At least one action looks valid
            }
        }
        
        return false;
    }
    
    @Override
    public String getDescription() {
        return "Randomly selects and executes one action from a list of possible actions";
    }
    
    @Override
    public String[] getUsageExamples() {
        return new String[]{
            "random:command:give {player} diamond|command:give {player} emerald|message:You got lucky!",
            "random:sound:ui.button.click|sound:entity.experience_orb.pickup",
            "random:economy:add:100|economy:add:200|economy:add:500",
            "random:message:Option 1|message:Option 2|message:Option 3"
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