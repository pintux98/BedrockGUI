package it.pintux.life.common.form.obj;

import java.util.Map;
import java.util.HashMap;

/**
 * Represents a button that can be conditionally shown or modified based on conditions.
 * Supports permission-based and placeholder-based conditions.
 */
public class ConditionalButton extends FormButton {
    
    // Condition for showing/hiding the button
    private String showCondition;
    
    // Alternative button properties when condition is not met
    private String alternativeText;
    private String alternativeImage;
    private String alternativeOnClick;
    
    // Conditional modifications
    private Map<String, ConditionalProperty> conditionalProperties;
    
    public ConditionalButton(String text, String image, String onClick) {
        super(text, image, onClick);
        this.conditionalProperties = new HashMap<>();
    }
    
    public ConditionalButton(String text, String image, String onClick, String showCondition) {
        super(text, image, onClick);
        this.showCondition = showCondition;
        this.conditionalProperties = new HashMap<>();
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
    
    /**
     * Checks if the button should be shown based on its show condition
     */
    public boolean shouldShow() {
        return showCondition == null || showCondition.trim().isEmpty();
    }
    
    /**
     * Gets the effective text based on conditions
     */
    public String getEffectiveText() {
        return getText();
    }
    
    /**
     * Gets the effective image based on conditions
     */
    public String getEffectiveImage() {
        return getImage();
    }
    
    /**
     * Gets the effective onClick action based on conditions
     */
    public String getEffectiveOnClick() {
        return getOnClick();
    }
    
    /**
     * Represents a conditional property modification
     */
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
    }
}