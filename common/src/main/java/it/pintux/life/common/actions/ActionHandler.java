package it.pintux.life.common.actions;

import it.pintux.life.common.utils.FormPlayer;

import java.util.Map;

/**
 * Interface for handling form actions in an extensible way.
 * Implementations should handle specific action types (e.g., command, open, message, etc.)
 */
public interface ActionHandler {
    
    /**
     * Gets the action type this handler supports (e.g., "command", "open", "message")
     * @return the action type identifier
     */
    String getActionType();
    
    /**
     * Executes the action with the given parameters
     * @param player the player executing the action
     * @param actionValue the action value/parameters
     * @param context additional context data (placeholders, form results, etc.)
     * @return ActionResult indicating success/failure and any messages
     */
    ActionResult execute(FormPlayer player, String actionValue, ActionContext context);
    
    /**
     * Validates if the action value is properly formatted for this handler
     * @param actionValue the action value to validate
     * @return true if valid, false otherwise
     */
    boolean isValidAction(String actionValue);
    
    /**
     * Gets a description of this action handler for documentation/help
     * @return description of the action handler
     */
    String getDescription();
    
    /**
     * Gets usage examples for this action handler
     * @return array of usage examples
     */
    String[] getUsageExamples();
}