package it.pintux.life.common.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class DefaultFormsExtractor {

    private static final String FORMS_DIR = "forms";
    private static final String YML = ".yml";

    private DefaultFormsExtractor() {
    }

    public static int extract(File dataFolder, Consumer<String> warn) {
        List<String> bundled = listBundledForms(warn);
        if (bundled.isEmpty()) {
            return 0;
        }

        File formsDir = new File(dataFolder, FORMS_DIR);
        if (!formsDir.isDirectory() && !formsDir.mkdirs()) {
            warn.accept("Could not create forms directory: " + formsDir.getAbsolutePath());
            return 0;
        }

        int written = 0;
        for (String name : bundled) {
            File target = new File(formsDir, name);
            if (target.exists()) {
                continue;
            }
            try (InputStream in = DefaultFormsExtractor.class.getResourceAsStream("/" + FORMS_DIR + "/" + name)) {
                if (in == null) {
                    warn.accept("Bundled form not readable: " + name);
                    continue;
                }
                Files.copy(in, target.toPath());
                written++;
            } catch (IOException e) {
                warn.accept("Failed to extract default form " + name + ": " + e.getMessage());
            }
        }
        return written;
    }

    private static List<String> listBundledForms(Consumer<String> warn) {
        URL location;
        try {
            location = DefaultFormsExtractor.class.getProtectionDomain().getCodeSource().getLocation();
        } catch (Exception e) {
            warn.accept("Could not locate plugin jar to read default forms: " + e.getMessage());
            return Collections.emptyList();
        }
        if (location == null) {
            return Collections.emptyList();
        }

        File asFile = toFile(location);
        if (asFile != null && asFile.isDirectory()) {
            return listFromDirectory(new File(asFile, FORMS_DIR));
        }

        List<String> names = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(location.openStream())) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String simple = simpleFormName(entry.getName());
                if (simple != null) {
                    names.add(simple);
                }
            }
        } catch (Exception e) {
            warn.accept("Could not list bundled default forms: " + e.getMessage());
            return Collections.emptyList();
        }
        return names;
    }

    private static List<String> listFromDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(YML)) {
                names.add(file.getName());
            }
        }
        return names;
    }

    private static String simpleFormName(String entryName) {
        String prefix = FORMS_DIR + "/";
        if (!entryName.startsWith(prefix) || !entryName.endsWith(YML)) {
            return null;
        }
        String simple = entryName.substring(prefix.length());
        if (simple.isEmpty() || simple.contains("/")) {
            return null;
        }
        return simple;
    }

    private static File toFile(URL url) {
        try {
            if (!"file".equals(url.getProtocol())) {
                return null;
            }
            return new File(url.toURI());
        } catch (Exception e) {
            return null;
        }
    }
}
