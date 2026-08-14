package it.pintux.life.duelsaddon.util;

import java.util.Locale;

public final class Formatting {
    private Formatting() {}

    public static String ratio(int numerator, int denominator) {
        if (denominator <= 0) {
            return numerator <= 0 ? "0.00" : String.format(Locale.ROOT, "%.2f", (double) numerator);
        }
        return String.format(Locale.ROOT, "%.2f", (double) numerator / denominator);
    }

    public static String percent(int part, int total) {
        if (total <= 0) {
            return "0.0";
        }
        return String.format(Locale.ROOT, "%.1f", (part * 100.0d) / total);
    }

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

    public static String stripColor(String input) {
        return input == null ? "" : input.replaceAll("(?i)[§&][0-9A-FK-ORX]", "");
    }
}
