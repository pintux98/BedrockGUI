package it.pintux.life.common.form.obj;

import java.util.StringJoiner;

/**
 * Represents a form button with priority-based display logic for Bedrock forms.
 * Items are filtered by view requirements and sorted by priority for sequential display.
 * 
 * Priority Logic:
 * - Lower priority numbers = higher priority (1 is highest priority)
 * - Items are filtered by view requirements first
 * - Remaining items are sorted by priority
 * - Items are added sequentially to the form in priority order
 */
public class PriorityItem extends FormButton {
    
    private int priority;
    private String viewRequirement;
    
    /**
     * Creates a new PriorityItem with default priority of 1
     */
    public PriorityItem(String text, String image, String onClick) {
        super(text, image, onClick);
        this.priority = 1;
    }
    
    /**
     * Creates a new PriorityItem with specified priority
     */
    public PriorityItem(String text, String image, String onClick, int priority) {
        super(text, image, onClick);
        this.priority = priority;
    }
    
    /**
     * Creates a new PriorityItem with priority and view requirement
     */
    public PriorityItem(String text, String image, String onClick, int priority, String viewRequirement) {
        super(text, image, onClick);
        this.priority = priority;
        this.viewRequirement = viewRequirement;
    }
    

    
    /**
     * Gets the priority of this item (lower numbers = higher priority)
     */
    public int getPriority() {
        return priority;
    }
    
    /**
     * Sets the priority of this item
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    /**
     * Gets the view requirement condition for this item
     */
    public String getViewRequirement() {
        return viewRequirement;
    }
    
    /**
     * Sets the view requirement condition for this item
     */
    public void setViewRequirement(String viewRequirement) {
        this.viewRequirement = viewRequirement;
    }
    

    
    /**
     * Checks if this item has a view requirement
     */
    public boolean hasViewRequirement() {
        return viewRequirement != null && !viewRequirement.trim().isEmpty();
    }
    

    
    /**
     * Compares this item with another PriorityItem based on priority
     * Returns negative if this item has higher priority (lower number)
     */
    public int compareTo(PriorityItem other) {
        return Integer.compare(this.priority, other.priority);
    }
    
    @Override
    public String toString() {
        return new StringJoiner(", ", PriorityItem.class.getSimpleName() + "[", "]")
                .add("text='" + getText() + "'")
                .add("image='" + getImage() + "'")
                .add("onClick='" + getOnClick() + "'")
                .add("priority=" + priority)
                .add("viewRequirement='" + viewRequirement + "'")

                .toString();
    }
}