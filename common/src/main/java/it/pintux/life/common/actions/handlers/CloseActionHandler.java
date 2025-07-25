package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionHandler;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;
import org.geysermc.floodgate.api.FloodgateApi;

/**
 * Handles closing the current form
 */
public class CloseActionHandler implements ActionHandler {
    
    private static final Logger logger = Logger.getLogger(CloseActionHandler.class);
    
    @Override
    public String getActionType() {
        return "close";
    }
    
    @Override
    public ActionResult execute(FormPlayer player, String actionValue, ActionContext context) {
        if (player == null) {
            return ActionResult.failure("Player cannot be null");
        }
        
        try {
            logger.debug("Closing form for player " + player.getName());
            return ActionResult.success("Form closed successfully");
            
        } catch (Exception e) {
            logger.error("Error closing form for player " + player.getName(), e);
            return ActionResult.failure("Failed to close form: " + e.getMessage(), e);
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