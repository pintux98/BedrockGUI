package it.pintux.life.common.utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Utility class for input validation and sanitization
 */
public final class ValidationUtils {
    
    private static final Pattern MENU_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]+$");
    
    private ValidationUtils() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Validates a menu name
     * @param menuName the menu name to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidMenuName(String menuName) {
        if (isNullOrEmpty(menuName)) {
            return false;
        }
        
        if (menuName.length() > 100) {
            return false;
        }
        
        return MENU_NAME_PATTERN.matcher(menuName).matches();
    }
    
    /**
     * Validates a UUID
     * @param uuid the UUID to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidUUID(UUID uuid) {
        return uuid != null;
    }
    
    /**
     * Validates a file path for security
     * @param filePath the file path to validate
     * @return true if safe, false otherwise
     */
    public static boolean isSafeFilePath(String filePath) {
        if (isNullOrEmpty(filePath)) {
            return false;
        }
        
        // Prevent directory traversal attacks
        if (filePath.contains("..") || filePath.contains("~")) {
            return false;
        }
        
        try {
            Path path = Paths.get(filePath);
            Path normalizedPath = path.normalize();
            
            // Ensure the path stays within safe directories
            return !normalizedPath.toString().contains("..");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Sanitizes a string by removing potentially dangerous characters
     * @param input the input string
     * @return sanitized string
     */
    public static String sanitizeString(String input) {
        if (input == null) {
            return "";
        }
        
        // Remove control characters and normalize whitespace
        return input.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "")
                   .replaceAll("\\s+", " ")
                   .trim();
    }
    
    /**
     * Validates a configuration value
     * @param value the value to validate
     * @param expectedType the expected type
     * @return true if valid, false otherwise
     */
    public static boolean isValidConfigValue(Object value, Class<?> expectedType) {
        if (value == null) {
            return false;
        }
        
        return expectedType.isAssignableFrom(value.getClass());
    }
    
    /**
     * Checks if a string is null or empty
     * @param str the string to check
     * @return true if null or empty, false otherwise
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * Validates an image URL or path
     * @param imageSource the image source to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidImageSource(String imageSource) {
        if (isNullOrEmpty(imageSource)) {
            return false;
        }
        
        // Allow HTTP(S) URLs and basic file paths
        return imageSource.startsWith("http://") ||
               imageSource.startsWith("https://") ||
               imageSource.startsWith("textures/");
    }
}