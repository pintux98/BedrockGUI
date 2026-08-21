package it.pintux.life.essentialsaddon.util;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CommandAliases {

    private final Set<String> roots;

    private CommandAliases(Set<String> roots) {
        this.roots = roots;
    }

    public static CommandAliases of(Collection<String> configured) {
        Set<String> roots = new LinkedHashSet<>();
        if (configured != null) {
            for (String entry : configured) {
                String root = normalize(entry);
                if (root != null) {
                    roots.add(root);
                }
            }
        }
        return new CommandAliases(roots);
    }

    public static CommandAliases of(String... configured) {
        return of(configured == null ? List.of() : List.of(configured));
    }

    public boolean matches(String root) {
        String normalized = normalize(root);
        return normalized != null && roots.contains(normalized);
    }

    public boolean matchesMessage(String message) {
        return matches(rootOf(message));
    }

    public boolean isEmpty() {
        return roots.isEmpty();
    }

    public Set<String> roots() {
        return Set.copyOf(roots);
    }

    public static String rootOf(String message) {
        if (message == null) {
            return null;
        }
        String trimmed = message.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        int space = firstSpace(trimmed);
        return normalize(space < 0 ? trimmed : trimmed.substring(0, space));
    }

    public static String[] argsOf(String message) {
        if (message == null) {
            return new String[0];
        }
        String trimmed = message.trim();
        int space = firstSpace(trimmed);
        if (space < 0) {
            return new String[0];
        }
        String remainder = trimmed.substring(space + 1).trim();
        return remainder.isEmpty() ? new String[0] : remainder.split("\\s+");
    }

    private static int firstSpace(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static String normalize(String entry) {
        if (entry == null) {
            return null;
        }
        String root = entry.trim();
        if (root.startsWith("/")) {
            root = root.substring(1);
        }
        int space = firstSpace(root);
        if (space >= 0) {
            root = root.substring(0, space);
        }
        int colon = root.indexOf(':');
        if (colon >= 0 && colon + 1 < root.length()) {
            root = root.substring(colon + 1);
        }
        return root.isEmpty() ? null : root.toLowerCase(Locale.ROOT);
    }
}
