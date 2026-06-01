package it.pintux.life.bedwarsaddon.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BedwarsActionPayloadsTest {
    @Test
    void roundTripsIdsContainingColonsAndSpaces() {
        String id = "default_blocks:wool tier:2";
        String encoded = BedwarsActionPayloads.encode(id);
        assertEquals(id, BedwarsActionPayloads.decode(encoded));
    }

    @Test
    void encodedFormHasNoColon() {
        String encoded = BedwarsActionPayloads.encode("a:b:c");
        assertFalse(encoded.contains(":"));
    }

    @Test
    void decodeNullOrBlankReturnsEmpty() {
        assertEquals("", BedwarsActionPayloads.decode(null));
        assertEquals("", BedwarsActionPayloads.decode("  "));
    }
}
