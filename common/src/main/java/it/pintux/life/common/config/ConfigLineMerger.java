package it.pintux.life.common.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ConfigLineMerger {

    public interface ValueSource {
        boolean contains(String path);

        boolean isSection(String path);

        Object get(String path);

        default Set<String> getKeys(String path) {
            return Collections.emptySet();
        }
    }

    private ConfigLineMerger() {
    }

    public static List<String> merge(List<String> templateLines, ValueSource values) {
        return merge(templateLines, values, Collections.emptyMap());
    }

    /**
     * @param verbatimSections body lines, copied straight from the user's own file, for paths that
     *                         must not be re-serialised - their comments, block scalars and layout
     *                         are preserved exactly as written.
     */
    public static List<String> merge(List<String> templateLines, ValueSource values,
                                     Map<String, List<String>> verbatimSections) {
        List<String> output = new ArrayList<>();
        mergeLines(templateLines, values, verbatimSections, output, "", 0, -1);
        return output;
    }

    private static int mergeLines(List<String> templateLines, ValueSource values,
                                  Map<String, List<String>> verbatimSections,
                                  List<String> output, String currentPath, int startIdx, int parentIndent) {
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

            List<String> verbatim = verbatimSections.get(fullPath);
            if (verbatim != null) {
                output.add(line);
                output.addAll(verbatim);
                i = skipSection(templateLines, i + 1, indent);
            } else if (values.isSection(fullPath) || (sectionHeader && !values.contains(fullPath))) {
                output.add(line);
                i = mergeLines(templateLines, values, verbatimSections, output, fullPath, i + 1, indent);
            } else if (values.contains(fullPath)) {
                emitEntry(indent, key, values.get(fullPath), output);
                i = skipSection(templateLines, i + 1, indent);
            } else {
                output.add(line);
                i++;
            }
        }
        return i;
    }

    private static int skipSection(List<String> templateLines, int fromIdx, int parentIndent) {
        int i = fromIdx;
        while (i < templateLines.size()) {
            String line = templateLines.get(i);
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                if (nextKeyDedents(templateLines, i, parentIndent)) {
                    return i;
                }
                i++;
                continue;
            }
            if (getIndent(line) <= parentIndent) {
                return i;
            }
            i++;
        }
        return i;
    }

    private static void emitEntry(int indent, String key, Object value, List<String> output) {
        if (needsBlock(value)) {
            output.add(spaces(indent) + key + ":");
            emitBlockList(indent + 2, (List<?>) value, output);
        } else if (value instanceof Map) {
            output.add(spaces(indent) + key + ":");
            emitBlockMap(indent + 2, (Map<?, ?>) value, output);
        } else {
            output.add(spaces(indent) + key + ": " + formatValue(value));
        }
    }

    private static boolean needsBlock(Object value) {
        if (!(value instanceof List)) {
            return false;
        }
        for (Object element : (List<?>) value) {
            if (element instanceof Map || element instanceof List) {
                return true;
            }
        }
        return false;
    }

    private static void emitBlockList(int indent, List<?> list, List<String> output) {
        for (Object element : list) {
            if (element instanceof Map) {
                emitListItemMap(indent, (Map<?, ?>) element, output);
            } else if (element instanceof List) {
                output.add(spaces(indent) + "-");
                emitBlockList(indent + 2, (List<?>) element, output);
            } else {
                output.add(spaces(indent) + "- " + formatValue(element));
            }
        }
    }

    private static void emitListItemMap(int indent, Map<?, ?> item, List<String> output) {
        if (item.isEmpty()) {
            output.add(spaces(indent) + "- {}");
            return;
        }
        boolean first = true;
        for (Map.Entry<?, ?> entry : item.entrySet()) {
            String prefix = first ? spaces(indent) + "- " : spaces(indent + 2);
            String name = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            if (needsBlock(value)) {
                output.add(prefix + name + ":");
                emitBlockList(indent + 4, (List<?>) value, output);
            } else if (value instanceof Map) {
                output.add(prefix + name + ":");
                emitBlockMap(indent + 4, (Map<?, ?>) value, output);
            } else {
                output.add(prefix + name + ": " + formatValue(value));
            }
            first = false;
        }
    }

    private static void emitBlockMap(int indent, Map<?, ?> map, List<String> output) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            emitEntry(indent, String.valueOf(entry.getKey()), entry.getValue(), output);
        }
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
                        .replace("\r", "\\r")
                        .replace("\n", "\\n") + '"';
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
