package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.utils.FormPlayer;
import org.geysermc.floodgate.api.FloodgateApi;

/**
 * Handles closing the current form
 */
public class CloseActionHandler extends BaseActionHandler {
    
    @Override
    public String getActionType() {
        return "close";
    }
    
    @Override
    public ActionResult execute(FormPlayer player, String actionValue, ActionContext context) {
        ActionResult validationResult = validateBasicParameters(player, actionValue);
        if (validationResult != null) {
            return validationResult;
        }
        
        try {
            logger.debug("Closing form for player " + player.getName());
            return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Form closed successfully"), player);
            
        } catch (Exception e) {
            logger.error("Error closing form for player " + player.getName(), e);
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to close form: " + e.getMessage()), player, e);
        }
    }
    
    @Override
    public boolean isValidAction(String actionValue) {
        // Close action doesn't require any specific value
        return true;
    }
    
    @Override
    public String getDescription() {
        return "Closes the current form without opening another one.";
    }
    
    @Override
    public String[] getUsageExamples() {
        return new String[]{
            "close",
            "" // Empty value is also valid
        };
    }
}