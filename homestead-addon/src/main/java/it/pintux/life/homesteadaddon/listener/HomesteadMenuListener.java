package it.pintux.life.homesteadaddon.listener;

import it.pintux.life.homesteadaddon.gateway.HomesteadGateway;
import it.pintux.life.homesteadaddon.service.BedrockChunkService;
import it.pintux.life.homesteadaddon.service.BedrockFlagService;
import it.pintux.life.homesteadaddon.service.BedrockLevelService;
import it.pintux.life.homesteadaddon.service.BedrockLogService;
import it.pintux.life.homesteadaddon.service.BedrockMemberService;
import it.pintux.life.homesteadaddon.service.BedrockMiscService;
import it.pintux.life.homesteadaddon.service.BedrockRegionService;
import it.pintux.life.homesteadaddon.service.BedrockSubAreaService;
import it.pintux.life.homesteadaddon.util.MenuKeyRoutes;
import it.pintux.life.homesteadaddon.util.MenuRoute;
import me.tayebyassine.homestead.api.events.menu.MenuOpenEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * Replaces Homestead's chest GUIs with Bedrock forms for Bedrock players.
 *
 * <p>Command interception already covers the usual entry points without ever opening a
 * chest. This is the net for every other path — welcome signs, other plugins, a menu
 * opened from code — where the chest is already on screen by the time we hear about it,
 * so it gets closed and the matching form takes its place.</p>
 */
public final class HomesteadMenuListener implements Listener {

    private final HomesteadGateway gateway;
    private final BedrockRegionService regionService;
    private final BedrockMemberService memberService;
    private final BedrockFlagService flagService;
    private final BedrockSubAreaService subAreaService;
    private final BedrockLevelService levelService;
    private final BedrockLogService logService;
    private final BedrockMiscService miscService;
    private final BedrockChunkService chunkService;

    public HomesteadMenuListener(HomesteadGateway gateway,
                                 BedrockRegionService regionService,
                                 BedrockMemberService memberService,
                                 BedrockFlagService flagService,
                                 BedrockSubAreaService subAreaService,
                                 BedrockLevelService levelService,
                                 BedrockLogService logService,
                                 BedrockMiscService miscService,
                                 BedrockChunkService chunkService) {
        this.gateway = gateway;
        this.regionService = regionService;
        this.memberService = memberService;
        this.flagService = flagService;
        this.subAreaService = subAreaService;
        this.levelService = levelService;
        this.logService = logService;
        this.miscService = miscService;
        this.chunkService = chunkService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMenuOpen(MenuOpenEvent event) {
        Player player = event.getPlayer();
        if (!regionService.shouldHandle(player)) {
            return;
        }
        Optional<MenuRoute> route = MenuKeyRoutes.of(event.getMenuKey());
        if (route.isEmpty()) {
            return;
        }
        MenuRoute target = route.get();

        long regionId = 0L;
        if (target.needsRegion()) {
            OptionalLong current = gateway.targetRegionId(player);
            if (current.isEmpty()) {
                return;
            }
            regionId = current.getAsLong();
        }

        player.closeInventory();
        open(target, player, regionId);
    }

    private void open(MenuRoute route, Player player, long regionId) {
        switch (route) {
            case REGION_LIST -> regionService.openRegionList(player, false, 1);
            case TOP_REGIONS -> regionService.openTopRegions(player, null);
            case WELCOME_SIGNS -> regionService.openWelcomeSigns(player, 1);
            case REGION_MENU -> regionService.openRegionMenu(player, regionId);
            case REGION_INFO -> regionService.openRegionInfo(player, regionId);
            case CLAIMED_CHUNKS -> chunkService.openClaimedChunks(player, regionId, 1);
            case MAP_COLOR -> chunkService.openMapColor(player, regionId);
            case MAP_ICON -> chunkService.openMapIcon(player, regionId);
            case LOGS -> logService.openLogs(player, regionId, 1);
            case RATING -> miscService.openRating(player, regionId);
            case MISC_SETTINGS -> miscService.openMiscSettings(player, regionId);
            case LEVELS -> levelService.openLevels(player, regionId);
            case REWARDS -> levelService.openRewards(player, regionId);
            case WORLD_FLAGS -> flagService.openWorldFlags(player, regionId);
            case GLOBAL_PLAYER_FLAGS -> flagService.openGlobalPlayerFlags(player, regionId);
            case PLAYERS_MANAGEMENT -> memberService.openPlayersManagement(player, regionId);
            case MEMBERS_LIST -> memberService.openMembersList(player, regionId, 1);
            case INVITES_LIST -> memberService.openInvitesList(player, regionId, 1);
            case BANS_LIST -> memberService.openBansList(player, regionId, 1);
            case SUB_AREAS_LIST -> subAreaService.openSubAreasList(player, regionId, 1);
        }
    }
}
