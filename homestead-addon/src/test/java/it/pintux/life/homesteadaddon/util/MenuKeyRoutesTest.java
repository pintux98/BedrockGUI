package it.pintux.life.homesteadaddon.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuKeyRoutesTest {

    // MENU_KEY constants declared by Homestead dev-6.0.0.0 under gui/menus.
    private static final List<String> UPSTREAM_KEYS = List.of(
            "map_color",
            "map_icon",
            "miscellaneous_settings",
            "player_info",
            "region_banned_players",
            "region_claimed_chunks",
            "region_global_player_flags",
            "region_info",
            "region_invited_players",
            "region_levels",
            "region_logs",
            "region_member_control_flags",
            "region_member_flags",
            "region_menu",
            "region_players_management",
            "region_rating",
            "region_trusted_players",
            "region_world_flags",
            "regions",
            "regions_with_welcome_signs",
            "rent_config",
            "rent_confirmation",
            "rewards",
            "sub_area",
            "sub_area_flags",
            "sub_area_member_flags",
            "sub_area_members",
            "sub_areas",
            "top_regions");

    // Homestead menus we deliberately leave on screen: no Bedrock form serves them.
    private static final Set<String> LEFT_ALONE = Set.of("rent_config", "rent_confirmation");

    @Test
    void everyUpstreamKeyIsEitherRoutedOrDeliberatelyLeftAlone() {
        for (String key : UPSTREAM_KEYS) {
            Optional<MenuRoute> route = MenuKeyRoutes.of(key);
            if (LEFT_ALONE.contains(key)) {
                assertTrue(route.isEmpty(), key + " should be left alone");
            } else {
                assertTrue(route.isPresent(), key + " has no route");
            }
        }
    }

    @Test
    void tableCoversExactlyTheUpstreamKeys() {
        assertEquals(Set.copyOf(UPSTREAM_KEYS), MenuKeyRoutes.knownKeys());
    }

    @Test
    void contextFreeMenusNeedNoRegion() {
        assertFalse(MenuKeyRoutes.of("regions").orElseThrow().needsRegion());
        assertFalse(MenuKeyRoutes.of("top_regions").orElseThrow().needsRegion());
        assertFalse(MenuKeyRoutes.of("regions_with_welcome_signs").orElseThrow().needsRegion());
    }

    @Test
    void regionScopedMenusNeedARegion() {
        assertTrue(MenuKeyRoutes.of("region_menu").orElseThrow().needsRegion());
        assertTrue(MenuKeyRoutes.of("region_claimed_chunks").orElseThrow().needsRegion());
        assertTrue(MenuKeyRoutes.of("sub_areas").orElseThrow().needsRegion());
    }

    @Test
    void memberScopedMenusFallBackToPlayersManagement() {
        assertEquals(MenuRoute.PLAYERS_MANAGEMENT, MenuKeyRoutes.of("player_info").orElseThrow());
        assertEquals(MenuRoute.PLAYERS_MANAGEMENT, MenuKeyRoutes.of("region_member_flags").orElseThrow());
        assertEquals(MenuRoute.PLAYERS_MANAGEMENT, MenuKeyRoutes.of("region_member_control_flags").orElseThrow());
    }

    @Test
    void subAreaScopedMenusFallBackToTheSubAreaList() {
        assertEquals(MenuRoute.SUB_AREAS_LIST, MenuKeyRoutes.of("sub_area").orElseThrow());
        assertEquals(MenuRoute.SUB_AREAS_LIST, MenuKeyRoutes.of("sub_area_members").orElseThrow());
        assertEquals(MenuRoute.SUB_AREAS_LIST, MenuKeyRoutes.of("sub_area_member_flags").orElseThrow());
        assertEquals(MenuRoute.SUB_AREAS_LIST, MenuKeyRoutes.of("sub_area_flags").orElseThrow());
    }

    @Test
    void listMenusKeepTheirOwnForm() {
        assertEquals(MenuRoute.MEMBERS_LIST, MenuKeyRoutes.of("region_trusted_players").orElseThrow());
        assertEquals(MenuRoute.INVITES_LIST, MenuKeyRoutes.of("region_invited_players").orElseThrow());
        assertEquals(MenuRoute.BANS_LIST, MenuKeyRoutes.of("region_banned_players").orElseThrow());
    }

    @Test
    void unknownKeysAreLeftAlone() {
        assertTrue(MenuKeyRoutes.of("something_new").isEmpty());
        assertTrue(MenuKeyRoutes.of("").isEmpty());
        assertTrue(MenuKeyRoutes.of(null).isEmpty());
    }
}
