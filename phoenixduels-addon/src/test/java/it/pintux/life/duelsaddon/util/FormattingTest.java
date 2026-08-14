package it.pintux.life.duelsaddon.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormattingTest {

    @Test
    void ratioHandlesZeroDeaths() {
        assertEquals("0.00", Formatting.ratio(0, 0));
        assertEquals("7.00", Formatting.ratio(7, 0));
        assertEquals("2.50", Formatting.ratio(5, 2));
    }

    @Test
    void percentHandlesZeroMatches() {
        assertEquals("0.0", Formatting.percent(0, 0));
        assertEquals("50.0", Formatting.percent(1, 2));
        assertEquals("100.0", Formatting.percent(4, 4));
    }

    @Test
    void prettifyTurnsIdentifiersIntoLabels() {
        assertEquals("Diamond Sword", Formatting.prettify("DIAMOND_SWORD"));
        assertEquals("Crystal", Formatting.prettify("crystal"));
        assertEquals("Bed Fight", Formatting.prettify("bed-fight"));
        assertEquals("", Formatting.prettify(null));
    }

    @Test
    void stripColorRemovesCodes() {
        assertEquals("Crystal", Formatting.stripColor("&aCrystal"));
        assertEquals("Crystal", Formatting.stripColor("§aCrystal"));
        assertEquals("", Formatting.stripColor(null));
    }
}
