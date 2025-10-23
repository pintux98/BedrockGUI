package it.pintux.life.common.form.obj;

import it.pintux.life.common.actions.ActionDefinition;
import it.pintux.life.common.actions.ActionParser;
import java.util.List;
import java.util.StringJoiner;


public class FormButton {
    private String text;
    private String image;
    private String onClick;
    private ActionDefinition action;
    private List<ActionDefinition> actions;

    public FormButton(String text, String image, String onClick) {
        this.text = text;
        this.image = image;
        this.onClick = onClick;
    }
    
    
    public FormButton(String text, String image, ActionDefinition action) {
        this.text = text;
        this.image = image;
        this.action = action;
    }
    
    
    public FormButton(String text, String image, List<ActionDefinition> actions) {
        this.text = text;
        this.image = image;
        this.actions = actions;
    }
    
    
    public FormButton(String text, String image, String yamlActions, boolean isYaml) {
        this.text = text;
        this.image = image;
        if (isYaml) {
            this.actions = ActionParser.parseList(yamlActions);
        } else {
            this.onClick = yamlActions;
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getOnClick() {
        return onClick;
    }

    public void setOnClick(String onClick) {
        this.onClick = onClick;
    }
    
    public ActionDefinition getAction() {
        return action;
    }
    
    public void setAction(ActionDefinition action) {
        this.action = action;
    }
    
    public List<ActionDefinition> getActions() {
        return actions;
    }
    
    public void setActions(List<ActionDefinition> actions) {
        this.actions = actions;
    }
    
    
    public boolean hasActions() {
        return onClick != null || action != null || (actions != null && !actions.isEmpty());
    }
    
    
    public ActionDefinition getPrimaryAction() {
        if (action != null) {
            return action;
        }
        if (actions != null && !actions.isEmpty()) {
            return actions.get(0);
        }
        
        return null;
    }
    
    
    public List<ActionDefinition> getAllActions() {
        if (actions != null && !actions.isEmpty()) {
            return actions;
        }
        if (action != null) {
            return List.of(action);
        }
        
        return List.of();
    }
    
    
    public void setActionsFromYaml(String yamlContent) {
        this.actions = ActionParser.parseList(yamlContent);
    }
    
    
    public String getActionsAsYaml() {
        if (actions != null && !actions.isEmpty()) {
            return ActionParser.toYaml(actions);
        }
        if (action != null) {
            return ActionParser.toYaml(action);
        }
        return null;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", FormButton.class.getSimpleName() + "[", "]")
                .add("text='" + text + "'")
                .add("image='" + image + "'");
        
        if (onClick != null) {
            joiner.add("onClick='" + onClick + "'");
        }
        if (action != null) {
            joiner.add("action=" + action);
        }
        if (actions != null && !actions.isEmpty()) {
            joiner.add("actions=" + actions.size() + " items");
        }
        
        return joiner.toString();
    }
}

