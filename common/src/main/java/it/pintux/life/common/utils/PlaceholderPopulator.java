package it.pintux.life.common.utils;

import it.pintux.life.common.actions.ActionContext;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for populating built-in placeholders in ActionContext
 */
public class PlaceholderPopulator {
    
    /**
     * Creates an ActionContext with built-in placeholders populated
     * @param player The player for context
     * @param additionalPlaceholders Additional placeholders to include
     * @return ActionContext with built-in placeholders
     */
    public static ActionContext createContextWithBuiltinPlaceholders(FormPlayer player, Map<String, String> additionalPlaceholders) {
        ActionContext.Builder builder = ActionContext.builder();
        
        // Add additional placeholders first
        if (additionalPlaceholders != null) {
            builder.placeholders(additionalPlaceholders);
        }
        
        // Add basic player placeholders
        if (player != null) {
            builder.placeholder("player", player.getName())
                   .placeholder("uuid", player.getUniqueId().toString());
            
            // Add platform-specific placeholders
            populatePlatformSpecificPlaceholders(builder, player);
        }
        
        // Add time-based placeholders
        populateTimePlaceholders(builder);
        
        return builder.build();
    }
    
    /**
     * Creates an ActionContext with built-in placeholders and MessageData
     * @param player The player to get data from
     * @param additionalPlaceholders Additional placeholders to include
     * @param messageData MessageData instance for PlaceholderAPI processing
     * @return ActionContext with built-in placeholders and MessageData
     */
    public static ActionContext createContextWithBuiltinPlaceholders(FormPlayer player, Map<String, String> additionalPlaceholders, it.pintux.life.common.utils.MessageData messageData) {
        Map<String, String> placeholders = new HashMap<>();
        
        // Add built-in placeholders
        populateBuiltinPlaceholders(placeholders, player);
        
        // Add additional placeholders if provided
        if (additionalPlaceholders != null) {
            placeholders.putAll(additionalPlaceholders);
        }
        
        Map<String, Object> metadata = new HashMap<>();
        if (messageData != null) {
            metadata.put("messageData", messageData);
        }
        
        return ActionContext.builder()
                .placeholders(placeholders)
                .metadata(metadata)
                .build();
    }
    
    /**
     * Populates built-in placeholders into a Map
     * @param placeholders The map to populate
     * @param player The player
     */
    private static void populateBuiltinPlaceholders(Map<String, String> placeholders, FormPlayer player) {
        // Add basic player placeholders
        if (player != null) {
            placeholders.put("player", player.getName());
            placeholders.put("uuid", player.getUniqueId().toString());
            
            // Add platform-specific placeholders
            populatePlatformSpecificPlaceholders(placeholders, player);
        }
        
        // Add time-based placeholders
        populateTimePlaceholders(placeholders);
    }
    
    /**
     * Populates platform-specific placeholders (location, world, etc.)
     * @param builder The ActionContext builder
     * @param player The player
     */
    private static void populatePlatformSpecificPlaceholders(ActionContext.Builder builder, FormPlayer player) {
        try {
            // Check if this is a PaperPlayer with Bukkit Player access
            if (player.getClass().getName().contains("PaperPlayer")) {
                populatePaperPlaceholders(builder, player);
            } else {
                // For Geyser or other platforms, provide default values
                populateDefaultPlaceholders(builder, player);
            }
        } catch (Exception e) {
            // Fallback to default placeholders if reflection fails
            populateDefaultPlaceholders(builder, player);
        }
    }
    
    /**
     * Populates placeholders for Paper platform using reflection
     * @param builder The ActionContext builder
     * @param player The player
     */
    private static void populatePaperPlaceholders(ActionContext.Builder builder, FormPlayer player) {
        try {
            // Use reflection to access the Bukkit Player object
            java.lang.reflect.Field playerField = player.getClass().getDeclaredField("player");
            playerField.setAccessible(true);
            Object bukkitPlayer = playerField.get(player);
            
            if (bukkitPlayer != null) {
                // Get location using reflection
                Object location = bukkitPlayer.getClass().getMethod("getLocation").invoke(bukkitPlayer);
                if (location != null) {
                    double x = (Double) location.getClass().getMethod("getX").invoke(location);
                    double y = (Double) location.getClass().getMethod("getY").invoke(location);
                    double z = (Double) location.getClass().getMethod("getZ").invoke(location);
                    
                    builder.placeholder("x", String.format("%.2f", x))
                           .placeholder("y", String.format("%.2f", y))
                           .placeholder("z", String.format("%.2f", z));
                    
                    // Get world name
                    Object world = location.getClass().getMethod("getWorld").invoke(location);
                    if (world != null) {
                        String worldName = (String) world.getClass().getMethod("getName").invoke(world);
                        builder.placeholder("world", worldName);
                    }
                }
                
                // Get health and food level
                try {
                    double health = (Double) bukkitPlayer.getClass().getMethod("getHealth").invoke(bukkitPlayer);
                    int foodLevel = (Integer) bukkitPlayer.getClass().getMethod("getFoodLevel").invoke(bukkitPlayer);
                    
                    builder.placeholder("health", String.format("%.1f", health))
                           .placeholder("food", String.valueOf(foodLevel));
                } catch (Exception ignored) {
                    // Health/food methods might not be available
                }
            }
        } catch (Exception e) {
            // If reflection fails, fall back to default placeholders
            populateDefaultPlaceholders(builder, player);
        }
    }
    
    /**
     * Populates default placeholders for non-Paper platforms
     * @param builder The ActionContext builder
     * @param player The player
     */
    private static void populateDefaultPlaceholders(ActionContext.Builder builder, FormPlayer player) {
        builder.placeholder("x", "0.0")
               .placeholder("y", "64.0")
               .placeholder("z", "0.0")
               .placeholder("world", "world")
               .placeholder("health", "20.0")
               .placeholder("food", "20");
    }
    
    /**
     * Populates platform-specific placeholders into a Map
     * @param placeholders The map to populate
     * @param player The player
     */
    private static void populatePlatformSpecificPlaceholders(Map<String, String> placeholders, FormPlayer player) {
        try {
            // Check if this is a PaperPlayer with Bukkit Player access
            if (player.getClass().getName().contains("PaperPlayer")) {
                populatePaperPlaceholders(placeholders, player);
            } else {
                // For Geyser or other platforms, provide default values
                populateDefaultPlaceholders(placeholders, player);
            }
        } catch (Exception e) {
            // Fallback to default placeholders if reflection fails
            populateDefaultPlaceholders(placeholders, player);
        }
    }
    
    /**
     * Populates placeholders for Paper platform using reflection (Map version)
     * @param placeholders The map to populate
     * @param player The player
     */
    private static void populatePaperPlaceholders(Map<String, String> placeholders, FormPlayer player) {
        try {
            // Use reflection to access the Bukkit Player object
            java.lang.reflect.Field playerField = player.getClass().getDeclaredField("player");
            playerField.setAccessible(true);
            Object bukkitPlayer = playerField.get(player);
            
            if (bukkitPlayer != null) {
                // Get location using reflection
                Object location = bukkitPlayer.getClass().getMethod("getLocation").invoke(bukkitPlayer);
                if (location != null) {
                    double x = (Double) location.getClass().getMethod("getX").invoke(location);
                    double y = (Double) location.getClass().getMethod("getY").invoke(location);
                    double z = (Double) location.getClass().getMethod("getZ").invoke(location);
                    
                    placeholders.put("x", String.format("%.2f", x));
                    placeholders.put("y", String.format("%.2f", y));
                    placeholders.put("z", String.format("%.2f", z));
                    
                    // Get world name
                    Object world = location.getClass().getMethod("getWorld").invoke(location);
                    if (world != null) {
                        String worldName = (String) world.getClass().getMethod("getName").invoke(world);
                        placeholders.put("world", worldName);
                    }
                }
                
                // Get health and food level
                try {
                    double health = (Double) bukkitPlayer.getClass().getMethod("getHealth").invoke(bukkitPlayer);
                    int foodLevel = (Integer) bukkitPlayer.getClass().getMethod("getFoodLevel").invoke(bukkitPlayer);
                    
                    placeholders.put("health", String.format("%.1f", health));
                    placeholders.put("food", String.valueOf(foodLevel));
                } catch (Exception ignored) {
                    // Health/food methods might not be available
                }
            }
        } catch (Exception e) {
            // If reflection fails, fall back to default placeholders
            populateDefaultPlaceholders(placeholders, player);
        }
    }
    
    /**
     * Populates default placeholders for non-Paper platforms (Map version)
     * @param placeholders The map to populate
     * @param player The player
     */
    private static void populateDefaultPlaceholders(Map<String, String> placeholders, FormPlayer player) {
        placeholders.put("x", "0.0");
        placeholders.put("y", "64.0");
        placeholders.put("z", "0.0");
        placeholders.put("world", "world");
        placeholders.put("health", "20.0");
        placeholders.put("food", "20");
    }
    
    /**
     * Populates time-based placeholders
     * @param builder The ActionContext builder
     */
    private static void populateTimePlaceholders(ActionContext.Builder builder) {
        LocalTime now = LocalTime.now();
        
        // Time in ticks (Minecraft time format: 0-24000)
        // 6:00 AM = 0 ticks, 12:00 PM = 6000 ticks, 6:00 PM = 12000 ticks, 12:00 AM = 18000 ticks
        int hour = now.getHour();
        int minute = now.getMinute();
        int totalMinutes = hour * 60 + minute;
        // Convert to Minecraft ticks (24000 ticks = 24 hours)
        int ticks = (totalMinutes * 24000) / (24 * 60);
        
        builder.placeholder("time", String.valueOf(ticks))
               .placeholder("hour", String.valueOf(hour))
               .placeholder("minute", String.valueOf(minute))
               .placeholder("timestamp", String.valueOf(System.currentTimeMillis()));
    }
    
    /**
     * Populates time-based placeholders into a Map
     * @param placeholders The map to populate
     */
    private static void populateTimePlaceholders(Map<String, String> placeholders) {
        LocalTime now = LocalTime.now();
        
        // Time in ticks (Minecraft time format: 0-24000)
        // 6:00 AM = 0 ticks, 12:00 PM = 6000 ticks, 6:00 PM = 12000 ticks, 12:00 AM = 18000 ticks
        int hour = now.getHour();
        int minute = now.getMinute();
        int totalMinutes = hour * 60 + minute;
        // Convert to Minecraft ticks (24000 ticks = 24 hours)
        int ticks = (totalMinutes * 24000) / (24 * 60);
        
        placeholders.put("time", String.valueOf(ticks));
        placeholders.put("hour", String.valueOf(hour));
        placeholders.put("minute", String.valueOf(minute));
        placeholders.put("timestamp", String.valueOf(System.currentTimeMillis()));
    }
    
    /**
     * Updates an existing ActionContext with built-in placeholders
     * @param context The existing context
     * @param player The player
     * @return New ActionContext with built-in placeholders added
     */
    public static ActionContext addBuiltinPlaceholders(ActionContext context, FormPlayer player) {
        if (context == null) {
            return createContextWithBuiltinPlaceholders(player, null);
        }
        
        return createContextWithBuiltinPlaceholders(player, context.getPlaceholders());
    }
}