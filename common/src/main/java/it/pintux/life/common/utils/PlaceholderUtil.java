package it.pintux.life.common.utils;

import it.pintux.life.common.actions.ActionSystem;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PlaceholderUtil {

    private static final Pattern DYNAMIC_PLACEHOLDER_PATTERN = Pattern.compile("\\$(\\w+)");


    public static String processPlaceholders(String text, FormPlayer player, Object messageData) {
        if (text == null) {
            return null;
        }

        String result = text;


        if (messageData instanceof MessageData && result.contains("%")) {
            result = ((MessageData) messageData).replaceVariables(result, null, player);
        }

        return result;
    }


    public static String processPlaceholders(String text, Map<String, String> dynamicPlaceholders,
                                             FormPlayer player, MessageData messageData) {
        if (text == null) {
            return null;
        }

        String result = text;


        if (dynamicPlaceholders != null && !dynamicPlaceholders.isEmpty()) {
            for (Map.Entry<String, String> entry : dynamicPlaceholders.entrySet()) {
                String placeholder = "$" + entry.getKey();
                String value = entry.getValue() != null ? entry.getValue() : "";
                result = result.replace(placeholder, value);
            }
        }


        if (messageData != null && result.contains("%")) {
            result = messageData.replaceVariables(result, null, player);
        }

        return result;
    }


    public static String processDynamicPlaceholders(String text, Map<String, String> dynamicPlaceholders) {
        if (text == null || dynamicPlaceholders == null || dynamicPlaceholders.isEmpty()) {
            return text;
        }

        String result = text;
        for (Map.Entry<String, String> entry : dynamicPlaceholders.entrySet()) {
            String value = entry.getValue() != null ? entry.getValue() : "";

            String dollarPlaceholder = "$" + entry.getKey();
            String bracePlaceholder = "{" + entry.getKey() + "}";
            result = result.replace(dollarPlaceholder, value);
            result = result.replace(bracePlaceholder, value);
        }

        return result;
    }


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


    public static boolean containsDynamicPlaceholders(String text) {
        if (text == null) {
            return false;
        }
        return DYNAMIC_PLACEHOLDER_PATTERN.matcher(text).find();
    }


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


    public static String processPlaceholdersWithContext(String text, ActionSystem.ActionContext context, FormPlayer player) {
        if (context == null) {
            return text;
        }

        String result = processDynamicPlaceholders(text, context.getPlaceholders());
        result = processFormResults(result, context.getFormResults());

        if (context.getMetadata() != null && context.getMetadata().containsKey("messageData")) {
            Object messageData = context.getMetadata().get("messageData");
            result = processPlaceholders(result, player, messageData);
        }

        return result;
    }


    public static ActionSystem.ActionContext createContextWithBuiltinPlaceholders(FormPlayer player, Map<String, String> additionalPlaceholders) {
        ActionSystem.ActionContext.Builder builder = ActionSystem.ActionContext.builder();


        if (additionalPlaceholders != null) {
            builder.placeholders(additionalPlaceholders);
        }


        if (player != null) {
            builder.placeholder("player", player.getName())
                    .placeholder("uuid", player.getUniqueId().toString());


            populatePlatformSpecificPlaceholders(builder, player);
        }


        populateTimePlaceholders(builder);

        return builder.build();
    }


    public static ActionSystem.ActionContext createContextWithBuiltinPlaceholders(FormPlayer player, Map<String, String> additionalPlaceholders, MessageData messageData) {
        Map<String, String> placeholders = new HashMap<>();


        populateBuiltinPlaceholders(placeholders, player);


        if (additionalPlaceholders != null) {
            placeholders.putAll(additionalPlaceholders);
        }

        Map<String, Object> metadata = new HashMap<>();
        if (messageData != null) {
            metadata.put("messageData", messageData);
        }

        return ActionSystem.ActionContext.builder()
                .placeholders(placeholders)
                .metadata(metadata)
                .build();
    }


    public static ActionSystem.ActionContext addBuiltinPlaceholders(ActionSystem.ActionContext context, FormPlayer player) {
        if (context == null) {
            return createContextWithBuiltinPlaceholders(player, null);
        }

        return createContextWithBuiltinPlaceholders(player, context.getPlaceholders());
    }


    private static void populateBuiltinPlaceholders(Map<String, String> placeholders, FormPlayer player) {

        if (player != null) {
            placeholders.put("player", player.getName());
            placeholders.put("uuid", player.getUniqueId().toString());


            populatePlatformSpecificPlaceholders(placeholders, player);
        }


        populateTimePlaceholders(placeholders);
    }


    private static void populatePlatformSpecificPlaceholders(ActionSystem.ActionContext.Builder builder, FormPlayer player) {
        try {

            if (player.getClass().getName().contains("PaperPlayer")) {
                populatePaperPlaceholders(builder, player);
            } else {

                populateDefaultPlaceholders(builder, player);
            }
        } catch (Exception e) {

            populateDefaultPlaceholders(builder, player);
        }
    }


    private static void populatePaperPlaceholders(ActionSystem.ActionContext.Builder builder, FormPlayer player) {
        try {

            java.lang.reflect.Field playerField = player.getClass().getDeclaredField("player");
            playerField.setAccessible(true);
            Object bukkitPlayer = playerField.get(player);

            if (bukkitPlayer != null) {

                Object location = bukkitPlayer.getClass().getMethod("getLocation").invoke(bukkitPlayer);
                if (location != null) {
                    double x = (Double) location.getClass().getMethod("getX").invoke(location);
                    double y = (Double) location.getClass().getMethod("getY").invoke(location);
                    double z = (Double) location.getClass().getMethod("getZ").invoke(location);

                    builder.placeholder("x", String.format("%.2f", x))
                            .placeholder("y", String.format("%.2f", y))
                            .placeholder("z", String.format("%.2f", z));


                    Object world = location.getClass().getMethod("getWorld").invoke(location);
                    if (world != null) {
                        String worldName = (String) world.getClass().getMethod("getName").invoke(world);
                        builder.placeholder("world", worldName);
                    }
                }


                try {
                    double health = (Double) bukkitPlayer.getClass().getMethod("getHealth").invoke(bukkitPlayer);
                    int foodLevel = (Integer) bukkitPlayer.getClass().getMethod("getFoodLevel").invoke(bukkitPlayer);

                    builder.placeholder("health", String.format("%.1f", health))
                            .placeholder("food", String.valueOf(foodLevel));
                } catch (Exception ignored) {

                }
            }
        } catch (Exception e) {

            populateDefaultPlaceholders(builder, player);
        }
    }


    private static void populateDefaultPlaceholders(ActionSystem.ActionContext.Builder builder, FormPlayer player) {
        builder.placeholder("x", "0.0")
                .placeholder("y", "64.0")
                .placeholder("z", "0.0")
                .placeholder("world", "world")
                .placeholder("health", "20.0")
                .placeholder("food", "20");
    }


    private static void populatePlatformSpecificPlaceholders(Map<String, String> placeholders, FormPlayer player) {
        try {

            if (player.getClass().getName().contains("PaperPlayer")) {
                populatePaperPlaceholders(placeholders, player);
            } else {

                populateDefaultPlaceholders(placeholders, player);
            }
        } catch (Exception e) {

            populateDefaultPlaceholders(placeholders, player);
        }
    }


    private static void populatePaperPlaceholders(Map<String, String> placeholders, FormPlayer player) {
        try {

            java.lang.reflect.Field playerField = player.getClass().getDeclaredField("player");
            playerField.setAccessible(true);
            Object bukkitPlayer = playerField.get(player);

            if (bukkitPlayer != null) {

                Object location = bukkitPlayer.getClass().getMethod("getLocation").invoke(bukkitPlayer);
                if (location != null) {
                    double x = (Double) location.getClass().getMethod("getX").invoke(location);
                    double y = (Double) location.getClass().getMethod("getY").invoke(location);
                    double z = (Double) location.getClass().getMethod("getZ").invoke(location);

                    placeholders.put("x", String.format("%.2f", x));
                    placeholders.put("y", String.format("%.2f", y));
                    placeholders.put("z", String.format("%.2f", z));


                    Object world = location.getClass().getMethod("getWorld").invoke(location);
                    if (world != null) {
                        String worldName = (String) world.getClass().getMethod("getName").invoke(world);
                        placeholders.put("world", worldName);
                    }
                }


                try {
                    double health = (Double) bukkitPlayer.getClass().getMethod("getHealth").invoke(bukkitPlayer);
                    int foodLevel = (Integer) bukkitPlayer.getClass().getMethod("getFoodLevel").invoke(bukkitPlayer);

                    placeholders.put("health", String.format("%.1f", health));
                    placeholders.put("food", String.valueOf(foodLevel));
                } catch (Exception ignored) {

                }
            }
        } catch (Exception e) {

            populateDefaultPlaceholders(placeholders, player);
        }
    }


    private static void populateDefaultPlaceholders(Map<String, String> placeholders, FormPlayer player) {
        placeholders.put("x", "0.0");
        placeholders.put("y", "64.0");
        placeholders.put("z", "0.0");
        placeholders.put("world", "world");
        placeholders.put("health", "20.0");
        placeholders.put("food", "20");
    }


    private static void populateTimePlaceholders(ActionSystem.ActionContext.Builder builder) {
        LocalTime now = LocalTime.now();


        int hour = now.getHour();
        int minute = now.getMinute();
        int totalMinutes = hour * 60 + minute;

        int ticks = (totalMinutes * 24000) / (24 * 60);

        builder.placeholder("time", String.valueOf(ticks))
                .placeholder("hour", String.valueOf(hour))
                .placeholder("minute", String.valueOf(minute))
                .placeholder("timestamp", String.valueOf(System.currentTimeMillis()));
    }


    private static void populateTimePlaceholders(Map<String, String> placeholders) {
        LocalTime now = LocalTime.now();


        int hour = now.getHour();
        int minute = now.getMinute();
        int totalMinutes = hour * 60 + minute;

        int ticks = (totalMinutes * 24000) / (24 * 60);

        placeholders.put("time", String.valueOf(ticks));
        placeholders.put("hour", String.valueOf(hour));
        placeholders.put("minute", String.valueOf(minute));
        placeholders.put("timestamp", String.valueOf(System.currentTimeMillis()));
    }
}

