package it.pintux.life.duelsaddon.listener;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Every menu id PhoenixDuels registers with its {@code MenuRegistry}, split into the three
 * ways this addon treats them.
 *
 * <p>The ids are the registry keys, not the menu file names: {@code menus/party/party.yml} is
 * registered as {@code party} and {@code menus/custom/duel_player.yml} as {@code duel_player}.
 * They are read back at runtime from {@code ContainerLayout.getId()}.</p>
 *
 * <p>{@link #GROUPS} is the single source of truth for what this addon replaces.
 * {@link MenuInterceptListener} builds its handler table by walking it, so an id cannot be
 * declared here without a handler existing, and a handler cannot exist for an id that is not
 * declared here. {@code DuelsMenusTest} asserts the three sets together cover {@link #ALL}, so a
 * PhoenixDuels update that renames a menu fails the build rather than silently losing a form.</p>
 */
public final class DuelsMenus {

    /**
     * Menu id mapped to the {@code menus.<group>} config toggle that enables it.
     */
    public static final Map<String, String> GROUPS = groups();

    /**
     * Resolution order for {@link PhoenixMenuResolver}.
     *
     * <p>Deliberately a {@link List} and deliberately ordered most-specific-purpose first. Two
     * registry keys can share one layout instance, and the resolver takes the first match, so this
     * order decides the winner. Iterating {@link #GROUPS} or {@link #ALL} instead would be
     * non-deterministic: {@code Map.copyOf} and {@code Set.of} randomise iteration order per JVM,
     * which made {@code /party} sometimes open the member list instead of the party menu.</p>
     */
    public static final List<String> PRIORITY = List.of(
            "queue",
            "party",
            "party_info",
            "party_invite_player",
            "party_manage_member",
            "party_ffa",
            "party_teamfight",
            "party_multiteam",
            "party_multiteam_spectators",
            "duel_player",
            "create_match",
            "custom_challenge_opponent",
            "custom_select_arena_mode",
            "unranked_select_player_mode",
            "unranked_select_arena_mode",
            "ranked_select_player_mode",
            "ranked_select_arena_mode",
            "settings",
            "stats",
            "leaderboard",
            "ongoing_matches",
            "spectator_players",
            "kit_preview",
            "lost_items");

    /**
     * Menus this addon serves as Bedrock forms.
     */
    public static final Set<String> HANDLED = GROUPS.keySet();

    /**
     * Menus that must stay on Java. The first three are item drag-and-drop inventories, which a
     * Bedrock form cannot express; {@code spectator_hotbar} is a hotbar layout, not a container.
     */
    public static final Set<String> JAVA_ONLY = Set.of(
            "items_betting",
            "player_kit_layout",
            "kit_items_editor",
            "spectator_hotbar");

    /**
     * Generic pickers PhoenixDuels reuses across several flows. What they mean depends on the
     * calling view's metadata, so routing them blind would be worse than the Java fallback.
     */
    public static final Set<String> CONTEXT_DEPENDENT = Set.of(
            "select_mode",
            "select_map",
            "select_option_menu",
            "confirmation_menu");

    /**
     * Every id registered by PhoenixDuels 4.1.0, transcribed from {@code MenuRegistry} bytecode.
     *
     * <p>Declared independently of the three sets above on purpose: the coverage test compares
     * their union against this list, so deriving it would make that test always pass.</p>
     */
    public static final Set<String> ALL = Set.of(
            "queue",
            "unranked_select_player_mode",
            "unranked_select_arena_mode",
            "ranked_select_player_mode",
            "ranked_select_arena_mode",
            "duel_player",
            "create_match",
            "custom_select_arena_mode",
            "custom_challenge_opponent",
            "lost_items",
            "party",
            "party_info",
            "party_invite_player",
            "party_manage_member",
            "party_ffa",
            "party_teamfight",
            "party_multiteam",
            "party_multiteam_spectators",
            "settings",
            "stats",
            "leaderboard",
            "ongoing_matches",
            "spectator_players",
            "kit_preview",
            "select_mode",
            "select_map",
            "select_option_menu",
            "confirmation_menu",
            "items_betting",
            "player_kit_layout",
            "kit_items_editor",
            "spectator_hotbar");

    /**
     * Layout class simple name mapped to the registry key it corresponds to.
     *
     * <p>Needed because {@code MenuRegistry.load()} calls {@code registerMenu} for only about a
     * third of the ids — the rest go through {@code registerConfiguration}, so their layouts are
     * constructed on demand and never stored in {@code MenuRegistry.menus}. Those menus are
     * therefore unresolvable through the registry no matter what key is asked for, which is why
     * {@code /duel} intercepted nothing. The layout's class is available on the instance either
     * way.</p>
     */
    public static final Map<String, String> KEY_BY_LAYOUT_CLASS = layoutClasses();

    private DuelsMenus() {}

    private static Map<String, String> layoutClasses() {
        Map<String, String> classes = new LinkedHashMap<>();
        classes.put("QueueLayoutMenu", "queue");
        classes.put("UnrankedSelectPlayerModeMenu", "unranked_select_player_mode");
        classes.put("UnrankedSelectModeMenu", "unranked_select_arena_mode");
        classes.put("RankedSelectPlayerModeMenu", "ranked_select_player_mode");
        classes.put("RankedSelectModeMenu", "ranked_select_arena_mode");
        classes.put("DuelPlayerLayoutMenu", "duel_player");
        classes.put("CreateMatchLayoutMenu", "create_match");
        classes.put("LostItemsMenu", "lost_items");
        classes.put("PartyLayoutMenu", "party");
        classes.put("PartyInfoLayoutMenu", "party_info");
        classes.put("PartyInvitePlayerLayoutMenu", "party_invite_player");
        classes.put("ManagePartyMemberLayoutMenu", "party_manage_member");
        classes.put("PartyFfaLayoutMenu", "party_ffa");
        classes.put("PartyTeamFightLayoutMenu", "party_teamfight");
        classes.put("PartyMultiTeamLayoutMenu", "party_multiteam");
        classes.put("PartySpectatorsListMenu", "party_multiteam_spectators");
        classes.put("ChooseOpponentPartyLayoutMenu", "custom_challenge_opponent");
        classes.put("SettingsLayoutMenu", "settings");
        classes.put("StatsLayoutMenu", "stats");
        classes.put("LeaderboardLayoutMenu", "leaderboard");
        classes.put("OngoingMatchesMenu", "ongoing_matches");
        classes.put("SpectatorPlayersMenu", "spectator_players");
        classes.put("KitPreviewMenu", "kit_preview");
        classes.put("ItemsBettingMenu", "items_betting");
        classes.put("KitItemsEditorMenu", "kit_items_editor");
        classes.put("SelectMapMenu", "select_map");
        classes.put("SelectModeMenu", "select_mode");
        classes.put("SelectOptionMenu", "select_option_menu");
        classes.put("ConfirmationMenu", "confirmation_menu");
        return Map.copyOf(classes);
    }

    private static Map<String, String> groups() {
        Map<String, String> groups = new LinkedHashMap<>();
        groups.put("queue", "queue");
        groups.put("unranked_select_player_mode", "queue");
        groups.put("unranked_select_arena_mode", "queue");
        groups.put("ranked_select_player_mode", "queue");
        groups.put("ranked_select_arena_mode", "queue");
        groups.put("duel_player", "duel");
        groups.put("create_match", "duel");
        groups.put("custom_select_arena_mode", "duel");
        groups.put("lost_items", "duel");
        groups.put("party", "party");
        groups.put("party_info", "party");
        groups.put("party_invite_player", "party");
        groups.put("party_manage_member", "party");
        groups.put("party_ffa", "party");
        groups.put("party_teamfight", "party");
        groups.put("party_multiteam", "party");
        groups.put("party_multiteam_spectators", "party");
        groups.put("custom_challenge_opponent", "party");
        groups.put("settings", "settings");
        groups.put("stats", "stats");
        groups.put("leaderboard", "stats");
        groups.put("ongoing_matches", "spectator");
        groups.put("spectator_players", "spectator");
        groups.put("kit_preview", "kit");
        return Map.copyOf(groups);
    }
}
