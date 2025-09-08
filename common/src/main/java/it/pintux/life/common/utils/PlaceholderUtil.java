package it.pintux.life.common.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Unified placeholder utility for handling different types of placeholders:
 * - '$' placeholders: Dynamic replacements from player input or form results
 * - '%' placeholders: PlaceholderAPI placeholders
 */
public class PlaceholderUtil {
    
    private static final Pattern DYNAMIC_PLACEHOLDER_PATTERN = Pattern.compile("\\$(\\w+)");
    
    /**
     * Process all placeholders in a text string (overloaded for action handlers)
     * @param text The text to process
     * @param player The player for PlaceholderAPI processing
     * @param messageData MessageData instance for PlaceholderAPI integration
     * @return Processed text with all placeholders replaced
     */
    public static String processPlaceholders(String text, FormPlayer player, Object messageData) {
        if (text == null) {
            return null;
        }
        
        String result = text;
        
        // Process PlaceholderAPI placeholders (% prefixed) if messageData is available
        if (messageData instanceof MessageData && result.contains("%")) {
            result = ((MessageData) messageData).replaceVariables(result, null, player);
        }
        
        return result;
    }
    
    /**
     * Process all placeholders in a text string
     * @param text The text to process
     * @param dynamicPlaceholders Map of dynamic placeholders ($ prefixed)
     * @param player The player for PlaceholderAPI processing
     * @param messageData MessageData instance for PlaceholderAPI integration
     * @return Processed text with all placeholders replaced
     */
    public static String processPlaceholders(String text, Map<String, String> dynamicPlaceholders, 
                                           FormPlayer player, MessageData messageData) {
        if (text == null) {
            return null;
        }
        
        String result = text;
        
        // First, replace dynamic placeholders ($ prefixed)
        if (dynamicPlaceholders != null && !dynamicPlaceholders.isEmpty()) {
            for (Map.Entry<String, String> entry : dynamicPlaceholders.entrySet()) {
                String placeholder = "$" + entry.getKey();
                String value = entry.getValue() != null ? entry.getValue() : "";
                result = result.replace(placeholder, value);
            }
        }
        
        // Then, process PlaceholderAPI placeholders (% prefixed) if messageData is available
        if (messageData != null && result.contains("%")) {
            result = messageData.replaceVariables(result, null, player);
        }
        
        return result;
    }
    
    /**
     * Process only dynamic placeholders ($ prefixed and {} wrapped)
     * @param text The text to process
     * @param dynamicPlaceholders Map of dynamic placeholders
     * @return Text with dynamic placeholders replaced
     */
    public static String processDynamicPlaceholders(String text, Map<String, String> dynamicPlaceholders) {
        if (text == null || dynamicPlaceholders == null || dynamicPlaceholders.isEmpty()) {
            return text;
        }
        
        String result = text;
        for (Map.Entry<String, String> entry : dynamicPlaceholders.entrySet()) {
            String value = entry.getValue() != null ? entry.getValue() : "";
            // Handle both $ prefixed and {} wrapped placeholders
            String dollarPlaceholder = "$" + entry.getKey();
            String bracePlaceholder = "{" + entry.getKey() + "}";
            result = result.replace(dollarPlaceholder, value);
            result = result.replace(bracePlaceholder, value);
        }
        
        return result;
    }
    
    /**
     * Process form results as dynamic placeholders
     * @param text The text to process
     * @param formResults Map of form results
     * @return Text with form result placeholders replaced
     */
    public static String processFormResults(String text, Map<String, Object> formResults) {
        if (text == null || formResults == null || formResults.isEmpty()) {
            return text;
        }
        
        String result = text;
        for (Map.Entry<String, Object> entry : formResults.entrySet()) {
            String placeholder = "$" + entry.getKey();
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        
        return result;
    }
    
    /**
     * Check if text contains dynamic placeholders ($ prefixed)
     * @param text The text to check
     * @return true if contains dynamic placeholders, false otherwise
     */
    public static boolean containsDynamicPlaceholders(String text) {
        if (text == null) {
            return false;
        }
        return DYNAMIC_PLACEHOLDER_PATTERN.matcher(text).find();
    }
    
    /**
     * Count the number of numbered placeholders ($1, $2, etc.)
     * @param text The text to analyze
     * @return The highest numbered placeholder found
     */
    public static int countNumberedPlaceholders(String text) {
        if (text == null) {
            return 0;
        }
        
        int count = 0;
        while (text.contains("$" + (count + 1))) {
            count++;
        }
        return count;
    }
    
    /**
     * Log missing placeholders for debugging
     * @param text The text to check
     * @param logger Logger instance
     */
    public static void logMissingPlaceholders(String text, Logger logger) {
        if (text == null || logger == null) {
            return;
        }
        
        Matcher matcher = DYNAMIC_PLACEHOLDER_PATTERN.matcher(text);
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            logger.warn("Missing replacement value for placeholder: $" + placeholder);
        }
    }
}