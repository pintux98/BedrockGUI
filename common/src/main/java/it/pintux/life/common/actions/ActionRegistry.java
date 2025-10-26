package it.pintux.life.common.actions;

import it.pintux.life.common.utils.Logger;
import it.pintux.life.common.utils.ValidationUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.Collection;


public class ActionRegistry {

    private static final Logger logger = Logger.getLogger(ActionRegistry.class);
    private static ActionRegistry instance;
    private final Map<String, ActionSystem.ActionHandler> handlers = new ConcurrentHashMap<>();

    private ActionRegistry() {

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


    public void registerHandler(ActionSystem.ActionHandler handler) {
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


    public boolean unregisterHandler(String actionType) {
        if (ValidationUtils.isNullOrEmpty(actionType)) {
            return false;
        }

        actionType = actionType.toLowerCase().trim();
        ActionSystem.ActionHandler removed = handlers.remove(actionType);

        if (removed != null) {
            logger.info("Unregistered action handler for type: " + actionType);
            return true;
        }

        return false;
    }


    public ActionSystem.ActionHandler getHandler(String actionType) {
        if (ValidationUtils.isNullOrEmpty(actionType)) {
            return null;
        }

        return handlers.get(actionType.toLowerCase().trim());
    }


    public boolean hasHandler(String actionType) {
        if (ValidationUtils.isNullOrEmpty(actionType)) {
            return false;
        }

        return handlers.containsKey(actionType.toLowerCase().trim());
    }


    public Set<String> getRegisteredActionTypes() {
        return Set.copyOf(handlers.keySet());
    }


    public Collection<ActionSystem.ActionHandler> getAllHandlers() {
        return handlers.values();
    }


    public void clear() {
        int count = handlers.size();
        handlers.clear();
        logger.info("Cleared " + count + " action handlers");
    }


    public int size() {
        return handlers.size();
    }
}

