package it.pintux.life.duelsaddon.listener;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuelsMenusTest {

    @Test
    void everyRegisteredMenuIsClassified() {
        Set<String> union = new HashSet<>();
        union.addAll(DuelsMenus.HANDLED);
        union.addAll(DuelsMenus.JAVA_ONLY);
        union.addAll(DuelsMenus.CONTEXT_DEPENDENT);
        assertEquals(DuelsMenus.ALL, union,
                "every PhoenixDuels MenuRegistry id must be handled, java-only, or context-dependent");
    }

    @Test
    void classificationsDoNotOverlap() {
        assertTrue(disjoint(DuelsMenus.HANDLED, DuelsMenus.JAVA_ONLY));
        assertTrue(disjoint(DuelsMenus.HANDLED, DuelsMenus.CONTEXT_DEPENDENT));
        assertTrue(disjoint(DuelsMenus.JAVA_ONLY, DuelsMenus.CONTEXT_DEPENDENT));
    }

    @Test
    void countsMatchPhoenixDuels410() {
        assertEquals(32, DuelsMenus.ALL.size());
        assertEquals(24, DuelsMenus.HANDLED.size());
        assertEquals(4, DuelsMenus.JAVA_ONLY.size());
        assertEquals(4, DuelsMenus.CONTEXT_DEPENDENT.size());
    }

    @Test
    void dragAndDropMenusStayOnJava() {
        assertTrue(DuelsMenus.JAVA_ONLY.contains("kit_items_editor"));
        assertTrue(DuelsMenus.JAVA_ONLY.contains("items_betting"));
        assertTrue(DuelsMenus.JAVA_ONLY.contains("player_kit_layout"));
        assertTrue(disjoint(DuelsMenus.HANDLED, Set.of("kit_items_editor", "items_betting", "player_kit_layout")));
    }

    private static boolean disjoint(Set<String> left, Set<String> right) {
        for (String value : left) {
            if (right.contains(value)) {
                return false;
            }
        }
        return true;
    }
}
