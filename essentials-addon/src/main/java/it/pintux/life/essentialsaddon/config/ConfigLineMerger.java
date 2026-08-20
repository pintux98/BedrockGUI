package it.pintux.life.essentialsaddon.config;

import java.util.ArrayList;
import java.util.List;

public final class ConfigLineMerger {

    public interface ValueSource {
        boolean contains(String path);

        boolean isSection(String path);

        Object get(String path);
    }

    private ConfigLineMerger() {
    }

    public static List<String> merge(List<String> templateLines, ValueSource values) {
        List<String> output = new ArrayList<>();
        mergeLines(templateLines, values, output, "", 0, -1);
        return output;
    }

    private static int mergeLines(List<String> templateLines, ValueSource values, List<String> output,
                                  String currentPath, int startIdx, int parentIndent) {
        boolean nested = !currentPath.isEmpty();
        int i = startIdx;
        while (i < templateLines.size()) {
            String line = templateLines.get(i);
            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                if (nested && nextKeyDedents(templateLines, i, parentIndent)) {
                    return i;
                }
                output.add(line);
                i++;
                continue;
            }

            String key = extractKey(trimmed);
            if (key == null) {
                output.add(line);
                i++;
                continue;
            }

            int indent = getIndent(line);
            if (nested && indent <= parentIndent) {
                return i;
            }

            String fullPath = currentPath.isEmpty() ? key : currentPath + "." + key;
            boolean sectionHeader = trimmed.endsWith(":");

            if (values.isSection(fullPath) || (sectionHeader && !values.contains(fullPath))) {
                output.add(line);
                i = mergeLines(templateLines, values, output, fullPath, i + 1, indent);
            } else if (values.contains(fullPath)) {
                output.add(spaces(indent) + key + ": " + formatValue(values.get(fullPath)));
                i++;
            } else {
                output.add(line);
                i++;
            }
        }
        return i;
    }

    private static boolean nextKeyDedents(List<String> templateLines, int fromIdx, int parentIndent) {
        for (int i = fromIdx + 1; i < templateLines.size(); i++) {
            String line = templateLines.get(i);
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            return getIndent(line) <= parentIndent;
        }
        return true;
    }

    private static String extractKey(String line) {
        int colon = line.indexOf(':');
        if (colon <= 0) return null;
        String candidate = line.substring(0, colon).trim();
        if (candidate.isEmpty() || candidate.contains(" ") || candidate.contains("\t")) return null;
        return candidate;
    }

    private static int getIndent(String line) {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == ' ') count++;
            else if (line.charAt(i) == '\t') count += 4;
            else break;
        }
        return count;
    }

    private static String spaces(int n) {
        return " ".repeat(n);
    }

    static String formatValue(Object value) {
        if (value == null) return "null";
        if (value instanceof String) {
            String text = (String) value;
            if (text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
                return '"' + text
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\r", "\r")
                        .replace("\n", "\n") + '"';
            }
            return "'" + text.replace("'", "''") + "'";
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) return "[]";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(formatValue(list.get(i)));
            }
            return "[" + sb + "]";
        }
        return value.toString();
    }
}
