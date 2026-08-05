package it.pintux.life.essentialsaddon.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopGuiAccessReflectionTest {

    static final class ModernShop {
        Object seenPlayer;
        Object seenItem;
        Boolean seenFlag;

        public boolean hasAccess(Object player, Object shopItem, boolean sendMessage) {
            this.seenPlayer = player;
            this.seenItem = shopItem;
            this.seenFlag = sendMessage;
            return true;
        }
    }

    static final class TwoArgShop {
        Object seenItem;

        public boolean hasAccess(Object player, Object shopItem) {
            this.seenItem = shopItem;
            return false;
        }
    }

    /** ShopGUI+ 1.113.0 shape: item gate is hasAccessToItem, hasAccess(Player) is the shop-level gate. */
    static final class RenamedItemGateShop {
        Object seenItem;
        Boolean seenFlag;

        public boolean hasAccessToItem(Object player, Object shopItem, boolean sendMessage) {
            this.seenItem = shopItem;
            this.seenFlag = sendMessage;
            return true;
        }

        public boolean hasAccess(Object player) {
            return false;
        }
    }

    /** Only a shop-level gate: must not be mistaken for an item check. */
    static final class ShopLevelOnlyShop {
        public boolean hasAccess(Object player) {
            return false;
        }
    }

    static final class LegacyShop {
        public String getId() {
            return "farm";
        }
    }

    static final class ThrowingShop {
        public boolean hasAccess(Object player, Object shopItem, boolean sendMessage) {
            throw new IllegalStateException("boom");
        }
    }

    static final class FlagShop {
        public boolean isDenyDirectAccess() {
            return true;
        }
    }

    @Test
    void prefersThreeArgOverloadAndPassesSilentFlag() {
        ModernShop shop = new ModernShop();
        Object player = new Object();
        Object item = new Object();

        assertEquals(Boolean.TRUE, ShopGuiReflectionSupport.invokeAccessCheck(shop, player, item));
        assertEquals(player, shop.seenPlayer);
        assertEquals(item, shop.seenItem);
        assertEquals(Boolean.FALSE, shop.seenFlag);
    }

    @Test
    void fallsBackToTwoArgOverload() {
        TwoArgShop shop = new TwoArgShop();
        Object item = new Object();

        assertEquals(Boolean.FALSE, ShopGuiReflectionSupport.invokeAccessCheck(shop, new Object(), item));
        assertEquals(item, shop.seenItem);
    }

    @Test
    void prefersRenamedItemGateOverShopLevelGate() {
        RenamedItemGateShop shop = new RenamedItemGateShop();
        Object item = new Object();

        assertEquals(Boolean.TRUE, ShopGuiReflectionSupport.invokeAccessCheck(shop, new Object(), item));
        assertEquals(item, shop.seenItem);
        assertEquals(Boolean.FALSE, shop.seenFlag);
    }

    @Test
    void ignoresShopLevelGateForItemChecks() {
        assertNull(ShopGuiReflectionSupport.invokeAccessCheck(new ShopLevelOnlyShop(), new Object(), new Object()));
    }

    @Test
    void returnsNullWhenNoOverloadExists() {
        assertNull(ShopGuiReflectionSupport.invokeAccessCheck(new LegacyShop(), new Object(), new Object()));
    }

    @Test
    void returnsNullWhenInvocationFails() {
        assertNull(ShopGuiReflectionSupport.invokeAccessCheck(new ThrowingShop(), new Object(), new Object()));
    }

    @Test
    void returnsNullForNullShop() {
        assertNull(ShopGuiReflectionSupport.invokeAccessCheck(null, new Object(), new Object()));
    }

    @Test
    void resolvesPerClassWithoutCrossContamination() {
        assertEquals(Boolean.TRUE,
                ShopGuiReflectionSupport.invokeAccessCheck(new ModernShop(), new Object(), new Object()));
        assertEquals(Boolean.FALSE,
                ShopGuiReflectionSupport.invokeAccessCheck(new TwoArgShop(), new Object(), new Object()));
        assertNull(ShopGuiReflectionSupport.invokeAccessCheck(new LegacyShop(), new Object(), new Object()));
    }

    @Test
    void booleanFlagReadsExistingGetter() {
        assertTrue(ShopGuiReflectionSupport.booleanFlag(new FlagShop(), false, "isDenyDirectAccess"));
    }

    @Test
    void booleanFlagUsesFallbackWhenGetterMissing() {
        assertTrue(ShopGuiReflectionSupport.booleanFlag(new LegacyShop(), true, "isDenyDirectAccess"));
        assertEquals(false, ShopGuiReflectionSupport.booleanFlag(new LegacyShop(), false, "isDenyDirectAccess"));
    }
}
