package it.pintux.life.duelsaddon.util;

import it.pintux.life.duelsaddon.model.TeamSize;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Payloads are hand-written in menu YAML, so these tests use literal strings rather than builders:
 * the input under test is what an admin actually types.
 */
class DuelsActionPayloadsTest {

    @Test
    void queuePayloadIsParsedIntoLadderSizeAndMode() {
        String payload = "ranked|duo|crystal";
        assertTrue(DuelsActionPayloads.ranked(payload));
        assertEquals("DUO", DuelsActionPayloads.size(payload));
        assertEquals(TeamSize.DUO, TeamSize.parse(DuelsActionPayloads.size(payload)));
        assertEquals("crystal", DuelsActionPayloads.queueMode(payload));
    }

    @Test
    void anythingOtherThanRankedMeansUnranked() {
        assertFalse(DuelsActionPayloads.ranked("unranked|solo|classic"));
        assertFalse(DuelsActionPayloads.ranked("typo|solo|classic"));
        assertFalse(DuelsActionPayloads.ranked(""));
        assertFalse(DuelsActionPayloads.ranked(null));
    }

    @Test
    void ladderAndSizeAreCaseInsensitive() {
        assertTrue(DuelsActionPayloads.ranked("RANKED|QUAD|uhc"));
        assertEquals(TeamSize.QUAD, TeamSize.parse(DuelsActionPayloads.size("RANKED|quad|uhc")));
    }

    @Test
    void pageFallsBackToDefault() {
        assertEquals(1, DuelsActionPayloads.page("crystal", 1));
        assertEquals(3, DuelsActionPayloads.page("crystal|3", 1));
        assertEquals(1, DuelsActionPayloads.page("crystal|notanumber", 1));
        assertEquals(2, DuelsActionPayloads.page(null, 2));
    }

    @Test
    void uuidIsParsedFromFirstSegment() {
        UUID id = UUID.randomUUID();
        assertEquals(id, DuelsActionPayloads.uuid(id.toString()));
        assertEquals(id, DuelsActionPayloads.uuid(id + "|extra"));
    }

    @Test
    void uuidRejectsGarbage() {
        assertThrows(IllegalArgumentException.class, () -> DuelsActionPayloads.uuid("not-a-uuid"));
        assertThrows(IllegalArgumentException.class, () -> DuelsActionPayloads.uuid(""));
        assertThrows(IllegalArgumentException.class, () -> DuelsActionPayloads.uuid(null));
    }

    @Test
    void incompleteQueuePayloadsReportWhatIsMissing() {
        assertThrows(IllegalArgumentException.class, () -> DuelsActionPayloads.size("ranked"));
        assertThrows(IllegalArgumentException.class, () -> DuelsActionPayloads.queueMode("ranked|duo"));
    }

    @Test
    void playerNameIsTrimmedAndNullSafe() {
        assertEquals("Notch", DuelsActionPayloads.playerName("  Notch "));
        assertEquals("", DuelsActionPayloads.playerName(null));
    }

    @Test
    void firstSegmentIsTrimmed() {
        assertEquals("crystal", DuelsActionPayloads.first(" crystal |2"));
        assertEquals("", DuelsActionPayloads.first(null));
        assertEquals("", DuelsActionPayloads.first("   "));
    }

    @Test
    void teamSizeParseIsLenient() {
        assertEquals(TeamSize.SOLO, TeamSize.parse(null));
        assertEquals(TeamSize.SOLO, TeamSize.parse("nonsense"));
        assertEquals(TeamSize.QUAD, TeamSize.parse(" quad "));
    }
}
