package it.pintux.life.common.utils;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LegacyColors {

    private static final char SECTION = '§';
    private static final String CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx";
    private static final Pattern HEX_ANGLE = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    private static final Pattern HEX_AMP = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern TAG = Pattern.compile("<(/?)([A-Za-z_]+)>");
    private static final Map<String, String> TAGS = Map.ofEntries(
            Map.entry("black", "§0"),
            Map.entry("dark_blue", "§1"),
            Map.entry("dark_green", "§2"),
            Map.entry("dark_aqua", "§3"),
            Map.entry("dark_red", "§4"),
            Map.entry("dark_purple", "§5"),
            Map.entry("gold", "§6"),
            Map.entry("gray", "§7"),
            Map.entry("grey", "§7"),
            Map.entry("dark_gray", "§8"),
            Map.entry("dark_grey", "§8"),
            Map.entry("blue", "§9"),
            Map.entry("green", "§a"),
            Map.entry("aqua", "§b"),
            Map.entry("red", "§c"),
            Map.entry("light_purple", "§d"),
            Map.entry("yellow", "§e"),
            Map.entry("white", "§f"),
            Map.entry("obfuscated", "§k"),
            Map.entry("obf", "§k"),
            Map.entry("bold", "§l"),
            Map.entry("b", "§l"),
            Map.entry("strikethrough", "§m"),
            Map.entry("st", "§m"),
            Map.entry("underlined", "§n"),
            Map.entry("u", "§n"),
            Map.entry("italic", "§o"),
            Map.entry("i", "§o"),
            Map.entry("em", "§o"),
            Map.entry("reset", "§r"));

    private LegacyColors() {
    }

    public static String translate(String message) {
        if (message == null) {
            return null;
        }
        return translateAmpersands(expandTags(expandHex(message)));
    }

    private static String expandHex(String message) {
        String result = replaceHex(message, HEX_ANGLE);
        return replaceHex(result, HEX_AMP);
    }

    private static String replaceHex(String message, Pattern pattern) {
        Matcher matcher = pattern.matcher(message);
        StringBuilder out = new StringBuilder(message.length());
        int last = 0;
        while (matcher.find()) {
            out.append(message, last, matcher.start()).append(toHexSequence(matcher.group(1)));
            last = matcher.end();
        }
        return last == 0 ? message : out.append(message.substring(last)).toString();
    }

    private static String toHexSequence(String hex) {
        StringBuilder sequence = new StringBuilder(14).append(SECTION).append('x');
        for (char digit : hex.toCharArray()) {
            sequence.append(SECTION).append(Character.toLowerCase(digit));
        }
        return sequence.toString();
    }

    /**
     * Rewrites the MiniMessage style tags plugins such as EconomyShopGUI accept into their legacy equivalents.
     * Closing tags are dropped because legacy formatting cannot end one decoration without resetting the rest,
     * and unknown tags are left alone so ordinary text keeps its angle brackets.
     */
    private static String expandTags(String message) {
        Matcher matcher = TAG.matcher(message);
        StringBuilder out = new StringBuilder(message.length());
        int last = 0;
        while (matcher.find()) {
            String code = TAGS.get(matcher.group(2).toLowerCase(Locale.ROOT));
            if (code == null) {
                continue;
            }
            out.append(message, last, matcher.start()).append(matcher.group(1).isEmpty() ? code : "");
            last = matcher.end();
        }
        return last == 0 ? message : out.append(message.substring(last)).toString();
    }

    private static String translateAmpersands(String message) {
        char[] chars = message.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && CODES.indexOf(chars[i + 1]) > -1) {
                chars[i] = SECTION;
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }
}
