package it.pintux.life.common.form.priority;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.form.obj.PriorityItem;
import it.pintux.life.common.utils.ConditionEvaluator;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;
import it.pintux.life.common.utils.MessageData;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles priority-based item resolution for Bedrock forms.
 * Filters items by view requirements and sorts them by priority for sequential display.
 */
public class PriorityResolver {
    
    private static final Logger logger = Logger.getLogger(PriorityResolver.class);
    
    /**
     * Resolves which items should be displayed from a list of priority items.
     * Filters items by view requirements and sorts by priority for sequential display.
     * 
     * @param items List of priority items to resolve
     * @param player The player viewing the form
     * @param context Action context for condition evaluation
     * @param messageData Message data for condition evaluation
     * @return List of resolved items in priority order
     */
    public static List<PriorityItem> resolveItems(List<PriorityItem> items, FormPlayer player, 
                                                  ActionContext context, MessageData messageData) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<PriorityItem> resolvedItems = new ArrayList<>();
        
        // Filter items by view requirements and sort by priority
        for (PriorityItem item : items) {
            if (meetsViewRequirements(item, player, context, messageData)) {
                resolvedItems.add(item);
            }
        }
        
        // Sort by priority (lower numbers = higher priority)
        resolvedItems.sort(Comparator.comparingInt(PriorityItem::getPriority));
        
        logger.debug("Resolved {} items from {} input items", resolvedItems.size(), items.size());
        return resolvedItems;
    }
    

    

    
    /**
     * Checks if an item meets its view requirements
     */
    private static boolean meetsViewRequirements(PriorityItem item, FormPlayer player, 
                                                ActionContext context, MessageData messageData) {
        if (!item.hasViewRequirement()) {
            return true; // No requirements means always visible
        }
        
        try {
            return ConditionEvaluator.evaluateCondition(player, item.getViewRequirement(), context, messageData);
        } catch (Exception e) {
            logger.warn("Failed to evaluate view requirement for item '" + item.getText() + "': " + e.getMessage());
            return false; // Fail safe - don't show item if condition evaluation fails
        }
    }
    
    /**
     * Creates a priority-based form layout from a list of priority items.
     * This method handles the complete resolution process and returns items in priority order.
     */
    public static List<PriorityItem> createFormLayout(List<PriorityItem> items, FormPlayer player, 
                                                     ActionContext context, MessageData messageData) {
        // The resolveItems method already filters and sorts by priority
        return resolveItems(items, player, context, messageData);
    }
    
    /**
     * Utility method to convert regular FormButtons to PriorityItems with default priority
     */
    public static List<PriorityItem> convertToPriorityItems(List<it.pintux.life.common.form.obj.FormButton> buttons) {
        List<PriorityItem> priorityItems = new ArrayList<>();
        
        for (it.pintux.life.common.form.obj.FormButton button : buttons) {
            PriorityItem priorityItem = new PriorityItem(
                button.getText(), 
                button.getImage(), 
                button.getOnClick(), 
                1, // Default priority
                null // No view requirement
            );
            priorityItems.add(priorityItem);
        }
        
        return priorityItems;
    }
    
    /**
     * Debug method to log priority resolution details
     */
    public static void logPriorityResolution(List<PriorityItem> items, List<PriorityItem> resolved) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        
        logger.debug("Priority Resolution Summary:");
        logger.debug("Input items: " + items.size());
        logger.debug("Resolved items: " + resolved.size());
        
        logger.debug("All input items:");
        for (int i = 0; i < items.size(); i++) {
            PriorityItem item = items.get(i);
            boolean included = resolved.contains(item);
            logger.debug("  [{}] {} (priority: {}, requirement: {}) - {}", 
                       i, item.getText(), item.getPriority(), 
                       item.getViewRequirement() != null ? item.getViewRequirement() : "none",
                       included ? "INCLUDED" : "FILTERED OUT");
        }
        
        logger.debug("Final button order:");
        for (int i = 0; i < resolved.size(); i++) {
            PriorityItem item = resolved.get(i);
            logger.debug("  Position {}: {} (priority: {})", i, item.getText(), item.getPriority());
        }
    }
}