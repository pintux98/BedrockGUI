package it.pintux.life.homesteadaddon.gateway;

import it.pintux.life.homesteadaddon.model.BanView;
import it.pintux.life.homesteadaddon.model.ChunkView;
import it.pintux.life.homesteadaddon.model.InviteView;
import it.pintux.life.homesteadaddon.model.LevelView;
import it.pintux.life.homesteadaddon.model.LogView;
import it.pintux.life.homesteadaddon.model.MemberView;
import it.pintux.life.homesteadaddon.model.RatingView;
import it.pintux.life.homesteadaddon.model.RegionView;
import it.pintux.life.homesteadaddon.model.RewardsView;
import it.pintux.life.homesteadaddon.model.SubAreaView;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HomesteadGateway {

    boolean isAvailable();


    List<RegionView> regionsFor(OfflinePlayer player);

    List<RegionView> allRegions();

    Optional<RegionView> region(long regionId);

    boolean teleport(Player player, long regionId);

    boolean leaveRegion(OfflinePlayer player, long regionId);

    boolean isMember(long regionId, OfflinePlayer player);

    boolean isOwner(long regionId, OfflinePlayer player);


    List<MemberView> membersOf(long regionId);

    List<InviteView> invitesOf(long regionId);

    List<BanView> bansOf(long regionId);

    boolean invitePlayer(long regionId, String playerName);

    boolean revokeInvite(long inviteId);

    boolean banPlayer(long regionId, String playerName, String reason);

    boolean unbanPlayer(long regionId, UUID playerId);

    boolean kickMember(long regionId, UUID playerId);

    boolean canManageMembers(long regionId, OfflinePlayer player);

    boolean hasControlFlag(long regionId, OfflinePlayer player, String controlFlagName);


    List<String> flagNames(FlagDomain domain);

    long flagValue(FlagDomain domain, String name);

    boolean setWorldFlags(long regionId, long mask);

    boolean setGlobalPlayerFlags(long regionId, long mask);

    boolean setMemberPlayerFlags(long regionId, UUID memberId, long mask);

    boolean setMemberControlFlags(long regionId, UUID memberId, long mask);


    List<SubAreaView> subAreasOf(long regionId);

    Optional<SubAreaView> subArea(long subAreaId);

    boolean renameSubArea(long subAreaId, String newName);

    boolean deleteSubArea(long subAreaId);

    boolean endSubAreaRent(long subAreaId);

    boolean setSubAreaFlags(long subAreaId, long mask);

    List<MemberView> subAreaMembers(long subAreaId);

    boolean addSubAreaMember(long subAreaId, String playerName);

    boolean removeSubAreaMember(long subAreaId, UUID memberId);

    boolean setSubAreaMemberFlags(long subAreaId, UUID memberId, long mask);

    boolean canManageSubAreas(long regionId, OfflinePlayer player);


    Optional<LevelView> level(long regionId);

    List<LogView> logs(long regionId);

    boolean markLogsRead(long regionId);

    boolean clearLogs(long regionId);

    boolean canManageLogs(long regionId, OfflinePlayer player);

    RatingView rating(long regionId, OfflinePlayer player);

    boolean rateRegion(long regionId, OfflinePlayer player, int score);

    boolean removeRating(long regionId, OfflinePlayer player);

    RewardsView rewards(long regionId, OfflinePlayer player);

    boolean renameRegion(long regionId, String newName);

    boolean setDisplayName(long regionId, String displayName);

    boolean setDescription(long regionId, String description);

    boolean setRegionSpawn(long regionId, Player at);

    boolean transferOwnership(long regionId, String playerName);

    boolean deleteRegion(long regionId, OfflinePlayer performedBy);


    List<ChunkView> chunksOf(long regionId);

    boolean unclaimChunk(long regionId, UUID worldId, int x, int z);

    boolean teleportToChunk(Player player, UUID worldId, int x, int z);

    boolean canManageChunks(long regionId, OfflinePlayer player);

    List<String> mapColors();

    String mapColorName(int value);

    boolean setMapColor(long regionId, String colorName);

    List<String> mapIcons();

    boolean setMapIcon(long regionId, String icon);

    boolean cycleWeather(long regionId);

    boolean cycleTime(long regionId);

    String weatherName(int value);

    String timeName(int value);

    List<RegionView> topRegions(String sorting, int limit);

    List<RegionView> welcomeSignRegions();

    boolean teleportToWelcomeSign(Player player, long regionId);
}
