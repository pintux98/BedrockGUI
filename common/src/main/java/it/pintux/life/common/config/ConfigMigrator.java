package it.pintux.life.common.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ConfigMigrator {

    public static final String DEFAULT_VERSION_KEY = "config-version";

    @FunctionalInterface
    public interface MigrationStep {
        void apply(YamlDocument document, Consumer<String> log);
    }

    public static final class Result {
        private final File file;
        private final int fromVersion;
        private final int toVersion;
        private final List<String> addedKeys;
        private final List<String> removedKeys;
        private final boolean created;

        private Result(File file, int fromVersion, int toVersion,
                       List<String> addedKeys, List<String> removedKeys, boolean created) {
            this.file = file;
            this.fromVersion = fromVersion;
            this.toVersion = toVersion;
            this.addedKeys = addedKeys;
            this.removedKeys = removedKeys;
            this.created = created;
        }

        public File getFile() {
            return file;
        }

        public int getFromVersion() {
            return fromVersion;
        }

        public int getToVersion() {
            return toVersion;
        }

        public List<String> getAddedKeys() {
            return addedKeys;
        }

        public List<String> getRemovedKeys() {
            return removedKeys;
        }

        public boolean wasCreated() {
            return created;
        }

        public boolean wasMigrated() {
            return fromVersion != toVersion && !created;
        }
    }

    private final File dataFolder;
    private final String fileName;
    private final Supplier<InputStream> bundled;
    private final Consumer<String> info;
    private final Consumer<String> warn;
    private final Map<Integer, List<MigrationStep>> steps = new HashMap<>();
    private final Set<String> preserved = new LinkedHashSet<>();
    private String versionKey = DEFAULT_VERSION_KEY;

    private ConfigMigrator(File dataFolder, String fileName, Supplier<InputStream> bundled,
                           Consumer<String> info, Consumer<String> warn) {
        this.dataFolder = dataFolder;
        this.fileName = fileName;
        this.bundled = bundled;
        this.info = info == null ? message -> {
        } : info;
        this.warn = warn == null ? this.info : warn;
    }

    public static ConfigMigrator of(File dataFolder, String fileName, Supplier<InputStream> bundled,
                                    Consumer<String> info, Consumer<String> warn) {
        return new ConfigMigrator(dataFolder, fileName, bundled, info, warn);
    }

    public static ConfigMigrator ofClasspath(File dataFolder, String fileName, ClassLoader loader,
                                             Consumer<String> info, Consumer<String> warn) {
        return new ConfigMigrator(dataFolder, fileName, () -> loader.getResourceAsStream(fileName), info, warn);
    }

    public ConfigMigrator versionKey(String key) {
        if (key != null && !key.isBlank()) {
            this.versionKey = key;
        }
        return this;
    }

    public ConfigMigrator step(int fromVersion, MigrationStep step) {
        if (step != null) {
            steps.computeIfAbsent(fromVersion, key -> new ArrayList<>()).add(step);
        }
        return this;
    }

    public ConfigMigrator preserve(String... paths) {
        if (paths != null) {
            for (String path : paths) {
                if (path != null && !path.isBlank()) {
                    preserved.add(path);
                }
            }
        }
        return this;
    }

    public Result migrate() {
        File dataFile = new File(dataFolder, fileName);

        String bundledText = readBundled();
        if (bundledText == null) {
            warn.accept("Bundled " + fileName + " not found in resources. Skipping migration.");
            return new Result(dataFile, 0, 0, List.of(), List.of(), false);
        }

        YamlDocument template = YamlDocument.parse(bundledText);
        int bundledVersion = template.getInt(versionKey, 1);

        if (!dataFile.exists()) {
            writeDefault(dataFile, bundledText);
            return new Result(dataFile, bundledVersion, bundledVersion, List.of(), List.of(), true);
        }

        YamlDocument user;
        try {
            user = YamlDocument.load(dataFile.toPath());
        } catch (IOException e) {
            warn.accept("Failed to read " + fileName + ": " + e.getMessage());
            return new Result(dataFile, 0, bundledVersion, List.of(), List.of(), false);
        }

        int userVersion = user.getInt(versionKey, 0);
        if (userVersion == bundledVersion) {
            return new Result(dataFile, userVersion, bundledVersion, List.of(), List.of(), false);
        }

        info.accept("Migrating " + fileName + " from version " + userVersion + " to " + bundledVersion);

        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        mergeKeys(template, user, "", added, removed);

        for (MigrationStep step : steps.getOrDefault(userVersion, List.of())) {
            step.apply(user, info);
        }

        user.set(versionKey, bundledVersion);
        write(dataFile, bundledText, user);

        if (!added.isEmpty()) {
            info.accept("  Added " + added.size() + " new key(s) to " + fileName + ".");
        }
        if (!removed.isEmpty()) {
            info.accept("  Removed " + removed.size() + " obsolete key(s) from " + fileName + ".");
        }

        return new Result(dataFile, userVersion, bundledVersion, added, removed, false);
    }

    private void mergeKeys(YamlDocument template, YamlDocument user, String path,
                           List<String> added, List<String> removed) {
        for (String key : template.getKeys(path)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            if (isPreserved(fullPath)) {
                continue;
            }
            if (template.isSection(fullPath)) {
                if (!user.isSection(fullPath)) {
                    user.set(fullPath, new java.util.LinkedHashMap<String, Object>());
                }
                mergeKeys(template, user, fullPath, added, removed);
            } else if (!user.contains(fullPath)) {
                user.set(fullPath, template.get(fullPath));
                added.add(fullPath);
            }
        }
        for (String key : user.getKeys(path)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            if (isPreserved(fullPath)) {
                continue;
            }
            if (!template.contains(fullPath)) {
                user.set(fullPath, null);
                removed.add(fullPath);
            }
        }
    }

    private boolean isPreserved(String path) {
        for (String preservedPath : preserved) {
            if (path.equals(preservedPath) || path.startsWith(preservedPath + ".")) {
                return true;
            }
        }
        return false;
    }

    private void writeDefault(File dataFile, String bundledText) {
        try {
            Path parent = dataFile.toPath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(dataFile.toPath(), bundledText, StandardCharsets.UTF_8);
        } catch (IOException e) {
            warn.accept("Failed to write default " + fileName + ": " + e.getMessage());
        }
    }

    private void write(File dataFile, String bundledText, YamlDocument user) {
        try {
            List<String> templateLines = Arrays.asList(bundledText.split("\n"));
            List<String> output = ConfigLineMerger.merge(templateLines, user, preserved);
            Files.writeString(dataFile.toPath(), String.join("\n", output) + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            warn.accept("Failed to write " + fileName + ": " + e.getMessage());
        }
    }

    private String readBundled() {
        try (InputStream stream = bundled.get()) {
            if (stream == null) {
                return null;
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("\r\n", "\n")
                    .replace("\r", "\n");
        } catch (IOException e) {
            warn.accept("Failed to read bundled " + fileName + ": " + e.getMessage());
            return null;
        }
    }
}
