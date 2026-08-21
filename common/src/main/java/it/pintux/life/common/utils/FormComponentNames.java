package it.pintux.life.common.utils;

/**
 * Derives the key a custom form component's answer is stored under.
 *
 * <p>Labels carry colour codes and punctuation, which used to end up inside the key and left a
 * handler reading {@code results.get("home_name")} with nothing to find.</p>
 */
public final class FormComponentNames {

    private static final String FALLBACK = "value";

    private FormComponentNames() {
    }

    public static String derive(String label) {
        if (label == null) {
            return FALLBACK;
        }
        String stripped = label.replaceAll("(?i)[&§][0-9a-fk-or]", "").trim().toLowerCase();
        String cleaned = stripped.replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return cleaned.isEmpty() ? FALLBACK : cleaned;
    }
}
