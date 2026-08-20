package it.pintux.life.duelsaddon.listener;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the menu routing table against a PhoenixDuels update renaming a menu id.
 */
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
        assertEquals(25, DuelsMenus.HANDLED.size());
        assertEquals(4, DuelsMenus.JAVA_ONLY.size());
        assertEquals(3, DuelsMenus.CONTEXT_DEPENDENT.size());
    }

    /**
     * The listener and {@link DuelsMenus} would otherwise be free to drift: an id could be declared
     * with no handler, and the coverage test above would still pass.
     */
    @Test
    void everyHandledMenuResolvesToAHandler() {
        MenuInterceptListener.Services none =
                new MenuInterceptListener.Services(null, null, null, null, null, null, null, null);
        for (String id : DuelsMenus.HANDLED) {
            assertNotNull(MenuInterceptListener.handlerFor(id, none), "no handler for declared id: " + id);
        }
    }

    @Test
    void unhandledMenusResolveToNothing() {
        MenuInterceptListener.Services none =
                new MenuInterceptListener.Services(null, null, null, null, null, null, null, null);
        for (String id : DuelsMenus.JAVA_ONLY) {
            assertNull(MenuInterceptListener.handlerFor(id, none), id + " must fall through to Java");
        }
        for (String id : DuelsMenus.CONTEXT_DEPENDENT) {
            assertNull(MenuInterceptListener.handlerFor(id, none), id + " must fall through to Java");
        }
        assertNull(MenuInterceptListener.handlerFor("some_future_menu", none));
    }

    @Test
    void everyHandledMenuHasAConfigGroup() {
        Set<String> groups = Set.of("queue", "duel", "party", "settings", "stats", "spectator", "kit",
                "confirmation");
        for (String id : DuelsMenus.HANDLED) {
            assertTrue(groups.contains(DuelsMenus.GROUPS.get(id)),
                    id + " maps to unknown config group " + DuelsMenus.GROUPS.get(id));
        }
    }

    @Test
    void dragAndDropMenusStayOnJava() {
        assertTrue(DuelsMenus.JAVA_ONLY.contains("kit_items_editor"));
        assertTrue(DuelsMenus.JAVA_ONLY.contains("items_betting"));
        assertTrue(DuelsMenus.JAVA_ONLY.contains("player_kit_layout"));
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
