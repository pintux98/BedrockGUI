package it.pintux.life.duelsaddon.util;

import it.pintux.life.duelsaddon.model.TeamSize;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuelsActionPayloadsTest {

    @Test
    void queuePayloadRoundTrips() {
        String payload = DuelsActionPayloads.queue(true, "DUO", "crystal");
        assertTrue(DuelsActionPayloads.ranked(payload));
        assertEquals("DUO", DuelsActionPayloads.size(payload));
        assertEquals(TeamSize.DUO, TeamSize.parse(DuelsActionPayloads.size(payload)));
        assertEquals("crystal", DuelsActionPayloads.queueMode(payload));
    }

    @Test
    void unrankedQueuePayloadIsNotRanked() {
        String payload = DuelsActionPayloads.queue(false, "solo", "classic");
        assertFalse(DuelsActionPayloads.ranked(payload));
        assertEquals(TeamSize.SOLO, TeamSize.parse(DuelsActionPayloads.size(payload)));
    }

    @Test
    void modePayloadRejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> DuelsActionPayloads.modeId(""));
        assertThrows(IllegalArgumentException.class, () -> DuelsActionPayloads.modeId(null));
        assertEquals("uhc", DuelsActionPayloads.modeId("uhc"));
    }

    @Test
    void pageFallsBackToDefault() {
        assertEquals(1, DuelsActionPayloads.page("crystal", 1));
        assertEquals(3, DuelsActionPayloads.page(DuelsActionPayloads.modePage("crystal", 3), 1));
        assertEquals(1, DuelsActionPayloads.page("crystal|notanumber", 1));
    }

    @Test
    void uuidPayloadRoundTrips() {
        UUID id = UUID.randomUUID();
        assertEquals(id, DuelsActionPayloads.uuid(DuelsActionPayloads.player(id)));
    }

    @Test
    void uuidPayloadRejectsGarbage() {
        assertThrows(IllegalArgumentException.class, () -> DuelsActionPayloads.uuid("not-a-uuid"));
        assertThrows(IllegalArgumentException.class, () -> DuelsActionPayloads.uuid(""));
    }

    @Test
    void roundsClampToAtLeastOne() {
        assertEquals(5, DuelsActionPayloads.rounds("crystal|5", 1));
        assertEquals(1, DuelsActionPayloads.rounds("crystal|0", 3));
        assertEquals(3, DuelsActionPayloads.rounds("crystal", 3));
    }

    @Test
    void sizeRequiresSecondSegment() {
        assertThrows(IllegalArgumentException.class, () -> DuelsActionPayloads.size("ranked"));
    }

    @Test
    void teamSizeParseIsLenient() {
        assertEquals(TeamSize.SOLO, TeamSize.parse(null));
        assertEquals(TeamSize.SOLO, TeamSize.parse("nonsense"));
        assertEquals(TeamSize.QUAD, TeamSize.parse(" quad "));
    }
}
