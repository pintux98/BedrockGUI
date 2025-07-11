package it.pintux.life.common.actions;

import it.pintux.life.common.utils.Logger;
import it.pintux.life.common.utils.ValidationUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.Collection;

/**
 * Registry for managing action handlers
 */
public class ActionRegistry {
    
    private static final Logger logger = Logger.getLogger(ActionRegistry.class);
    private static ActionRegistry instance;
    private final Map<String, ActionHandler> handlers = new ConcurrentHashMap<>();
    
    private ActionRegistry() {
        // Private constructor for singleton
    }
    
    public static ActionRegistry getInstance() {
        if (instance == null) {
            synchronized (ActionRegistry.class) {
                if (instance == null) {
                    instance = new ActionRegistry();
                }
            }
        }
        return instance;
    }
    
    /**
     * Registers an action handler
     * @param handler the action handler to register
     * @throws IllegalArgumentException if handler is null or action type is invalid
     */
    public void registerHandler(ActionHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Action handler cannot be null");
        }
        
        String actionType = handler.getActionType();
        if (ValidationUtils.isNullOrEmpty(actionType)) {
            throw new IllegalArgumentException("Action type cannot be null or empty");
        }
        
        actionType = actionType.toLowerCase().trim();
        
        if (handlers.containsKey(actionType)) {
            logger.warn("Overriding existing action handler for type: " + actionType);
        }
        
        handlers.put(actionType, handler);
        logger.info("Registered action handler for type: " + actionType);
    }
    
    /**
     * Unregisters an action handler
     * @param actionType the action type to unregister
     * @return true if handler was removed, false if not found
     */
    public boolean unregisterHandler(String actionType) {
        if (ValidationUtils.isNullOrEmpty(actionType)) {
            return false;
        }
        
        actionType = actionType.toLowerCase().trim();
        ActionHandler removed = handlers.remove(actionType);
        
        if (removed != null) {
            logger.info("Unregistered action handler for type: " + actionType);
            return true;
        }
        
        return false;
    }
    
    /**
     * Gets an action handler by type
     * @param actionType the action type
     * @return the action handler or null if not found
     */
    public ActionHandler getHandler(String actionType) {
        if (ValidationUtils.isNullOrEmpty(actionType)) {
            return null;
        }
        
        return handlers.get(actionType.toLowerCase().trim());
    }
    
    /**
     * Checks if a handler is registered for the given action type
     * @param actionType the action type to check
     * @return true if handler exists, false otherwise
     */
    public boolean hasHandler(String actionType) {
        if (ValidationUtils.isNullOrEmpty(actionType)) {
            return false;
        }
        
        return handlers.containsKey(actionType.toLowerCase().trim());
    }
    
    /**
     * Gets all registered action types
     * @return set of registered action types
     */
    public Set<String> getRegisteredActionTypes() {
        return Set.copyOf(handlers.keySet());
    }
    
    /**
     * Gets all registered handlers
     * @return collection of registered handlers
     */
    public Collection<ActionHandler> getAllHandlers() {
        return handlers.values();
    }
    
    /**
     * Clears all registered handlers
     */
    public void clear() {
        int count = handlers.size();
        handlers.clear();
        logger.info("Cleared " + count + " action handlers");
    }
    
    /**
     * Gets the number of registered handlers
     * @return number of registered handlers
     */
    public int size() {
        return handlers.size();
    }
}