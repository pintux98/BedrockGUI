package it.pintux.life.common.form.obj;

import java.util.Map;
import java.util.HashMap;
import java.util.StringJoiner;


public class ConditionalButton extends FormButton {
    private final String showCondition;
    private String alternativeText;
    private String alternativeImage;
    private String alternativeOnClick;
    private final Map<String, ConditionalProperty> conditionalProperties = new HashMap<>();

    public ConditionalButton(String text, String image, String onClick, String showCondition) {
        super(text, image, onClick);
        this.showCondition = showCondition;
    }

    public String getShowCondition() {
        return showCondition;
    }

    public void setAlternativeText(String alternativeText) {
        this.alternativeText = alternativeText;
    }

    public void setAlternativeImage(String alternativeImage) {
        this.alternativeImage = alternativeImage;
    }

    public void setAlternativeOnClick(String alternativeOnClick) {
        this.alternativeOnClick = alternativeOnClick;
    }

    public Map<String, ConditionalProperty> getConditionalProperties() {
        return conditionalProperties;
    }

    public void addConditionalProperty(String condition, String property, String value) {
        String key = createConditionalPropertyKey(condition, property);
        conditionalProperties.put(key, new ConditionalProperty(condition, property, value));
    }


    public boolean hasShowCondition() {
        return showCondition != null && !showCondition.trim().isEmpty();
    }


    public boolean hasAlternative() {
        return alternativeText != null || alternativeImage != null || alternativeOnClick != null;
    }


    public String getEffectiveText(String condition, boolean useAlternative) {

        if (condition != null) {
            ConditionalProperty property = conditionalProperties.get(createConditionalPropertyKey(condition, "text"));
            if (property == null) {
                property = findConditionalProperty(condition, "text");
            }
            if (property != null && "text".equals(property.getProperty())) {
                return property.getValue();
            }
        }


        if (useAlternative && alternativeText != null) {
            return alternativeText;
        }


        return getText();
    }


    public String getEffectiveImage(String condition, boolean useAlternative) {

        if (condition != null) {
            ConditionalProperty property = conditionalProperties.get(createConditionalPropertyKey(condition, "image"));
            if (property == null) {
                property = findConditionalProperty(condition, "image");
            }
            if (property != null && "image".equals(property.getProperty())) {
                return property.getValue();
            }
        }


        if (useAlternative && alternativeImage != null) {
            return alternativeImage;
        }


        return getImage();
    }


    public String getEffectiveOnClick(String condition, boolean useAlternative) {

        if (condition != null) {
            ConditionalProperty property = conditionalProperties.get(createConditionalPropertyKey(condition, "onClick"));
            if (property == null) {
                property = findConditionalProperty(condition, "onClick");
            }
            if (property != null && "onClick".equals(property.getProperty())) {
                return property.getValue();
            }
        }


        if (useAlternative && alternativeOnClick != null) {
            return alternativeOnClick;
        }


        return getOnClick();
    }


    public static class ConditionalProperty {
        private final String condition;
        private final String property;
        private final String value;

        public ConditionalProperty(String condition, String property, String value) {
            this.condition = condition;
            this.property = property;
            this.value = value;
        }

        public String getCondition() {
            return condition;
        }

        public String getProperty() {
            return property;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return condition + " -> " + property + "=" + value;
        }
    }

    private ConditionalProperty findConditionalProperty(String condition, String propertyName) {
        for (ConditionalProperty property : conditionalProperties.values()) {
            if (condition.equals(property.getCondition()) && propertyName.equals(property.getProperty())) {
                return property;
            }
        }
        return null;
    }

    private String createConditionalPropertyKey(String condition, String propertyName) {
        return condition + "::" + propertyName;
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
                .toString();
    }
}

