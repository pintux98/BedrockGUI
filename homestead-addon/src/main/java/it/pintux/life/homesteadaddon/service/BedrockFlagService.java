package it.pintux.life.homesteadaddon.service;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.homesteadaddon.config.HomesteadAddonConfiguration;
import it.pintux.life.homesteadaddon.gateway.FlagDomain;
import it.pintux.life.homesteadaddon.gateway.HomesteadGateway;
import it.pintux.life.homesteadaddon.model.MemberView;
import it.pintux.life.homesteadaddon.model.RegionView;
import it.pintux.life.homesteadaddon.model.SubAreaView;
import it.pintux.life.homesteadaddon.util.BukkitFormPlayer;
import it.pintux.life.homesteadaddon.util.HomesteadActionPayloads;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongConsumer;

public final class BedrockFlagService {
    private static final String SET_WORLD_FLAGS = "SET_WORLD_FLAGS";
    private static final String SET_GLOBAL_FLAGS = "SET_GLOBAL_FLAGS";
    private static final String SET_MEMBER_FLAGS = "SET_MEMBER_FLAGS";

    private final HomesteadAddonConfiguration config;
    private final HomesteadGateway gateway;

    public BedrockFlagService(HomesteadAddonConfiguration config, HomesteadGateway gateway) {
        this.config = config;
        this.gateway = gateway;
    }


    public void openFlagsChooser(Player player, long regionId) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        RegionView region = requireRegion(player, regionId);
        if (region == null) {
            return;
        }
        boolean canGlobal = canSet(player, regionId, SET_GLOBAL_FLAGS);
        boolean canWorld = canSet(player, regionId, SET_WORLD_FLAGS);
        if (!canGlobal && !canWorld) {
            player.sendMessage(config.text("messages.no-permission"));
            return;
        }
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(
                config.apply(config.text("flags.chooser-title"), Map.of("name", region.name())));
        form.content(config.text("flags.chooser-content"));
        if (canGlobal) {
            form.button(config.text("flags.button-global"), fp -> openGlobalPlayerFlags(player, regionId));
        }
        if (canWorld) {
            form.button(config.text("flags.button-world"), fp -> openWorldFlags(player, regionId));
        }
        form.button(config.text("common.back-button"),
                fp -> navigate(fp, "hs_region_menu:" + regionId));
        form.send(new BukkitFormPlayer(player));
    }


    public void openWorldFlags(Player player, long regionId) {
        RegionView region = guard(player, regionId, SET_WORLD_FLAGS);
        if (region == null) {
            return;
        }
        buildFlagForm(player,
                config.apply(config.text("flags.world-title"), Map.of("name", region.name())),
                FlagDomain.WORLD_FLAGS, region.worldFlags(),
                mask -> {
                    gateway.setWorldFlags(regionId, mask);
                    player.sendMessage(config.text("messages.flags-updated"));
                    openFlagsChooser(player, regionId);
                });
    }

    public void openGlobalPlayerFlags(Player player, long regionId) {
        RegionView region = guard(player, regionId, SET_GLOBAL_FLAGS);
        if (region == null) {
            return;
        }
        buildFlagForm(player,
                config.apply(config.text("flags.global-title"), Map.of("name", region.name())),
                FlagDomain.PLAYER_FLAGS, region.playerFlags(),
                mask -> {
                    gateway.setGlobalPlayerFlags(regionId, mask);
                    player.sendMessage(config.text("messages.flags-updated"));
                    openFlagsChooser(player, regionId);
                });
    }


    public void openMemberFlags(Player player, long regionId, UUID memberId) {
        MemberView member = guardMember(player, regionId, memberId);
        if (member == null) {
            return;
        }
        buildFlagForm(player,
                config.apply(config.text("flags.member-title"), Map.of("player", member.playerName())),
                FlagDomain.PLAYER_FLAGS, member.playerFlags(),
                mask -> {
                    gateway.setMemberPlayerFlags(regionId, memberId, mask);
                    player.sendMessage(config.text("messages.flags-updated"));
                    navigate(new BukkitFormPlayer(player),
                            "hs_player_info:" + HomesteadActionPayloads.regionMember(regionId, memberId));
                });
    }

    public void openMemberControlFlags(Player player, long regionId, UUID memberId) {
        MemberView member = guardMember(player, regionId, memberId);
        if (member == null) {
            return;
        }
        buildFlagForm(player,
                config.apply(config.text("flags.control-title"), Map.of("player", member.playerName())),
                FlagDomain.CONTROL_FLAGS, member.controlFlags(),
                mask -> {
                    gateway.setMemberControlFlags(regionId, memberId, mask);
                    player.sendMessage(config.text("messages.flags-updated"));
                    navigate(new BukkitFormPlayer(player),
                            "hs_player_info:" + HomesteadActionPayloads.regionMember(regionId, memberId));
                });
    }


    public void openSubAreaFlags(Player player, long subAreaId) {
        if (!ensureAvailable(player)) {
            return;
        }
        Optional<SubAreaView> subArea = gateway.subArea(subAreaId);
        if (subArea.isEmpty()) {
            player.sendMessage(config.text("messages.sub-area-not-found"));
            return;
        }
        SubAreaView view = subArea.get();
        if (!canManageSubArea(player, view.regionId())) {
            player.sendMessage(config.text("messages.no-permission"));
            return;
        }
        buildFlagForm(player,
                config.apply(config.text("flags.subarea-title"), Map.of("name", view.name())),
                FlagDomain.PLAYER_FLAGS, view.playerFlags(),
                mask -> {
                    gateway.setSubAreaFlags(subAreaId, mask);
                    player.sendMessage(config.text("messages.flags-updated"));
                    navigate(new BukkitFormPlayer(player), "hs_subarea_menu:" + subAreaId);
                });
    }

    public void openSubAreaMemberFlags(Player player, long subAreaId, UUID memberId) {
        if (!ensureAvailable(player)) {
            return;
        }
        Optional<SubAreaView> subArea = gateway.subArea(subAreaId);
        if (subArea.isEmpty()) {
            player.sendMessage(config.text("messages.sub-area-not-found"));
            return;
        }
        SubAreaView view = subArea.get();
        if (!canManageSubArea(player, view.regionId())) {
            player.sendMessage(config.text("messages.no-permission"));
            return;
        }
        MemberView member = null;
        for (MemberView candidate : gateway.subAreaMembers(subAreaId)) {
            if (candidate.playerId() != null && candidate.playerId().equals(memberId)) {
                member = candidate;
                break;
            }
        }
        if (member == null) {
            player.sendMessage(config.text("messages.member-not-found"));
            return;
        }
        String playerName = member.playerName();
        buildFlagForm(player,
                config.apply(config.text("flags.subarea-member-title"), Map.of("player", playerName)),
                FlagDomain.PLAYER_FLAGS, member.playerFlags(),
                mask -> {
                    gateway.setSubAreaMemberFlags(subAreaId, memberId, mask);
                    player.sendMessage(config.text("messages.flags-updated"));
                    navigate(new BukkitFormPlayer(player),
                            "hs_subarea_member:" + HomesteadActionPayloads.regionMember(subAreaId, memberId));
                });
    }

    private boolean canManageSubArea(Player player, long regionId) {
        return player.hasPermission(BedrockRegionService.ADMIN_PERMISSION)
                || gateway.isOwner(regionId, player)
                || gateway.hasControlFlag(regionId, player, "MANAGE_SUBAREAS");
    }


    private void buildFlagForm(Player player, String title, FlagDomain domain, long currentMask, LongConsumer persist) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) {
            return;
        }
        List<String> names = gateway.flagNames(domain);
        BedrockGUIApi.CustomFormBuilder form = api.createCustomForm(title);
        for (String name : names) {
            long bit = gateway.flagValue(domain, name);
            form.toggle(flagLabel(name), (currentMask & bit) != 0);
        }
        form.onSubmit(results -> {
            long newMask = 0L;
            for (String name : names) {
                Object value = results.get(componentName(flagLabel(name)));
                if (value instanceof Boolean enabled && enabled) {
                    newMask |= gateway.flagValue(domain, name);
                }
            }
            persist.accept(newMask);
        });
        form.send(new BukkitFormPlayer(player));
    }

    private String flagLabel(String flagName) {
        return config.apply(config.text("flags.label"), Map.of("flag", prettify(flagName)));
    }


    private RegionView guard(Player player, long regionId, String controlFlag) {
        if (!ensureAvailable(player)) {
            return null;
        }
        RegionView region = requireRegion(player, regionId);
        if (region == null) {
            return null;
        }
        if (!canSet(player, regionId, controlFlag)) {
            player.sendMessage(config.text("messages.no-permission"));
            return null;
        }
        return region;
    }

    private MemberView guardMember(Player player, long regionId, UUID memberId) {
        if (!ensureAvailable(player) || requireRegion(player, regionId) == null) {
            return null;
        }
        if (!canSet(player, regionId, SET_MEMBER_FLAGS)) {
            player.sendMessage(config.text("messages.no-permission"));
            return null;
        }
        for (MemberView member : gateway.membersOf(regionId)) {
            if (member.playerId() != null && member.playerId().equals(memberId)) {
                return member;
            }
        }
        player.sendMessage(config.text("messages.member-not-found"));
        return null;
    }

    private boolean canSet(Player player, long regionId, String controlFlag) {
        return player.hasPermission(BedrockRegionService.ADMIN_PERMISSION)
                || gateway.isOwner(regionId, player)
                || gateway.hasControlFlag(regionId, player, controlFlag);
    }

    private void navigate(it.pintux.life.common.utils.FormPlayer fp, String actionString) {
        BedrockGUIApi api;
        try {
            api = BedrockGUIApi.getInstance();
        } catch (IllegalStateException e) {
            return;
        }
        api.executeActionString(fp, actionString,
                ActionSystem.ActionContext.builder().menuName("flags").formType("homestead").build());
    }

    private RegionView requireRegion(Player player, long regionId) {
        Optional<RegionView> region = gateway.region(regionId);
        if (region.isEmpty()) {
            player.sendMessage(config.text("messages.region-not-found"));
            return null;
        }
        return region.get();
    }

    private boolean ensureAvailable(Player player) {
        if (!gateway.isAvailable()) {
            player.sendMessage(config.text("messages.homestead-unavailable"));
            return false;
        }
        return true;
    }

    private BedrockGUIApi requireApi(Player player) {
        try {
            return BedrockGUIApi.getInstance();
        } catch (IllegalStateException e) {
            player.sendMessage(config.text("messages.homestead-unavailable"));
            return null;
        }
    }

    private static String componentName(String text) {
        return text.toLowerCase().replaceAll("\\s+", "_");
    }

    private static String prettify(String flagName) {
        String[] parts = flagName.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
