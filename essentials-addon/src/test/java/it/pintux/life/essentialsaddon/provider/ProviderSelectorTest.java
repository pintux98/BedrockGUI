package it.pintux.life.essentialsaddon.provider;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderSelectorTest {

    private final List<String> warnings = new ArrayList<>();

    private Map<String, Supplier<String>> factories(String... names) {
        Map<String, Supplier<String>> factories = new LinkedHashMap<>();
        for (String name : names) {
            factories.put(name, () -> name);
        }
        return factories;
    }

    @Test
    void autoPicksFirstInPriorityOrder() {
        assertEquals("Essentials",
                ProviderSelector.select(factories("Essentials", "CMI", "HuskHomes"), "auto", "home", warnings::add));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void blankAndNullPreferenceBehaveAsAuto() {
        assertEquals("CMI", ProviderSelector.select(factories("CMI", "HuskHomes"), "  ", "home", warnings::add));
        assertEquals("CMI", ProviderSelector.select(factories("CMI", "HuskHomes"), null, "home", warnings::add));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void explicitPreferenceWinsOverPriorityOrder() {
        assertEquals("HuskHomes",
                ProviderSelector.select(factories("Essentials", "CMI", "HuskHomes"), "HuskHomes", "TPA", warnings::add));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void preferenceIsCaseInsensitive() {
        assertEquals("HuskHomes",
                ProviderSelector.select(factories("Essentials", "HuskHomes"), "huskhomes", "TPA", warnings::add));
    }

    @Test
    void essentialsXAliasResolvesToEssentials() {
        assertEquals("Essentials",
                ProviderSelector.select(factories("CMI", "Essentials"), "EssentialsX", "home", warnings::add));
    }

    @Test
    void uninstalledPreferenceFallsBackAndWarns() {
        assertEquals("Essentials",
                ProviderSelector.select(factories("Essentials", "CMI"), "HuskHomes", "home", warnings::add));
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("HuskHomes"));
        assertTrue(warnings.get(0).contains("Essentials, CMI"));
    }

    @Test
    void failingPreferredProviderFallsBackToNextAvailable() {
        Map<String, Supplier<String>> factories = new LinkedHashMap<>();
        factories.put("Essentials", () -> "Essentials");
        factories.put("CMI", () -> {
            throw new NoClassDefFoundError("CMI");
        });
        assertEquals("Essentials",
                ProviderSelector.select(factories, "CMI", "home", warnings::add));
        assertEquals(2, warnings.size());
    }

    @Test
    void preferredProviderIsNotRetriedDuringFallback() {
        List<String> calls = new ArrayList<>();
        Map<String, Supplier<String>> factories = new LinkedHashMap<>();
        factories.put("Essentials", () -> {
            calls.add("Essentials");
            return null;
        });
        factories.put("CMI", () -> {
            calls.add("CMI");
            return "CMI";
        });
        assertEquals("CMI", ProviderSelector.select(factories, "Essentials", "home", warnings::add));
        assertEquals(List.of("Essentials", "CMI"), calls);
    }

    @Test
    void emptyFactoryMapYieldsNull() {
        assertNull(ProviderSelector.select(new LinkedHashMap<String, Supplier<String>>(), "auto", "home", warnings::add));
    }

    @Test
    void allProvidersFailingYieldsNull() {
        Map<String, Supplier<String>> factories = new LinkedHashMap<>();
        factories.put("Essentials", () -> {
            throw new NoClassDefFoundError("Essentials");
        });
        assertNull(ProviderSelector.select(factories, "auto", "home", warnings::add));
        assertEquals(1, warnings.size());
    }
}
