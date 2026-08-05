package it.pintux.life.essentialsaddon.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopGuiNamesTest {

    @Test
    void substitutesPagePlaceholder() {
        assertEquals("&9&lBlocks (page 3)", ShopGuiNames.resolvePageName("&9&lBlocks (page %page%)", 3));
    }

    @Test
    void substitutesEveryOccurrence() {
        assertEquals("Ores 2 of 2", ShopGuiNames.resolvePageName("Ores %page% of %page%", 2));
    }

    @Test
    void leavesNamesWithoutPlaceholderUntouched() {
        assertEquals("&3&lOres", ShopGuiNames.resolvePageName("&3&lOres", 1));
    }

    @Test
    void nullNameBecomesEmpty() {
        assertEquals("", ShopGuiNames.resolvePageName(null, 1));
    }
}
