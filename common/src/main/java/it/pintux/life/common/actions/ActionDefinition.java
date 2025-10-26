package it.pintux.life.common.actions;

import java.util.*;


public class ActionDefinition {


    private Map<String, Object> actions;


    public ActionDefinition() {
        this.actions = new LinkedHashMap<>();
    }

    public ActionDefinition(Map<String, Object> actions) {
        this.actions = actions != null ? new LinkedHashMap<>(actions) : new LinkedHashMap<>();
    }


    public Map<String, Object> getActions() {
        return actions;
    }

    public void setActions(Map<String, Object> actions) {
        this.actions = actions != null ? new LinkedHashMap<>(actions) : new LinkedHashMap<>();
    }


    public void addAction(String actionType, Object value) {
        actions.put(actionType, value);
    }


    public Object getAction(String actionType) {
        return actions.get(actionType);
    }


    public boolean hasAction(String actionType) {
        return actions.containsKey(actionType);
    }


    public void removeAction(String actionType) {
        actions.remove(actionType);
    }


    public Set<String> getActionTypes() {
        return actions.keySet();
    }


    public boolean isConditional() {
        return hasAction("conditional");
    }


    public boolean isMultiAction() {
        return actions.size() > 1 || (actions.size() == 1 && !isConditional());
    }


    public boolean isEmpty() {
        return actions.isEmpty();
    }


    public static ActionDefinition simple(String actionType, Object value) {
        ActionDefinition action = new ActionDefinition();
        action.addAction(actionType, value);
        return action;
    }


    public static List<ActionDefinition> multi(ActionDefinition... actions) {
        return Arrays.asList(actions);
    }

    @Override
    public String toString() {
        return "ActionDefinition{actions=" + actions + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionDefinition that = (ActionDefinition) o;
        return Objects.equals(actions, that.actions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actions);
    }
}
