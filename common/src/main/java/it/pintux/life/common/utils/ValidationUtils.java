package it.pintux.life.common.utils;

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


    public static int parseSeconds(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        String value = raw.trim().toLowerCase();
        int multiplier = 1;
        if (value.endsWith("s")) {
            value = value.substring(0, value.length() - 1);
        } else if (value.endsWith("m")) {
            multiplier = 60;
            value = value.substring(0, value.length() - 1);
        } else if (value.endsWith("h")) {
            multiplier = 3600;
            value = value.substring(0, value.length() - 1);
        }
        try {
            return Math.max(0, Integer.parseInt(value.trim()) * multiplier);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }


    public static boolean isValidImageSource(String imageSource) {
        if (isNullOrEmpty(imageSource)) {
            return false;
        }

        String trimmed = imageSource.trim();
        if (trimmed.isEmpty()) return false;

        if (trimmed.contains("%") || trimmed.contains("{") || trimmed.contains("$")) {
            return true;
        }

        // Direct URLs and internal texture references
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("textures/")) {
            return true;
        }

        // Pack-relative paths under any other root, e.g. a custom resource pack folder
        if (trimmed.matches("^[A-Za-z0-9_.\\-]+(/[A-Za-z0-9_.\\-]+)+$")) {
            return true;
        }

        // Prefixed forms: head:<owner>, POTION:<effect>, TIPPED_ARROW:<effect>
        if (trimmed.matches("^[A-Za-z_]+:[A-Za-z0-9_.\\-]+$")) {
            return true;
        }

        // Mojang texture URLs
        if (trimmed.startsWith("http://textures.minecraft.net/texture/") ||
            trimmed.startsWith("https://textures.minecraft.net/texture/")) {
            return true;
        }

        // Base64-encoded textures from Minecraft-Heads.com API (JSON with textures.SKIN.url)
        // Accept typical Base64 character set and a reasonable minimum length
        if (trimmed.matches("^[A-Za-z0-9+/=]+$") && trimmed.length() > 40) {
            return true;
        }

        // Plain identifiers (player name, UUID, texture hash) mapped via image mapper
        if (trimmed.matches("^[A-Za-z0-9_.\\-]+$")) {
            return true;
        }

        // Local file references
        return trimmed.matches("^[A-Za-z0-9_./\\-]+\\.(png|jpg|jpeg|gif|webp)$");
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


}

