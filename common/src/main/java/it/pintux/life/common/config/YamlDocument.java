package it.pintux.life.common.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class YamlDocument implements ConfigLineMerger.ValueSource {

    private final Map<String, Object> root;

    private YamlDocument(Map<String, Object> root) {
        this.root = root == null ? new LinkedHashMap<>() : root;
    }

    public static YamlDocument empty() {
        return new YamlDocument(new LinkedHashMap<>());
    }

    public static YamlDocument parse(String text) {
        return text == null ? empty() : read(new StringReader(text));
    }

    public static YamlDocument load(Path file) throws IOException {
        if (!Files.exists(file)) {
            return empty();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return read(reader);
        }
    }

    private static YamlDocument read(Reader reader) {
        return new YamlDocument(asSection(new Yaml().load(reader)));
    }

    @Override
    public boolean contains(String path) {
        return resolve(path) != null;
    }

    @Override
    public boolean isSection(String path) {
        return asSection(resolve(path)) != null;
    }

    @Override
    public Object get(String path) {
        return resolve(path);
    }

    public int getInt(String path, int fallback) {
        Object value = resolve(path);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    public Set<String> getKeys(String path) {
        Map<String, Object> section = path == null || path.isEmpty() ? root : asSection(resolve(path));
        return section == null ? Collections.emptySet() : new LinkedHashSet<>(section.keySet());
    }

    public void set(String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Map<String, Object> section = asSection(current.get(parts[i]));
            if (section == null) {
                if (value == null) {
                    return;
                }
                section = new LinkedHashMap<>();
                current.put(parts[i], section);
            }
            current = section;
        }
        if (value == null) {
            current.remove(parts[parts.length - 1]);
        } else {
            current.put(parts[parts.length - 1], value);
        }
    }

    public Map<String, Object> asMap() {
        return root;
    }

    private Object resolve(String path) {
        if (path == null || path.isEmpty()) {
            return root;
        }
        Object current = root;
        for (String part : path.split("\\.")) {
            Map<String, Object> section = asSection(current);
            if (section == null) {
                return null;
            }
            current = section.get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asSection(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }
}
