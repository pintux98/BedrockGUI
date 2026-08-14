package it.pintux.life.duelsaddon.util;

import java.util.Locale;

/**
 * Number and label formatting for the stat and kit forms.
 */
public final class Formatting {
    private Formatting() {}

    /**
     * Divides two counts for display, treating a zero denominator as "every kill was free" rather
     * than as an error, which is what a K/D of {@code kills / 0} means in practice.
     *
     * @return the ratio to two decimal places
     */
    public static String ratio(int numerator, int denominator) {
        if (denominator <= 0) {
            return numerator <= 0 ? "0.00" : String.format(Locale.ROOT, "%.2f", (double) numerator);
        }
        return String.format(Locale.ROOT, "%.2f", (double) numerator / denominator);
    }

    /**
     * @return {@code part} as a percentage of {@code total} to one decimal place, or {@code 0.0}
     *         when nothing has been played
     */
    public static String percent(int part, int total) {
        if (total <= 0) {
            return "0.0";
        }
        return String.format(Locale.ROOT, "%.1f", (part * 100.0d) / total);
    }

    /**
     * Turns an identifier into a label, so a mode or material with no display name still reads
     * properly: {@code DIAMOND_SWORD} becomes {@code Diamond Sword}.
     */
    public static String prettify(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String[] words = raw.replace('_', ' ').replace('-', ' ').trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.toString();
    }
}
