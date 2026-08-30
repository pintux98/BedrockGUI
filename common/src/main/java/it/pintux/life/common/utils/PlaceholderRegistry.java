package it.pintux.life.common.utils;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlaceholderRegistry {

    private static final Pattern PLACEHOLDER = Pattern.compile("%([a-zA-Z0-9]+)(?:_([^%\\s]*))?%");
    private static final PlaceholderRegistry SHARED = new PlaceholderRegistry();

    private final Map<String, PlaceholderResolver> resolvers = new ConcurrentHashMap<>();

    public static PlaceholderRegistry shared() {
        return SHARED;
    }

    public boolean register(String identifier, PlaceholderResolver resolver) {
        if (identifier == null || identifier.isBlank() || resolver == null) {
            return false;
        }
        return resolvers.putIfAbsent(normalize(identifier), resolver) == null;
    }

    public void replace(String identifier, PlaceholderResolver resolver) {
        if (identifier == null || identifier.isBlank() || resolver == null) {
            return;
        }
        resolvers.put(normalize(identifier), resolver);
    }

    public boolean unregister(String identifier) {
        return identifier != null && resolvers.remove(normalize(identifier)) != null;
    }

    public boolean isRegistered(String identifier) {
        return identifier != null && resolvers.containsKey(normalize(identifier));
    }

    public Set<String> getIdentifiers() {
        return Collections.unmodifiableSet(new TreeSet<>(resolvers.keySet()));
    }

    public void clear() {
        resolvers.clear();
    }

    public String resolve(FormPlayer player, String identifier, String params) {
        if (identifier == null) {
            return null;
        }
        PlaceholderResolver resolver = resolvers.get(normalize(identifier));
        if (resolver == null) {
            return null;
        }
        try {
            return resolver.resolve(player, params == null ? "" : params);
        } catch (Exception e) {
            return null;
        }
    }

    public String apply(FormPlayer player, String text) {
        if (text == null || text.indexOf('%') < 0 || resolvers.isEmpty()) {
            return text;
        }

        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuilder out = new StringBuilder(text.length());
        int last = 0;
        boolean replaced = false;

        while (matcher.find()) {
            PlaceholderResolver resolver = resolvers.get(normalize(matcher.group(1)));
            if (resolver == null) {
                continue;
            }
            String params = matcher.group(2) == null ? "" : matcher.group(2);
            String value;
            try {
                value = resolver.resolve(player, params);
            } catch (Exception e) {
                value = null;
            }
            if (value == null) {
                continue;
            }
            out.append(text, last, matcher.start()).append(value);
            last = matcher.end();
            replaced = true;
        }

        return replaced ? out.append(text.substring(last)).toString() : text;
    }

    private static String normalize(String identifier) {
        return identifier.toLowerCase(Locale.ROOT);
    }
}
