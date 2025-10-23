package it.pintux.life.common.actions;

import java.util.*;


public class ActionParser {
    
    
    public static ActionDefinition parseFromMap(Map<String, Object> yamlMap) {
        if (yamlMap == null || yamlMap.isEmpty()) {
            return new ActionDefinition();
        }
        
        return new ActionDefinition(yamlMap);
    }
    
    
    public static List<ActionDefinition> parseFromList(List<Object> yamlList) {
        List<ActionDefinition> actions = new ArrayList<>();
        
        if (yamlList == null || yamlList.isEmpty()) {
            return actions;
        }
        
        for (Object item : yamlList) {
            ActionDefinition action = convertToActionDefinition(item);
            if (action != null && !action.isEmpty()) {
                actions.add(action);
            }
        }
        
        return actions;
    }
    
    
    public static ActionDefinition parse(String actionString) {
        if (actionString == null || actionString.trim().isEmpty()) {
            return new ActionDefinition();
        }
        
        
        ActionDefinition actionDef = new ActionDefinition();
        actionDef.addAction("command", actionString.trim());
        return actionDef;
    }
    
    
    public static List<ActionDefinition> parseList(String yamlContent) {
        if (yamlContent == null || yamlContent.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        
        List<ActionDefinition> actions = new ArrayList<>();
        actions.add(parse(yamlContent));
        return actions;
    }
    
    
    public static String toYaml(List<ActionDefinition> actions) {
        if (actions == null || actions.isEmpty()) {
            return "";
        }
        
        
        return toYaml(actions.get(0));
    }
    
    
    public static String toYaml(ActionDefinition action) {
        if (action == null || action.isEmpty()) {
            return "";
        }
        
        
        for (String actionType : action.getActionTypes()) {
            Object value = action.getAction(actionType);
            return actionType + ": " + (value != null ? value.toString() : "");
        }
        
        return "";
    }
    
    
    
    
    
    
    
    
    public static ActionDefinition convertToActionDefinition(Object obj) {
        if (obj == null) {
            return null;
        }
        
        if (obj instanceof ActionDefinition) {
            return (ActionDefinition) obj;
        }
        
        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            return parseFromMap(map);
        }
        
        if (obj instanceof ActionDefinition) {
            return (ActionDefinition) obj;
        }
        
        if (obj instanceof String) {
            
            return null;
        }
        
        return null;
    }
    
    
    public static boolean isValid(ActionDefinition action) {
        if (action == null || action.isEmpty()) {
            return false;
        }
        
        
        return !action.getActionTypes().isEmpty();
    }
    
    
    
    public static List<ActionDefinition> getExamples() {
        List<ActionDefinition> examples = new ArrayList<>();
        
        
        examples.add(ActionDefinition.simple("command", "give @s diamond 1"));
        
        
        examples.add(ActionDefinition.simple("message", "Hello &cWorld!"));
        
        
        examples.add(ActionDefinition.simple("give_money", 100));
        
        
        examples.add(ActionDefinition.simple("delay", 1000));
        
        
        examples.add(ActionDefinition.simple("teleport", "spawn"));
        
        
        Map<String, Object> multiActionMap = new HashMap<>();
        multiActionMap.put("message", "Starting process...");
        multiActionMap.put("give_money", 50);
        multiActionMap.put("command", "effect give @s minecraft:speed 30 1");
        examples.add(new ActionDefinition(multiActionMap));
        
        return examples;
    }
    
    
    public static List<String> getYamlExamples() {
        List<String> examples = new ArrayList<>();
        
        examples.add("# Simple command\ncommand: \"give @s diamond 1\"");
        
        examples.add("# Simple message\nmessage: \"Hello &cWorld!\"");
        
        examples.add("# Give money\ngive_money: 100");
        
        examples.add("# Multiple actions\ncommand: \"give @s diamond 1\"\ndelay: 1000\nmessage: \"You got a diamond!\"");
        
        examples.add("# Button onClick with multiple actions\nonClick:\n  - command: \"give @s diamond 1\"\n  - delay: 500\n  - message: \"You received a diamond!\"\n  - give_money: 50");
        
        return examples;
    }
    

}
