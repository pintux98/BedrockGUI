package it.pintux.life.essentialsaddon.provider;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HuskHomesValuesTest {

    /** HuskHomes 4.x: the name is a getter on the home itself. */
    public static final class GetterHome {
        public String getName() {
            return "base";
        }
    }

    /** Older shape: the name lives on a meta holder reached through a getter. */
    public static final class MetaGetterHome {
        public Object getMeta() {
            return new Meta("mine");
        }

        public static final class Meta {
            private final String name;

            Meta(String name) {
                this.name = name;
            }

            public String getName() {
                return name;
            }
        }
    }

    /** The shape the HuskHomes wiki example reads: a public {@code meta.name} field. */
    public static final class MetaFieldHome {
        public final Meta meta = new Meta();

        public static final class Meta {
            public final String name = "shop";
        }
    }

    public static final class NamelessHome {
    }

    @Test
    void syncListIsAcceptedJustLikeAFuture() throws Exception {
        List<String> homes = List.of("base", "mine");

        assertEquals(homes, HuskHomesValues.await(homes, "getUserHomes", 1));
        assertEquals(homes, HuskHomesValues.await(CompletableFuture.completedFuture(homes), "getUserHomes", 1));
    }

    @Test
    void optionalsAreUnwrappedFromBothShapes() throws Exception {
        assertEquals("base", HuskHomesValues.await(Optional.of("base"), "getHome", 1));
        assertNull(HuskHomesValues.await(Optional.empty(), "getHome", 1));
        assertEquals("base",
                HuskHomesValues.await(CompletableFuture.completedFuture(Optional.of("base")), "getHome", 1));
        assertNull(HuskHomesValues.unwrapOptional(Optional.empty()));
        assertEquals("raw", HuskHomesValues.unwrapOptional("raw"));
    }

    @Test
    void aFutureThatNeverCompletesFailsLoudlyInsteadOfLookingEmpty() {
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> HuskHomesValues.await(new CompletableFuture<>(), "getUserHomes", 1));

        assertTrue(failure.getMessage().contains("getUserHomes"), failure.getMessage());
    }

    @Test
    void listsArraysAndIterablesAllYieldTheirItems() {
        assertEquals(List.of("a", "b"), HuskHomesValues.asList(List.of("a", "b")));
        assertEquals(List.of("a", "b"), HuskHomesValues.asList(new String[]{"a", "b"}));
        assertEquals(List.of("a"), HuskHomesValues.asList(Set.of("a")));
        assertEquals(List.of(), HuskHomesValues.asList(null));
        assertEquals(List.of(), HuskHomesValues.asList("not a collection"));
    }

    @Test
    void everyHomeNameShapeIsRead() {
        assertEquals("base", HuskHomesValues.homeName(new GetterHome()));
        assertEquals("mine", HuskHomesValues.homeName(new MetaGetterHome()));
        assertEquals("shop", HuskHomesValues.homeName(new MetaFieldHome()));
    }

    @Test
    void anUnreadableHomeYieldsNullRatherThanABlankButton() {
        assertNull(HuskHomesValues.homeName(new NamelessHome()));
        assertNull(HuskHomesValues.homeName(null));
    }

    @Test
    void missingMembersAreReportedAsNull() {
        assertNull(HuskHomesValues.call(new GetterHome(), "getNope"));
        assertNull(HuskHomesValues.call(null, "getName"));
        assertNull(HuskHomesValues.field(new MetaFieldHome(), "nope"));
        assertEquals("base", HuskHomesValues.call(new GetterHome(), "getNope", "getName"));
    }
}
