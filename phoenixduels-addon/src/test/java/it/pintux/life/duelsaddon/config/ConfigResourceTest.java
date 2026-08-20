package it.pintux.life.duelsaddon.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the shipped {@code config.yml}.
 *
 * <p>Duplicate-key detection is the point of the strict loader: an earlier revision declared
 * {@code invitations} twice, SnakeYAML silently kept the last block, and both invitation toggles
 * stopped existing at runtime while everything still looked fine.</p>
 */
class ConfigResourceTest {

    private static Map<String, Object> load() {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        try (InputStream in = ConfigResourceTest.class.getClassLoader().getResourceAsStream("config.yml")) {
            assertNotNull(in, "config.yml is missing from the jar resources");
            Object parsed = new Yaml(options).load(in);
            assertTrue(parsed instanceof Map, "config.yml must parse to a mapping");
            @SuppressWarnings("unchecked")
            Map<String, Object> root = (Map<String, Object>) parsed;
            return root;
        } catch (Exception e) {
            throw new AssertionError("config.yml does not parse: " + e.getMessage(), e);
        }
    }

    @Test
    void parsesWithNoDuplicateKeys() {
        assertFalse(load().isEmpty());
    }

    /**
     * Proves the guard above can actually fail, so a green run means something.
     */
    @Test
    void strictLoaderRejectsADuplicateKey() {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        String duplicated = "invitations:\n  party-forms: true\ninvitations:\n  party-title: 'x'\n";
        assertThrows(Exception.class, () -> new Yaml(options).load(duplicated));
    }

    @Test
    void togglesTheCodeReadsArePresent() {
        Map<String, Object> root = load();
        for (String path : List.of(
                "integrated-gui",
                "register-actions",
                "debug",
                "menus.queue",
                "menus.duel",
                "menus.party",
                "menus.settings",
                "menus.stats",
                "menus.spectator",
                "menus.kit",
                "menus.confirmation",
                "general.items-per-page",
                "general.default-rounds",
                "invitations.party-forms",
                "invitations.duel-forms")) {
            assertNotNull(get(root, path), "missing config key: " + path);
        }
    }

    @Test
    void textKeysWithNonObviousUsageArePresent() {
        Map<String, Object> root = load();
        for (String path : List.of(
                "invitations.accept",
                "invitations.decline",
                "invitations.party-content",
                "invitations.duel-content",
                "queue.mode-button",
                "queue.mode-button-described",
                "queue.mode-button-locked",
                "party.member-button",
                "party.member-offline-button",
                "party.member-pending-button",
                "party.ffa-no-modes",
                "kit.list-title",
                "kit.list-content",
                "confirmation.title",
                "confirmation.content",
                "confirmation.accept",
                "messages.duels-unavailable",
                "common.confirm-no")) {
            assertNotNull(get(root, path), "missing config key: " + path);
        }
    }

    @Test
    void documentedPlaceholdersAppearInTheirValues() {
        Map<String, Object> root = load();
        assertTrue(String.valueOf(get(root, "queue.mode-button-described")).contains("%summary%"));
        assertTrue(String.valueOf(get(root, "invitations.party-content")).contains("%leader%"));
        assertTrue(String.valueOf(get(root, "invitations.duel-content")).contains("%rounds%"));
        assertTrue(String.valueOf(get(root, "party.content")).contains("%slots%"));
        assertTrue(String.valueOf(get(root, "stats.content")).contains("%winrate%"));
    }

    private static Object get(Map<String, Object> root, String path) {
        Object current = root;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(part);
        }
        return current;
    }
}
