package it.pintux.life.essentialsaddon.util;

import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.util.Colorizer;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.entity.Player;

/**
 * Thin bridge to MyPet's own localized messages so the addon reuses MyPet's language files
 * instead of duplicating strings. Only call when MyPet is present (the pet module is gated on
 * that); each method degrades to an empty/raw string if MyPet's API is unexpectedly missing.
 */
public final class MyPetMessages {

    private MyPetMessages() {
    }

    /** MyPet's localized string for {@code key}, or "" if unavailable. */
    public static String get(String key, Player player) {
        try {
            return Translation.getString(key, player);
        } catch (Throwable t) {
            return "";
        }
    }

    /** MyPet's localized string for {@code key} with %s-style placeholders filled in. */
    public static String format(String key, Player player, Object... args) {
        try {
            return Util.formatText(Translation.getString(key, player), args);
        } catch (Throwable t) {
            return "";
        }
    }

    /** Translate MyPet's color codes in an arbitrary string (e.g. a skilltree display name). */
    public static String colorize(String text) {
        try {
            return Colorizer.setColors(text == null ? "" : text);
        } catch (Throwable t) {
            return text == null ? "" : text;
        }
    }
}
