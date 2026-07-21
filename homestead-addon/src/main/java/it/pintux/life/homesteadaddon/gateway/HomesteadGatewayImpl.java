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
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import tfagaming.projects.minecraft.homestead.flags.ControlFlags;
import tfagaming.projects.minecraft.homestead.flags.PlayerFlags;
import tfagaming.projects.minecraft.homestead.flags.WorldFlags;
import tfagaming.projects.minecraft.homestead.managers.BanManager;
import tfagaming.projects.minecraft.homestead.managers.ChunkManager;
import tfagaming.projects.minecraft.homestead.managers.InviteManager;
import tfagaming.projects.minecraft.homestead.managers.LevelManager;
import tfagaming.projects.minecraft.homestead.managers.LogManager;
import tfagaming.projects.minecraft.homestead.managers.MemberManager;
import tfagaming.projects.minecraft.homestead.managers.RateManager;
import tfagaming.projects.minecraft.homestead.managers.RegionManager;
import tfagaming.projects.minecraft.homestead.managers.SubAreaManager;
import tfagaming.projects.minecraft.homestead.models.Level;
import tfagaming.projects.minecraft.homestead.models.Region;
import tfagaming.projects.minecraft.homestead.models.RegionBan;
import tfagaming.projects.minecraft.homestead.models.RegionChunk;
import tfagaming.projects.minecraft.homestead.models.RegionInvite;
import tfagaming.projects.minecraft.homestead.models.RegionLog;
import tfagaming.projects.minecraft.homestead.models.RegionMember;
import tfagaming.projects.minecraft.homestead.models.RegionRate;
import tfagaming.projects.minecraft.homestead.models.SubArea;
import tfagaming.projects.minecraft.homestead.models.serialize.SeLocation;
import tfagaming.projects.minecraft.homestead.tools.minecraft.plugins.MapColor;
import tfagaming.projects.minecraft.homestead.tools.minecraft.plugins.MapIcon;
import tfagaming.projects.minecraft.homestead.tools.minecraft.rewards.Rewards;
import tfagaming.projects.minecraft.homestead.weatherandtime.RegionTime;
import tfagaming.projects.minecraft.homestead.weatherandtime.RegionWeather;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public final class HomesteadGatewayImpl implements HomesteadGateway {
    private final Logger logger;

    public HomesteadGatewayImpl(Logger logger) {
        this.logger = logger;
    }

    @Override
    public boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("Homestead") != null;
    }


    @Override
    public List<RegionView> regionsFor(OfflinePlayer player) {
        UUID id = player.getUniqueId();
        Map<Long, RegionView> byId = new LinkedHashMap<>();
        for (Region region : RegionManager.getRegionsOwnedByPlayer(id)) {
            if (region != null) byId.putIfAbsent(region.getUniqueId(), toView(region));
        }
        for (Region region : RegionManager.getRegionsHasPlayerAsMember(id)) {
            if (region != null) byId.putIfAbsent(region.getUniqueId(), toView(region));
        }
        return new ArrayList<>(byId.values());
    }

    @Override
    public List<RegionView> allRegions() {
        List<RegionView> out = new ArrayList<>();
        for (Region region : RegionManager.getAll()) {
            if (region != null) out.add(toView(region));
        }
        return out;
    }

    @Override
    public Optional<RegionView> region(long regionId) {
        Region region = RegionManager.findRegion(regionId);
        return region == null ? Optional.empty() : Optional.of(toView(region));
    }

    @Override
    public boolean teleport(Player player, long regionId) {
        Region region = RegionManager.findRegion(regionId);
        if (region == null) {
            return false;
        }
        return teleportTo(player, region.getLocation());
    }

    @Override
    public boolean leaveRegion(OfflinePlayer player, long regionId) {
        if (!isMember(regionId, player)) {
            return false;
        }
        MemberManager.removeMemberFromRegion(player, regionId);
        return true;
    }

    @Override
    public boolean isMember(long regionId, OfflinePlayer player) {
        return MemberManager.isMemberOfRegion(regionId, player.getUniqueId());
    }

    @Override
    public boolean isOwner(long regionId, OfflinePlayer player) {
        Region region = RegionManager.findRegion(regionId);
        return region != null && region.getOwnerId() != null && region.getOwnerId().equals(player.getUniqueId());
    }


    @Override
    public List<MemberView> membersOf(long regionId) {
        List<MemberView> out = new ArrayList<>();
        for (RegionMember member : MemberManager.getMembersOfRegion(regionId)) {
            if (member != null) out.add(toMember(member));
        }
        return out;
    }

    @Override
    public List<InviteView> invitesOf(long regionId) {
        List<InviteView> out = new ArrayList<>();
        for (RegionInvite invite : InviteManager.getInvitesOfRegion(regionId)) {
            if (invite != null) {
                out.add(new InviteView(invite.getUniqueId(), invite.getPlayerId(), invite.getPlayerName(), invite.getInvitedAt()));
            }
        }
        return out;
    }

    @Override
    public List<BanView> bansOf(long regionId) {
        List<BanView> out = new ArrayList<>();
        for (RegionBan ban : BanManager.getBansOfRegion(regionId)) {
            if (ban != null) {
                out.add(new BanView(ban.getUniqueId(), ban.getPlayerId(), ban.getPlayerName(), ban.getReason(), ban.getBannedAt()));
            }
        }
        return out;
    }

    @Override
    public boolean invitePlayer(long regionId, String playerName) {
        OfflinePlayer target = resolvePlayer(playerName);
        if (target == null) {
            return false;
        }
        InviteManager.invitePlayer(regionId, target);
        return true;
    }

    @Override
    public boolean revokeInvite(long inviteId) {
        InviteManager.deleteInvite(inviteId);
        return true;
    }

    @Override
    public boolean banPlayer(long regionId, String playerName, String reason) {
        OfflinePlayer target = resolvePlayer(playerName);
        if (target == null) {
            return false;
        }
        BanManager.banPlayer(regionId, target, reason);
        return true;
    }

    @Override
    public boolean unbanPlayer(long regionId, UUID playerId) {
        BanManager.unbanPlayer(regionId, playerId);
        return true;
    }

    @Override
    public boolean kickMember(long regionId, UUID playerId) {
        MemberManager.removeMemberFromRegion(Bukkit.getOfflinePlayer(playerId), regionId);
        return true;
    }

    @Override
    public boolean canManageMembers(long regionId, OfflinePlayer player) {
        return isOwner(regionId, player) || hasControlFlag(regionId, player, "SET_MEMBER_FLAGS");
    }

    @Override
    public boolean hasControlFlag(long regionId, OfflinePlayer player, String controlFlagName) {
        long bit = ControlFlags.valueOf(controlFlagName);
        return bit != 0L && MemberManager.hasControlFlag(regionId, player, bit);
    }


    @Override
    public List<String> flagNames(FlagDomain domain) {
        return switch (domain) {
            case WORLD_FLAGS -> WorldFlags.getFlags();
            case PLAYER_FLAGS -> PlayerFlags.getFlags();
            case CONTROL_FLAGS -> ControlFlags.getFlags();
        };
    }

    @Override
    public long flagValue(FlagDomain domain, String name) {
        return switch (domain) {
            case WORLD_FLAGS -> WorldFlags.valueOf(name);
            case PLAYER_FLAGS -> PlayerFlags.valueOf(name);
            case CONTROL_FLAGS -> ControlFlags.valueOf(name);
        };
    }

    @Override
    public boolean setWorldFlags(long regionId, long mask) {
        Region region = RegionManager.findRegion(regionId);
        if (region == null) {
            return false;
        }
        region.setWorldFlags(mask);
        return true;
    }

    @Override
    public boolean setGlobalPlayerFlags(long regionId, long mask) {
        Region region = RegionManager.findRegion(regionId);
        if (region == null) {
            return false;
        }
        region.setPlayerFlags(mask);
        return true;
    }

    @Override
    public boolean setMemberPlayerFlags(long regionId, UUID memberId, long mask) {
        return MemberManager.setPlayerFlags(regionId, Bukkit.getOfflinePlayer(memberId), mask);
    }

    @Override
    public boolean setMemberControlFlags(long regionId, UUID memberId, long mask) {
        return MemberManager.setControlFlags(regionId, Bukkit.getOfflinePlayer(memberId), mask);
    }


    @Override
    public List<SubAreaView> subAreasOf(long regionId) {
        List<SubAreaView> out = new ArrayList<>();
        for (SubArea subArea : SubAreaManager.getSubAreasOfRegion(regionId)) {
            if (subArea != null) out.add(toSubArea(subArea));
        }
        return out;
    }

    @Override
    public Optional<SubAreaView> subArea(long subAreaId) {
        SubArea subArea = SubAreaManager.findSubArea(subAreaId);
        return subArea == null ? Optional.empty() : Optional.of(toSubArea(subArea));
    }

    @Override
    public boolean renameSubArea(long subAreaId, String newName) {
        SubArea subArea = SubAreaManager.findSubArea(subAreaId);
        if (subArea == null) {
            return false;
        }
        subArea.setName(newName);
        return true;
    }

    @Override
    public boolean deleteSubArea(long subAreaId) {
        SubAreaManager.deleteSubArea(subAreaId);
        return true;
    }

    @Override
    public boolean endSubAreaRent(long subAreaId) {
        SubArea subArea = SubAreaManager.findSubArea(subAreaId);
        if (subArea == null) {
            return false;
        }
        subArea.setRent(null);
        return true;
    }

    @Override
    public boolean setSubAreaFlags(long subAreaId, long mask) {
        SubArea subArea = SubAreaManager.findSubArea(subAreaId);
        if (subArea == null) {
            return false;
        }
        subArea.setPlayerFlags(mask);
        return true;
    }

    @Override
    public List<MemberView> subAreaMembers(long subAreaId) {
        List<MemberView> out = new ArrayList<>();
        for (RegionMember member : MemberManager.getMembersOfSubArea(subAreaId)) {
            if (member != null) out.add(toMember(member));
        }
        return out;
    }

    @Override
    public boolean addSubAreaMember(long subAreaId, String playerName) {
        OfflinePlayer target = resolvePlayer(playerName);
        if (target == null) {
            return false;
        }
        MemberManager.addMemberToSubArea(target, subAreaId);
        return true;
    }

    @Override
    public boolean removeSubAreaMember(long subAreaId, UUID memberId) {
        MemberManager.removeMemberFromSubArea(Bukkit.getOfflinePlayer(memberId), subAreaId);
        return true;
    }

    @Override
    public boolean setSubAreaMemberFlags(long subAreaId, UUID memberId, long mask) {
        RegionMember member = MemberManager.getMemberOfSubArea(subAreaId, Bukkit.getOfflinePlayer(memberId));
        if (member == null) {
            return false;
        }
        member.setPlayerFlags(mask);
        return true;
    }

    @Override
    public boolean canManageSubAreas(long regionId, OfflinePlayer player) {
        return isOwner(regionId, player) || hasControlFlag(regionId, player, "MANAGE_SUBAREAS");
    }


    @Override
    public Optional<LevelView> level(long regionId) {
        Level level = LevelManager.getLevelByRegion(regionId);
        if (level == null) {
            return Optional.empty();
        }
        return Optional.of(new LevelView(
                level.getLevel(),
                level.getXpProgress(),
                level.getXpForNextLevel(),
                level.getProgressPercentage(),
                level.getTotalExperience(),
                LevelManager.getRank(regionId)
        ));
    }

    @Override
    public List<LogView> logs(long regionId) {
        List<LogView> out = new ArrayList<>();
        for (RegionLog log : LogManager.getLogs(regionId)) {
            if (log != null) {
                out.add(new LogView(log.getAuthor(), log.getMessage(), log.getSentAt(), log.isRead()));
            }
        }
        return out;
    }

    @Override
    public boolean markLogsRead(long regionId) {
        LogManager.markAllAsRead(regionId);
        return true;
    }

    @Override
    public boolean clearLogs(long regionId) {
        LogManager.deleteLogsOfRegion(regionId);
        return true;
    }

    @Override
    public boolean canManageLogs(long regionId, OfflinePlayer player) {
        return isOwner(regionId, player) || hasControlFlag(regionId, player, "MANAGE_LOGS");
    }

    @Override
    public RatingView rating(long regionId, OfflinePlayer player) {
        double average = RateManager.getAverageRating(regionId);
        int count = RateManager.getRateCount(regionId);
        RegionRate rate = RateManager.getPlayerRate(player, regionId);
        return new RatingView(average, count, rate != null ? rate.getRate() : 0);
    }

    @Override
    public boolean rateRegion(long regionId, OfflinePlayer player, int score) {
        RateManager.rateRegion(regionId, player, score);
        return true;
    }

    @Override
    public boolean removeRating(long regionId, OfflinePlayer player) {
        return RateManager.deletePlayerRating(player, regionId);
    }

    @Override
    public RewardsView rewards(long regionId, OfflinePlayer player) {
        Region region = RegionManager.findRegion(regionId);
        if (region == null) {
            return new RewardsView(0, 0, 0, 0);
        }
        return new RewardsView(
                Rewards.getChunksByEachMember(region),
                Rewards.getSubAreasByEachMember(region),
                Rewards.getChunksByPlayTime(player),
                Rewards.getSubAreasByPlayTime(player)
        );
    }

    @Override
    public boolean renameRegion(long regionId, String newName) {
        Region region = RegionManager.findRegion(regionId);
        if (region == null) {
            return false;
        }
        RegionManager.renameRegion(region, newName);
        return true;
    }

    @Override
    public boolean setDisplayName(long regionId, String displayName) {
        Region region = RegionManager.findRegion(regionId);
        if (region == null) {
            return false;
        }
        region.setDisplayName(displayName);
        return true;
    }

    @Override
    public boolean setDescription(long regionId, String description) {
        Region region = RegionManager.findRegion(regionId);
        if (region == null) {
            return false;
        }
        region.setDescription(description);
        return true;
    }

    @Override
    public boolean setRegionSpawn(long regionId, Player at) {
        Region region = RegionManager.findRegion(regionId);
        if (region == null) {
            return false;
        }
        region.setLocation(at.getLocation());
        return true;
    }

    @Override
    public boolean transferOwnership(long regionId, String playerName) {
        Region region = RegionManager.findRegion(regionId);
        if (region == null) {
            return false;
        }
        OfflinePlayer target = resolvePlayer(playerName);
        if (target == null) {
            return false;
        }
        region.setOwner(target);
        return true;
    }

    @Override
    public boolean deleteRegion(long regionId, OfflinePlayer performedBy) {
        RegionManager.deleteRegion(regionId, performedBy);
        return true;
    }


    @Override
    public List<ChunkView> chunksOf(long regionId) {
        List<ChunkView> out = new ArrayList<>();
        for (RegionChunk chunk : ChunkManager.getChunksOfRegion(regionId)) {
            if (chunk == null) {
                continue;
            }
            UUID worldId = chunk.getWorldId();
            World world = worldId != null ? Bukkit.getWorld(worldId) : null;
            out.add(new ChunkView(world != null ? world.getName() : "?", worldId, chunk.getX(), chunk.getZ()));
        }
        return out;
    }

    @Override
    public boolean unclaimChunk(long regionId, UUID worldId, int x, int z) {
        World world = worldId == null ? null : Bukkit.getWorld(worldId);
        if (world == null) {
            return false;
        }
        ChunkManager.unclaimChunk(regionId, world.getChunkAt(x, z));
        return true;
    }

    @Override
    public boolean teleportToChunk(Player player, UUID worldId, int x, int z) {
        World world = worldId == null ? null : Bukkit.getWorld(worldId);
        if (world == null) {
            return false;
        }
        int bx = (x << 4) + 8;
        int bz = (z << 4) + 8;
        int by = world.getHighestBlockYAt(bx, bz) + 1;
        return player.teleport(new Location(world, bx + 0.5, by, bz + 0.5));
    }

    @Override
    public boolean canManageChunks(long regionId, OfflinePlayer player) {
        return isOwner(regionId, player) || hasControlFlag(regionId, player, "UNCLAIM_CHUNKS");
    }

    @Override
    public List<String> mapColors() {
        return MapColor.getAll();
    }

    @Override
    public String mapColorName(int value) {
        String name = MapColor.fromInt(value);
        return name != null ? name : String.valueOf(value);
    }

    @Override
    public boolean setMapColor(long regionId, String colorName) {
        Region region = RegionManager.findRegion(regionId);
        if (region == null) {
            return false;
        }
        int value = MapColor.parseFromString(colorName);
        if (value < 0) {
            return false;
        }
        region.setMapColor(value);
        return true;
    }

    @Override
    public List<String> mapIcons() {
        return MapIcon.getAllIcons();
    }

    @Override
    public boolean setMapIcon(long regionId, String icon) {
        Region region = RegionManager.findRegion(regionId);
        if (region == null) {
            return false;
        }
        region.setMapIcon(icon);
        return true;
    }

    @Override
    public boolean cycleWeather(long regionId) {
        Region region = RegionManager.findRegion(regionId);
        if (region == null) {
            return false;
        }
        region.setWeather(RegionWeather.next(region.getWeather()));
        return true;
    }

    @Override
    public boolean cycleTime(long regionId) {
        Region region = RegionManager.findRegion(regionId);
        if (region == null) {
            return false;
        }
        region.setTime(RegionTime.next(region.getTime()));
        return true;
    }

    @Override
    public String weatherName(int value) {
        String name = RegionWeather.from(value);
        return name != null ? name : String.valueOf(value);
    }

    @Override
    public String timeName(int value) {
        String name = RegionTime.from(value);
        return name != null ? name : String.valueOf(value);
    }

    @Override
    public List<RegionView> topRegions(String sorting, int limit) {
        List<RegionView> all = allRegions();
        Comparator<RegionView> comparator;
        switch (sorting == null ? "BANK" : sorting) {
            case "CHUNKS_COUNT" -> comparator = Comparator.comparingInt(RegionView::chunkCount).reversed();
            case "MEMBERS_COUNT" -> comparator = Comparator.comparingInt(RegionView::memberCount).reversed();
            case "CREATION_DATE" -> comparator = Comparator.comparingLong(RegionView::createdAt);
            case "RATING" -> {
                Map<Long, Double> averages = new HashMap<>();
                for (RegionView region : all) {
                    averages.put(region.id(), RateManager.getAverageRating(region.id()));
                }
                comparator = Comparator.comparingDouble((RegionView region) -> averages.getOrDefault(region.id(), 0.0)).reversed();
            }
            default -> comparator = Comparator.comparingDouble(RegionView::bank).reversed();
        }
        all.sort(comparator);
        return all.size() > limit ? new ArrayList<>(all.subList(0, limit)) : all;
    }

    @Override
    public List<RegionView> welcomeSignRegions() {
        List<RegionView> out = new ArrayList<>();
        for (Region region : RegionManager.getRegionsWithWelcomeSigns()) {
            if (region != null) out.add(toView(region));
        }
        return out;
    }

    @Override
    public boolean teleportToWelcomeSign(Player player, long regionId) {
        Region region = RegionManager.findRegion(regionId);
        if (region == null) {
            return false;
        }
        return teleportTo(player, region.getWelcomeSign());
    }


    private RegionView toView(Region region) {
        long id = region.getUniqueId();
        return new RegionView(
                id,
                region.getName(),
                region.getOwnerId(),
                region.getOwnerName(),
                region.getBank(),
                region.getMapColor(),
                region.getMapIcon(),
                region.getWeather(),
                region.getTime(),
                region.getCreatedAt(),
                region.isPublic(),
                region.getWorldFlags(),
                region.getPlayerFlags(),
                MemberManager.getMemberCount(id),
                ChunkManager.getChunkCount(id),
                SubAreaManager.getSubAreaCount(id),
                RegionManager.getGlobalRank(id)
        );
    }

    private MemberView toMember(RegionMember member) {
        return new MemberView(
                member.getUniqueId(),
                member.getPlayerId(),
                member.getPlayerName(),
                member.getPlayerFlags(),
                member.getControlFlags(),
                member.getJoinedAt()
        );
    }

    private SubAreaView toSubArea(SubArea subArea) {
        return new SubAreaView(
                subArea.getUniqueId(),
                subArea.getRegionId(),
                subArea.getName(),
                subArea.getPlayerFlags(),
                subArea.getVolume(),
                subArea.getCreatedAt(),
                subArea.getRent() != null
        );
    }

    private boolean teleportTo(Player player, SeLocation location) {
        if (location == null) {
            return false;
        }
        Location bukkit = location.toBukkit();
        if (bukkit == null || bukkit.getWorld() == null) {
            return false;
        }
        return player.teleport(bukkit);
    }

    private OfflinePlayer resolvePlayer(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Player online = Bukkit.getPlayerExact(name.trim());
        return online != null ? online : Bukkit.getOfflinePlayer(name.trim());
    }
}
