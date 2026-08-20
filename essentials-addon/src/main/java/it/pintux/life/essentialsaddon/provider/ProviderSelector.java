package it.pintux.life.essentialsaddon.provider;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ProviderSelector {

    public static final String AUTO = "auto";

    private static final Map<String, String> ALIASES = Map.of(
            "essentialsx", "Essentials",
            "husk", "HuskHomes",
            "husk-homes", "HuskHomes",
            "husk_homes", "HuskHomes"
    );

    private ProviderSelector() {
    }

    public static <T> T select(Map<String, Supplier<T>> factories, String preference,
                              String feature, Consumer<String> warn) {
        if (factories == null || factories.isEmpty()) {
            return null;
        }

        Map<String, Supplier<T>> remaining = new LinkedHashMap<>(factories);
        String requested = normalize(preference);

        if (requested != null) {
            String key = resolveKey(factories, requested);
            if (key == null) {
                warn.accept("Configured " + feature + " provider '" + preference.trim()
                        + "' is not available. Installed: " + String.join(", ", factories.keySet())
                        + ". Falling back to auto-detection.");
            } else {
                T provider = instantiate(remaining.remove(key), key, feature, warn);
                if (provider != null) {
                    return provider;
                }
                warn.accept("Configured " + feature + " provider '" + key
                        + "' failed to initialize. Falling back to auto-detection.");
            }
        }

        for (Map.Entry<String, Supplier<T>> entry : remaining.entrySet()) {
            T provider = instantiate(entry.getValue(), entry.getKey(), feature, warn);
            if (provider != null) {
                return provider;
            }
        }
        return null;
    }

    private static <T> T instantiate(Supplier<T> factory, String name, String feature, Consumer<String> warn) {
        if (factory == null) {
            return null;
        }
        try {
            return factory.get();
        } catch (Throwable throwable) {
            warn.accept("Failed to initialize the " + name + " " + feature + " provider: "
                    + throwable.getClass().getSimpleName());
            return null;
        }
    }

    private static <T> String resolveKey(Map<String, Supplier<T>> factories, String requested) {
        for (String key : factories.keySet()) {
            if (key.equalsIgnoreCase(requested)) {
                return key;
            }
        }
        return null;
    }

    private static String normalize(String preference) {
        if (preference == null) {
            return null;
        }
        String trimmed = preference.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase(AUTO) || trimmed.equalsIgnoreCase("default")) {
            return null;
        }
        String alias = ALIASES.get(trimmed.toLowerCase(Locale.ROOT));
        return alias != null ? alias : trimmed;
    }
}
