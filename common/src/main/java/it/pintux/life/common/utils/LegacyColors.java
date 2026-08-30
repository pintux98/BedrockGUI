package it.pintux.life.common.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LegacyColors {

    private static final char SECTION = '§';
    private static final String CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx";
    private static final Pattern HEX_ANGLE = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    private static final Pattern HEX_AMP = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private LegacyColors() {
    }

    public static String translate(String message) {
        if (message == null) {
            return null;
        }
        return translateAmpersands(expandHex(message));
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
