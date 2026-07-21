package it.pintux.life.homesteadaddon.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HomesteadActionPayloadsTest {

    @Test
    void regionRoundTrip() {
        String p = HomesteadActionPayloads.region(1234567890123L);
        assertEquals(1234567890123L, HomesteadActionPayloads.regionId(p));
        assertEquals(7, HomesteadActionPayloads.page(p, 7));
    }

    @Test
    void regionPageRoundTrip() {
        String p = HomesteadActionPayloads.regionPage(42L, 3);
        assertEquals(42L, HomesteadActionPayloads.regionId(p));
        assertEquals(3, HomesteadActionPayloads.page(p, 1));
    }

    @Test
    void regionMemberRoundTrip() {
        UUID member = UUID.randomUUID();
        String p = HomesteadActionPayloads.regionMember(99L, member);
        assertEquals(99L, HomesteadActionPayloads.regionId(p));
        assertEquals(member, HomesteadActionPayloads.member(p));
    }

    @Test
    void regionSubRoundTrip() {
        String p = HomesteadActionPayloads.regionSub(5L, 77L);
        assertEquals(5L, HomesteadActionPayloads.regionId(p));
        assertEquals(77L, HomesteadActionPayloads.subAreaId(p));
    }

    @Test
    void regionSubMemberRoundTrip() {
        UUID member = UUID.randomUUID();
        String p = HomesteadActionPayloads.regionSubMember(5L, 77L, member);
        assertEquals(5L, HomesteadActionPayloads.regionId(p));
        assertEquals(77L, HomesteadActionPayloads.subAreaId(p));
        assertEquals(member, HomesteadActionPayloads.subAreaMember(p));
    }

    @Test
    void partsSplitsOnPipe() {
        assertArrayEquals(new String[]{"1", "2", "3"}, HomesteadActionPayloads.parts("1|2|3"));
        assertArrayEquals(new String[0], HomesteadActionPayloads.parts(""));
        assertArrayEquals(new String[0], HomesteadActionPayloads.parts(null));
    }

    @Test
    void invalidPayloadsThrow() {
        assertThrows(IllegalArgumentException.class, () -> HomesteadActionPayloads.regionId(""));
        assertThrows(IllegalArgumentException.class, () -> HomesteadActionPayloads.regionId("abc"));
        assertThrows(IllegalArgumentException.class, () -> HomesteadActionPayloads.member("42"));
        assertThrows(IllegalArgumentException.class, () -> HomesteadActionPayloads.subAreaId("42"));
    }
}
