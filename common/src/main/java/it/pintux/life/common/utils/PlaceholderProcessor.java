package it.pintux.life.common.utils;

import java.util.Map;

/**
 * PlaceholderProcessor provides a simplified interface for processing placeholders
 * in text strings. This class acts as a wrapper around PlaceholderUtil for
 * backward compatibility and cleaner API usage.
 */
public class PlaceholderProcessor {
    
    /**
     * Process placeholders in text for a specific player
     * @param text The text to process
     * @param player The player for context
     * @return Processed text with placeholders replaced
     */
    public String processPlaceholders(String text, FormPlayer player) {
        return PlaceholderUtil.processPlaceholders(text, player, null);
    }
    
    /**
     * Process placeholders in text with additional dynamic placeholders
     * @param text The text to process
     * @param dynamicPlaceholders Map of dynamic placeholders
     * @param player The player for context
     * @return Processed text with placeholders replaced
     */
    public String processPlaceholders(String text, Map<String, String> dynamicPlaceholders, FormPlayer player) {
        return PlaceholderUtil.processPlaceholders(text, dynamicPlaceholders, player, null);
    }
    
    /**
     * Process placeholders in text with MessageData for PlaceholderAPI integration
     * @param text The text to process
     * @param player The player for context
     * @param messageData MessageData instance for PlaceholderAPI
     * @return Processed text with placeholders replaced
     */
    public String processPlaceholders(String text, FormPlayer player, Object messageData) {
        return PlaceholderUtil.processPlaceholders(text, player, messageData);
    }
    
    /**
     * Process only dynamic placeholders ($ prefixed and {} wrapped)
     * @param text The text to process
     * @param dynamicPlaceholders Map of dynamic placeholders
     * @return Text with dynamic placeholders replaced
     */
    public String processDynamicPlaceholders(String text, Map<String, String> dynamicPlaceholders) {
        return PlaceholderUtil.processDynamicPlaceholders(text, dynamicPlaceholders);
    }
    
    /**
     * Process form results as dynamic placeholders
     * @param text The text to process
     * @param formResults Map of form results
     * @return Text with form result placeholders replaced
     */
    public String processFormResults(String text, Map<String, Object> formResults) {
        return PlaceholderUtil.processFormResults(text, formResults);
    }
}