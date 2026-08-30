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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    /**
     * Upgrading a config must never be fatal: whatever goes wrong, the caller still gets a result
     * and the file on disk is left exactly as it was.
     */
    public Result migrate() {
        File dataFile = new File(dataFolder, fileName);
        try {
            return runMigration(dataFile);
        } catch (Exception e) {
            warn.accept("Could not migrate " + fileName + ", keeping the existing file: " + e);
            return new Result(dataFile, 0, 0, List.of(), List.of(), false);
        }
    }

    private Result runMigration(File dataFile) {
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

        String userText = readUser(dataFile);
        if (userText == null) {
            return new Result(dataFile, 0, bundledVersion, List.of(), List.of(), false);
        }

        YamlDocument user = YamlDocument.parse(userText);
        int userVersion = user.getInt(versionKey, 0);
        if (userVersion == bundledVersion) {
            return new Result(dataFile, userVersion, bundledVersion, List.of(), List.of(), false);
        }

        info.accept("Migrating " + fileName + " from version " + userVersion + " to " + bundledVersion);

        YamlDocument original = YamlDocument.parse(userText);
        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        mergeKeys(template, user, "", added, removed);

        YamlDocument beforeSteps = user.copy();
        for (MigrationStep step : steps.getOrDefault(userVersion, List.of())) {
            step.apply(user, info);
        }
        Set<String> stepChanged = changedPaths(beforeSteps, user);

        user.set(versionKey, bundledVersion);

        List<String> userLines = Arrays.asList(userText.split("\n"));
        Map<String, List<String>> verbatim = new LinkedHashMap<>();
        for (String path : preserved) {
            List<String> body = sectionBody(userLines, path);
            if (body != null) {
                verbatim.put(path, body);
            }
        }

        List<String> output = ConfigLineMerger.merge(
                Arrays.asList(bundledText.split("\n")), user, verbatim);
        String merged = String.join("\n", output) + "\n";

        String problem = verifyNothingLost(original, merged, removed, stepChanged);
        if (problem != null) {
            warn.accept("Migrating " + fileName + " would have changed " + problem
                    + ", so the existing file was kept untouched. Please report this.");
            return new Result(dataFile, userVersion, bundledVersion, List.of(), List.of(), false);
        }

        if (!write(dataFile, merged)) {
            return new Result(dataFile, userVersion, bundledVersion, List.of(), List.of(), false);
        }

        if (!added.isEmpty()) {
            info.accept("  Added " + added.size() + " new key(s) to " + fileName + ".");
        }
        if (!removed.isEmpty()) {
            info.accept("  Removed " + removed.size() + " obsolete key(s) from " + fileName + ".");
        }

        return new Result(dataFile, userVersion, bundledVersion, added, removed, false);
    }

    /**
     * Re-reads what is about to be written and refuses it unless every value the user had is still
     * there, unchanged. New keys are never worth a silently mangled setting.
     */
    private static Set<String> changedPaths(YamlDocument before, YamlDocument after) {
        Set<String> changed = new LinkedHashSet<>(before.leafPaths());
        changed.addAll(after.leafPaths());
        changed.removeIf(path -> Objects.equals(before.get(path), after.get(path)));
        return changed;
    }

    private String verifyNothingLost(YamlDocument original, String merged, List<String> removed,
                                     Set<String> stepChanged) {
        YamlDocument reloaded;
        try {
            reloaded = YamlDocument.parse(merged);
        } catch (Exception e) {
            return "the file into something unreadable (" + e + ")";
        }

        for (String path : original.leafPaths()) {
            if (path.equals(versionKey) || isUnder(path, removed) || isUnder(path, preserved)
                    || isUnder(path, stepChanged)) {
                continue;
            }
            if (!Objects.equals(original.get(path), reloaded.get(path))) {
                return path + " from " + original.get(path) + " to " + reloaded.get(path);
            }
        }

        for (String path : preserved) {
            if (original.contains(path) && !Objects.equals(original.get(path), reloaded.get(path))) {
                return "the preserved section " + path;
            }
        }

        return null;
    }

    private static boolean isUnder(String path, Iterable<String> parents) {
        for (String parent : parents) {
            if (path.equals(parent) || path.startsWith(parent + ".")) {
                return true;
            }
        }
        return false;
    }

    /**
     * The user's own lines for a section, so a preserved block is copied rather than re-serialised.
     */
    static List<String> sectionBody(List<String> lines, String path) {
        String[] parts = path.split("\\.");
        int depth = 0;
        int parentIndent = -1;

        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int indent = indentOf(lines.get(i));
            if (depth > 0 && indent <= parentIndent) {
                return null;
            }
            String key = keyOf(trimmed);
            if (key == null || !key.equals(parts[depth])) {
                continue;
            }
            if (depth < parts.length - 1) {
                parentIndent = indent;
                depth++;
                continue;
            }

            List<String> body = new ArrayList<>();
            for (int j = i + 1; j < lines.size(); j++) {
                String next = lines.get(j);
                String nextTrimmed = next.trim();
                if (nextTrimmed.isEmpty() || nextTrimmed.startsWith("#")) {
                    body.add(next);
                    continue;
                }
                if (indentOf(next) <= indent) {
                    break;
                }
                body.add(next);
            }
            while (!body.isEmpty()) {
                String last = body.get(body.size() - 1).trim();
                if (last.isEmpty() || last.startsWith("#")) {
                    body.remove(body.size() - 1);
                } else {
                    break;
                }
            }
            return body;
        }
        return null;
    }

    private static int indentOf(String line) {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ' ') count++;
            else if (c == '\t') count += 4;
            else break;
        }
        return count;
    }

    private static String keyOf(String trimmed) {
        int colon = trimmed.indexOf(':');
        if (colon <= 0) {
            return null;
        }
        String candidate = trimmed.substring(0, colon).trim();
        return candidate.isEmpty() || candidate.contains(" ") || candidate.contains("\t") ? null : candidate;
    }

    private void mergeKeys(YamlDocument template, YamlDocument user, String path,
                           List<String> added, List<String> removed) {
        for (String key : template.getKeys(path)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            if (isUnder(fullPath, preserved)) {
                continue;
            }
            if (template.isSection(fullPath)) {
                if (!user.isSection(fullPath)) {
                    user.set(fullPath, new LinkedHashMap<String, Object>());
                }
                mergeKeys(template, user, fullPath, added, removed);
            } else if (!user.contains(fullPath)) {
                user.set(fullPath, template.get(fullPath));
                added.add(fullPath);
            }
        }
        for (String key : user.getKeys(path)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            if (isUnder(fullPath, preserved)) {
                continue;
            }
            if (!template.contains(fullPath)) {
                user.set(fullPath, null);
                removed.add(fullPath);
            }
        }
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

    private boolean write(File dataFile, String merged) {
        try {
            Files.writeString(dataFile.toPath(), merged, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            warn.accept("Failed to write " + fileName + ": " + e.getMessage());
            return false;
        }
    }

    private String readUser(File dataFile) {
        try {
            return normalize(Files.readString(dataFile.toPath(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            warn.accept("Failed to read " + fileName + ": " + e.getMessage());
            return null;
        }
    }

    private String readBundled() {
        try (InputStream stream = bundled.get()) {
            if (stream == null) {
                return null;
            }
            return normalize(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            warn.accept("Failed to read bundled " + fileName + ": " + e.getMessage());
            return null;
        }
    }

    private static String normalize(String text) {
        return text.replace("\r\n", "\n").replace("\r", "\n");
    }
}
