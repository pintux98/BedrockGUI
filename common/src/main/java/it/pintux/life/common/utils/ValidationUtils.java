package it.pintux.life.common.utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;


public final class ValidationUtils {

    private static final Pattern MENU_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]+$");

    private ValidationUtils() {

    }


    public static boolean isValidMenuName(String menuName) {
        if (isNullOrEmpty(menuName)) {
            return false;
        }

        if (menuName.length() > 100) {
            return false;
        }

        return MENU_NAME_PATTERN.matcher(menuName).matches();
    }


    public static boolean isValidUUID(UUID uuid) {
        return uuid != null;
    }


    public static boolean isSafeFilePath(String filePath) {
        if (isNullOrEmpty(filePath)) {
            return false;
        }


        if (filePath.contains("..") || filePath.contains("~")) {
            return false;
        }

        try {
            Path path = Paths.get(filePath);
            Path normalizedPath = path.normalize();


            return !normalizedPath.toString().contains("..");
        } catch (Exception e) {
            return false;
        }
    }


    public static String sanitizeString(String input) {
        if (input == null) {
            return "";
        }


        return input.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }


    public static boolean isValidConfigValue(Object value, Class<?> expectedType) {
        if (value == null) {
            return false;
        }

        return expectedType.isAssignableFrom(value.getClass());
    }


    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }


    public static boolean isValidImageSource(String imageSource) {
        if (isNullOrEmpty(imageSource)) {
            return false;
        }


        return imageSource.startsWith("http://") ||
               imageSource.startsWith("https://") ||
               imageSource.startsWith("textures/");
    }


    public static boolean isValidActionFormat(String actionValue, int minParts) {
        if (isNullOrEmpty(actionValue)) {
            return false;
        }

        String[] parts = actionValue.split(":");
        return parts.length >= minParts;
    }


    public static boolean isValidCommand(String command) {
        if (isNullOrEmpty(command)) {
            return false;
        }


        String lowerCommand = command.toLowerCase().trim();
        String[] dangerousCommands = {
                "rm ", "del ", "format", "shutdown", "reboot",
                "kill", "sudo", "chmod", "chown", "passwd"
        };

        for (String dangerous : dangerousCommands) {
            if (lowerCommand.startsWith(dangerous)) {
                return false;
            }
        }

        return true;
    }


    public static boolean isValidNumericRange(String value, double min, double max) {
        if (isNullOrEmpty(value)) {
            return false;
        }

        try {
            double numValue = Double.parseDouble(value);
            return numValue >= min && numValue <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    public static boolean isValidPlayerName(String playerName) {
        if (isNullOrEmpty(playerName)) {
            return false;
        }


        return playerName.matches("^[a-zA-Z0-9_]{3,16}$");
    }


    public static boolean isValidCoordinates(String x, String y, String z) {
        try {
            Double.parseDouble(x);
            Double.parseDouble(y);
            Double.parseDouble(z);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    public static boolean isValidGameMode(String gameMode) {
        if (isNullOrEmpty(gameMode)) {
            return false;
        }

        String mode = gameMode.toLowerCase();
        return mode.equals("survival") || mode.equals("creative") ||
               mode.equals("adventure") || mode.equals("spectator") ||
               mode.equals("0") || mode.equals("1") || mode.equals("2") || mode.equals("3") ||
               mode.equals("s") || mode.equals("c") || mode.equals("a") || mode.equals("sp");
    }


    public static boolean isValidItemId(String itemId) {
        if (isNullOrEmpty(itemId)) {
            return false;
        }


        return itemId.matches("^[a-z0-9_:]+$") && !itemId.startsWith(":") && !itemId.endsWith(":");
    }


    public static boolean isValidPotionEffect(String effectName) {
        if (isNullOrEmpty(effectName)) {
            return false;
        }


        return effectName.matches("^[a-z0-9_:]+$");
    }
}
