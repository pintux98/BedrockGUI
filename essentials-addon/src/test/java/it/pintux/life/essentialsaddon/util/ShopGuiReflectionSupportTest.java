package it.pintux.life.essentialsaddon.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopGuiReflectionSupportTest {

    @Test
    void normalPotion() {
        assertEquals("Potion of Healing",
                ShopGuiReflectionSupport.composePotionName("POTION", "HEALING", false, false));
    }

    @Test
    void splashUpgraded() {
        assertEquals("Splash Potion of Speed II",
                ShopGuiReflectionSupport.composePotionName("SPLASH_POTION", "SPEED", true, false));
    }

    @Test
    void lingeringExtended() {
        assertEquals("Lingering Potion of Regen (Extended)",
                ShopGuiReflectionSupport.composePotionName("LINGERING_POTION", "REGEN", false, true));
    }

    @Test
    void tippedArrow() {
        assertEquals("Tipped Arrow of Poison",
                ShopGuiReflectionSupport.composePotionName("TIPPED_ARROW", "POISON", false, false));
    }

    @Test
    void multiWordEffect() {
        assertEquals("Potion of Instant Heal",
                ShopGuiReflectionSupport.composePotionName("POTION", "INSTANT_HEAL", false, false));
    }

    @Test
    void waterHasNoEffectSuffix() {
        assertEquals("Potion", ShopGuiReflectionSupport.composePotionName("POTION", "WATER", false, false));
    }

    @Test
    void awkwardHasNoEffectSuffix() {
        assertEquals("Potion", ShopGuiReflectionSupport.composePotionName("POTION", "AWKWARD", false, false));
    }

    @Test
    void unknownMaterialFallsBackToPrettifiedPrefix() {
        assertEquals("Splash Potion of Luck",
                ShopGuiReflectionSupport.composePotionName("SPLASH_POTION", "LUCK", false, false));
    }
}
