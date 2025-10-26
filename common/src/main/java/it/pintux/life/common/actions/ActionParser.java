package it.pintux.life.common.actions;

import java.util.*;


public class ActionParser {


    public static ActionSystem.ActionDefinition parseFromMap(Map<String, Object> yamlMap) {
        if (yamlMap == null || yamlMap.isEmpty()) {
            return new ActionSystem.ActionDefinition();
        }

        return new ActionSystem.ActionDefinition(yamlMap);
    }


    public static List<ActionSystem.ActionDefinition> parseFromList(List<Object> yamlList) {
        List<ActionSystem.ActionDefinition> actions = new ArrayList<>();

        if (yamlList == null || yamlList.isEmpty()) {
            return actions;
        }

        for (Object item : yamlList) {
            ActionSystem.ActionDefinition action = convertToActionDefinition(item);
            if (action != null && !action.isEmpty()) {
                actions.add(action);
            }
        }

        return actions;
    }


    public static ActionSystem.ActionDefinition parse(String actionString) {
        if (actionString == null || actionString.trim().isEmpty()) {
            return new ActionSystem.ActionDefinition();
        }


        ActionSystem.ActionDefinition actionDef = new ActionSystem.ActionDefinition();
        actionDef.addAction("command", actionString.trim());
        return actionDef;
    }


    public static List<ActionSystem.ActionDefinition> parseList(String yamlContent) {
        if (yamlContent == null || yamlContent.trim().isEmpty()) {
            return new ArrayList<>();
        }


        List<ActionSystem.ActionDefinition> actions = new ArrayList<>();
        actions.add(parse(yamlContent));
        return actions;
    }


    public static String toYaml(List<ActionSystem.ActionDefinition> actions) {
        if (actions == null || actions.isEmpty()) {
            return "";
        }


        return toYaml(actions.get(0));
    }


    public static String toYaml(ActionSystem.ActionDefinition action) {
        if (action == null || action.isEmpty()) {
            return "";
        }


        for (String actionType : action.getActionTypes()) {
            Object value = action.getAction(actionType);
            return actionType + ": " + (value != null ? value.toString() : "");
        }

        return "";
    }


    public static ActionSystem.ActionDefinition convertToActionDefinition(Object obj) {
        if (obj == null) {
            return null;
        }

        if (obj instanceof ActionSystem.ActionDefinition) {
            return (ActionSystem.ActionDefinition) obj;
        }

        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            return parseFromMap(map);
        }

        if (obj instanceof ActionSystem.ActionDefinition) {
            return (ActionSystem.ActionDefinition) obj;
        }

        if (obj instanceof String) {

            return null;
        }

        return null;
    }


    public static boolean isValid(ActionSystem.ActionDefinition action) {
        if (action == null || action.isEmpty()) {
            return false;
        }


        return !action.getActionTypes().isEmpty();
    }
}

