package it.pintux.life.homesteadaddon.util;

public enum MenuRoute {
    REGION_LIST(false),
    TOP_REGIONS(false),
    WELCOME_SIGNS(false),

    REGION_MENU(true),
    REGION_INFO(true),
    CLAIMED_CHUNKS(true),
    LOGS(true),
    RATING(true),
    LEVELS(true),
    REWARDS(true),
    MISC_SETTINGS(true),
    MAP_COLOR(true),
    MAP_ICON(true),
    WORLD_FLAGS(true),
    GLOBAL_PLAYER_FLAGS(true),
    PLAYERS_MANAGEMENT(true),
    MEMBERS_LIST(true),
    INVITES_LIST(true),
    BANS_LIST(true),
    SUB_AREAS_LIST(true);

    private final boolean needsRegion;

    MenuRoute(boolean needsRegion) {
        this.needsRegion = needsRegion;
    }

    public boolean needsRegion() {
        return needsRegion;
    }
}
