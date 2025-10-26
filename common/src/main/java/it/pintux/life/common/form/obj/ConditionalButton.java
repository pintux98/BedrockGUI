package it.pintux.life.common.form.obj;

import it.pintux.life.common.actions.ActionSystem;

import it.pintux.life.common.actions.ActionSystem.ActionDefinition;
import it.pintux.life.common.actions.ActionParser;

import java.util.Map;
import java.util.HashMap;
import java.util.StringJoiner;


public class ConditionalButton extends FormButton {
    private String showCondition;
    private String alternativeText;
    private String alternativeImage;
    private String alternativeOnClick;
    private Map<String, ConditionalProperty> conditionalProperties;
    private int priority;
    private String priorityCondition;
    private ActionSystem.ActionDefinition actionDefinition;
    private ActionSystem.ActionDefinition alternativeActionDefinition;
    private Map<String, ActionSystem.ActionDefinition> conditionalActions;

    public ConditionalButton(String text, String image, String onClick) {
        super(text, image, onClick);
        this.conditionalProperties = new HashMap<>();
        this.conditionalActions = new HashMap<>();
        this.priority = 0;

        this.actionDefinition = ActionParser.parse(onClick);
    }

    public ConditionalButton(String text, String image, String onClick, String showCondition) {
        super(text, image, onClick);
        this.showCondition = showCondition;
        this.conditionalProperties = new HashMap<>();
        this.conditionalActions = new HashMap<>();
        this.priority = 0;

        this.actionDefinition = ActionParser.parse(onClick);
    }

    public ConditionalButton(String text, String image, ActionSystem.ActionDefinition actionDefinition) {
        super(text, image, actionDefinition);
        this.conditionalProperties = new HashMap<>();
        this.conditionalActions = new HashMap<>();
        this.priority = 0;
        this.actionDefinition = actionDefinition;
    }

    public ConditionalButton(String text, String image, ActionSystem.ActionDefinition actionDefinition, String showCondition, int priority) {
        super(text, image, actionDefinition);
        this.showCondition = showCondition;
        this.conditionalProperties = new HashMap<>();
        this.conditionalActions = new HashMap<>();
        this.priority = priority;
        this.actionDefinition = actionDefinition;
    }

    public String getShowCondition() {
        return showCondition;
    }

    public void setShowCondition(String showCondition) {
        this.showCondition = showCondition;
    }

    public String getAlternativeText() {
        return alternativeText;
    }

    public void setAlternativeText(String alternativeText) {
        this.alternativeText = alternativeText;
    }

    public String getAlternativeImage() {
        return alternativeImage;
    }

    public void setAlternativeImage(String alternativeImage) {
        this.alternativeImage = alternativeImage;
    }

    public String getAlternativeOnClick() {
        return alternativeOnClick;
    }

    public void setAlternativeOnClick(String alternativeOnClick) {
        this.alternativeOnClick = alternativeOnClick;
    }

    public Map<String, ConditionalProperty> getConditionalProperties() {
        return conditionalProperties;
    }

    public void addConditionalProperty(String condition, String property, String value) {
        conditionalProperties.put(condition, new ConditionalProperty(property, value));
    }


    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getPriorityCondition() {
        return priorityCondition;
    }

    public void setPriorityCondition(String priorityCondition) {
        this.priorityCondition = priorityCondition;
    }


    public ActionSystem.ActionDefinition getActionDefinition() {
        return actionDefinition;
    }

    public void setActionDefinition(ActionSystem.ActionDefinition actionDefinition) {
        this.actionDefinition = actionDefinition;
    }

    public ActionSystem.ActionDefinition getAlternativeActionDefinition() {
        return alternativeActionDefinition;
    }

    public void setAlternativeActionDefinition(ActionSystem.ActionDefinition alternativeActionDefinition) {
        this.alternativeActionDefinition = alternativeActionDefinition;
    }

    public Map<String, ActionSystem.ActionDefinition> getConditionalActions() {
        return conditionalActions;
    }

    public void addConditionalAction(String condition, ActionSystem.ActionDefinition action) {
        conditionalActions.put(condition, action);
    }

    public void addConditionalAction(String condition, String actionString) {
        ActionSystem.ActionDefinition action = ActionParser.parse(actionString);
        if (action != null) {
            conditionalActions.put(condition, action);
        }
    }


    public boolean hasShowCondition() {
        return showCondition != null && !showCondition.trim().isEmpty();
    }


    public String getEffectiveText(String condition) {

        if (condition != null) {
            ConditionalProperty property = conditionalProperties.get(condition);
            if (property != null && "text".equals(property.getProperty())) {
                return property.getValue();
            }
        }


        if (alternativeText != null) {
            return alternativeText;
        }


        return getText();
    }


    public String getEffectiveImage(String condition) {

        if (condition != null) {
            ConditionalProperty property = conditionalProperties.get(condition);
            if (property != null && "image".equals(property.getProperty())) {
                return property.getValue();
            }
        }


        if (alternativeImage != null) {
            return alternativeImage;
        }


        return getImage();
    }


    public String getEffectiveOnClick(String condition) {

        if (condition != null) {
            ConditionalProperty property = conditionalProperties.get(condition);
            if (property != null && "onClick".equals(property.getProperty())) {
                return property.getValue();
            }
        }


        if (alternativeOnClick != null) {
            return alternativeOnClick;
        }


        return getOnClick();
    }


    public ActionSystem.ActionDefinition getEffectiveActionDefinition(String condition) {

        if (condition != null) {
            ActionSystem.ActionDefinition conditionalAction = conditionalActions.get(condition);
            if (conditionalAction != null) {
                return conditionalAction;
            }


        }


        if (alternativeActionDefinition != null) {
            return alternativeActionDefinition;
        }


        return actionDefinition;
    }


    public boolean hasPriority() {
        return priority != 0 || (priorityCondition != null && !priorityCondition.trim().isEmpty());
    }


    public int getEffectivePriority() {


        return priority;
    }


    @Override
    public void setOnClick(String onClick) {
        super.setOnClick(onClick);
        this.actionDefinition = ActionParser.parse(onClick);
    }


    public static class ConditionalProperty {
        private final String property;
        private final String value;

        public ConditionalProperty(String property, String value) {
            this.property = property;
            this.value = value;
        }

        public String getProperty() {
            return property;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return property + "=" + value;
        }
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ConditionalButton.class.getSimpleName() + "[", "]")
                .add("text='" + getText() + "'")
                .add("image='" + getImage() + "'")
                .add("onClick='" + getOnClick() + "'")
                .add("showCondition='" + showCondition + "'")
                .add("alternativeText='" + alternativeText + "'")
                .add("alternativeImage='" + alternativeImage + "'")
                .add("alternativeOnClick='" + alternativeOnClick + "'")
                .add("conditionalProperties=" + conditionalProperties)
                .add("priority=" + priority)
                .add("priorityCondition='" + priorityCondition + "'")
                .add("actionDefinition=" + actionDefinition)
                .add("alternativeActionDefinition=" + alternativeActionDefinition)
                .add("conditionalActions=" + conditionalActions)
                .toString();
    }
}

