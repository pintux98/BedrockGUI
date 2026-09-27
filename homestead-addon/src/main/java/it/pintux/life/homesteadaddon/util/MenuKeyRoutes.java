package it.pintux.life.homesteadaddon.util;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Maps Homestead's MENU_KEY constants onto the Bedrock form that replaces each menu.
 *
 * <p>MenuOpenEvent carries only the key, so menus scoped to a member or a sub-area cannot
 * recover their target id and fall back to the nearest parent form. Keys with no Bedrock
 * equivalent resolve to empty, which leaves Homestead's own chest GUI on screen.</p>
 */
public final class MenuKeyRoutes {

    private static final Map<String, MenuRoute> ROUTES = Map.ofEntries(
            Map.entry("regions", MenuRoute.REGION_LIST),
            Map.entry("top_regions", MenuRoute.TOP_REGIONS),
            Map.entry("regions_with_welcome_signs", MenuRoute.WELCOME_SIGNS),

            Map.entry("region_menu", MenuRoute.REGION_MENU),
            Map.entry("region_info", MenuRoute.REGION_INFO),
            Map.entry("region_claimed_chunks", MenuRoute.CLAIMED_CHUNKS),
            Map.entry("region_logs", MenuRoute.LOGS),
            Map.entry("region_rating", MenuRoute.RATING),
            Map.entry("region_levels", MenuRoute.LEVELS),
            Map.entry("rewards", MenuRoute.REWARDS),
            Map.entry("miscellaneous_settings", MenuRoute.MISC_SETTINGS),
            Map.entry("map_color", MenuRoute.MAP_COLOR),
            Map.entry("map_icon", MenuRoute.MAP_ICON),
            Map.entry("region_world_flags", MenuRoute.WORLD_FLAGS),
            Map.entry("region_global_player_flags", MenuRoute.GLOBAL_PLAYER_FLAGS),
            Map.entry("region_players_management", MenuRoute.PLAYERS_MANAGEMENT),
            Map.entry("region_trusted_players", MenuRoute.MEMBERS_LIST),
            Map.entry("region_invited_players", MenuRoute.INVITES_LIST),
            Map.entry("region_banned_players", MenuRoute.BANS_LIST),
            Map.entry("sub_areas", MenuRoute.SUB_AREAS_LIST),

            Map.entry("player_info", MenuRoute.PLAYERS_MANAGEMENT),
            Map.entry("region_member_flags", MenuRoute.PLAYERS_MANAGEMENT),
            Map.entry("region_member_control_flags", MenuRoute.PLAYERS_MANAGEMENT),
            Map.entry("sub_area", MenuRoute.SUB_AREAS_LIST),
            Map.entry("sub_area_flags", MenuRoute.SUB_AREAS_LIST),
            Map.entry("sub_area_members", MenuRoute.SUB_AREAS_LIST),
            Map.entry("sub_area_member_flags", MenuRoute.SUB_AREAS_LIST));

    private static final Set<String> LEFT_ALONE = Set.of("rent_config", "rent_confirmation");

    private MenuKeyRoutes() {
    }

    public static Optional<MenuRoute> of(String menuKey) {
        if (menuKey == null || menuKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ROUTES.get(menuKey.trim().toLowerCase(Locale.ROOT)));
    }

    public static Set<String> knownKeys() {
        Set<String> keys = new HashSet<>(ROUTES.keySet());
        keys.addAll(LEFT_ALONE);
        return Set.copyOf(keys);
    }
}
