package it.pintux.life.common.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigLineMergerTest {

    private static final class MapValues implements ConfigLineMerger.ValueSource {
        private final Map<String, Object> values = new LinkedHashMap<>();

        MapValues put(String path, Object value) {
            values.put(path, value);
            return this;
        }

        @Override
        public boolean contains(String path) {
            if (values.containsKey(path)) return true;
            return isSection(path);
        }

        @Override
        public boolean isSection(String path) {
            String prefix = path + ".";
            for (String key : values.keySet()) {
                if (key.startsWith(prefix)) return true;
            }
            return false;
        }

        @Override
        public Object get(String path) {
            return values.get(path);
        }
    }

    private List<String> merge(String template, ConfigLineMerger.ValueSource values) {
        return ConfigLineMerger.merge(Arrays.asList(template.split("\n")), values);
    }

    @Test
    void siblingSectionsWithSharedKeyNamesKeepTheirOwnValues() {
        String template = String.join("\n",
                "modules:",
                "  warps: false",
                "  tpa: false",
                "",
                "providers:",
                "  warps: auto",
                "  tpa: auto");
        MapValues values = new MapValues()
                .put("modules.warps", true)
                .put("modules.tpa", true)
                .put("providers.warps", "auto")
                .put("providers.tpa", "HuskHomes");

        assertEquals(List.of(
                "modules:",
                "  warps: true",
                "  tpa: true",
                "",
                "providers:",
                "  warps: 'auto'",
                "  tpa: 'HuskHomes'"), merge(template, values));
    }

    @Test
    void keysAfterANestedSectionUseUserValues() {
        String template = String.join("\n",
                "modules:",
                "  warps: false",
                "",
                "integrated-gui: true",
                "",
                "ui:",
                "  warp-title: '&bWarps'");
        MapValues values = new MapValues()
                .put("modules.warps", true)
                .put("integrated-gui", false)
                .put("ui.warp-title", "&cMy Warps");

        List<String> output = merge(template, values);
        assertTrue(output.contains("integrated-gui: false"), output.toString());
        assertTrue(output.contains("  warp-title: '&cMy Warps'"), output.toString());
    }

    @Test
    void commentsStayAttachedToTheKeyBelowThem() {
        String template = String.join("\n",
                "providers:",
                "  warps: auto",
                "",
                "# Popup for incoming requests",
                "tpa-request-popup:",
                "  enabled: true");
        MapValues values = new MapValues()
                .put("providers.warps", "CMI")
                .put("tpa-request-popup.enabled", false);

        assertEquals(List.of(
                "providers:",
                "  warps: 'CMI'",
                "",
                "# Popup for incoming requests",
                "tpa-request-popup:",
                "  enabled: false"), merge(template, values));
    }

    @Test
    void deeplyNestedSectionsDedentBackToRoot() {
        String template = String.join("\n",
                "a:",
                "  b:",
                "    c: 1",
                "d: 2");
        MapValues values = new MapValues()
                .put("a.b.c", 9)
                .put("d", 8);

        assertEquals(List.of("a:", "  b:", "    c: 9", "d: 8"), merge(template, values));
    }

    @Test
    void sectionMissingFromUserConfigDoesNotLeakItsPath() {
        String template = String.join("\n",
                "unknown-section:",
                "  warps: keep-me",
                "providers:",
                "  warps: auto");
        MapValues values = new MapValues().put("providers.warps", "Essentials");

        assertEquals(List.of(
                "unknown-section:",
                "  warps: keep-me",
                "providers:",
                "  warps: 'Essentials'"), merge(template, values));
    }

    @Test
    void trailingCommentsSurviveExactlyOnce() {
        String template = String.join("\n",
                "providers:",
                "  warps: auto",
                "",
                "# end of file");
        List<String> output = merge(template, new MapValues().put("providers.warps", "CMI"));

        List<String> comments = new ArrayList<>();
        for (String line : output) {
            if (line.startsWith("#")) comments.add(line);
        }
        assertEquals(List.of("# end of file"), comments);
    }

    @Test
    void colourCodesAreQuotedSoTheyAreNotReadAsYamlAnchors() {
        assertEquals("'&6&lEssentials Menu'", ConfigLineMerger.formatValue("&6&lEssentials Menu"));
        assertEquals("'it''s'", ConfigLineMerger.formatValue("it's"));
    }

    @Test
    void multilineStringsBecomeEscapedDoubleQuoted() {
        assertEquals("\"&7Type: &fCat\\n&7Level: &f3\"",
                ConfigLineMerger.formatValue("&7Type: &fCat\n&7Level: &f3"));
    }

    @Test
    void scalarsAndListsKeepTheirNativeForm() {
        assertEquals("true", ConfigLineMerger.formatValue(true));
        assertEquals("64", ConfigLineMerger.formatValue(64));
        assertEquals("[]", ConfigLineMerger.formatValue(List.of()));
        assertEquals("[1, 8, 64]", ConfigLineMerger.formatValue(List.of(1, 8, 64)));
        assertEquals("null", ConfigLineMerger.formatValue(null));
    }
}
