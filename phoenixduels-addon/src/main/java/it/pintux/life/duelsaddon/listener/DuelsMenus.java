package it.pintux.life.duelsaddon.listener;

import java.util.Set;

public final class DuelsMenus {

    public static final Set<String> JAVA_ONLY = Set.of(
            "items_betting",
            "player_kit_layout",
            "kit_items_editor",
            "spectator_hotbar");

    public static final Set<String> CONTEXT_DEPENDENT = Set.of(
            "select_mode",
            "select_map",
            "select_option_menu",
            "confirmation_menu");

    public static final Set<String> HANDLED = Set.of(
            "queue",
            "unranked_select_player_mode",
            "unranked_select_arena_mode",
            "ranked_select_player_mode",
            "ranked_select_arena_mode",
            "duel_player",
            "create_match",
            "custom_select_arena_mode",
            "lost_items",
            "party",
            "party_info",
            "party_invite_player",
            "party_manage_member",
            "party_ffa",
            "party_teamfight",
            "party_multiteam",
            "party_multiteam_spectators",
            "custom_challenge_opponent",
            "settings",
            "stats",
            "leaderboard",
            "ongoing_matches",
            "spectator_players",
            "kit_preview");

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

    private DuelsMenus() {}
}
