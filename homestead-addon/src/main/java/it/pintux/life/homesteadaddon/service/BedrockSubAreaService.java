package it.pintux.life.homesteadaddon.service;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.homesteadaddon.config.HomesteadAddonConfiguration;
import it.pintux.life.homesteadaddon.gateway.HomesteadGateway;
import it.pintux.life.homesteadaddon.model.MemberView;
import it.pintux.life.homesteadaddon.model.RegionView;
import it.pintux.life.homesteadaddon.model.SubAreaView;
import it.pintux.life.homesteadaddon.util.BukkitFormPlayer;
import it.pintux.life.homesteadaddon.util.Formatting;
import it.pintux.life.homesteadaddon.util.HomesteadActionPayloads;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class BedrockSubAreaService {
    private final HomesteadAddonConfiguration config;
    private final HomesteadGateway gateway;

    public BedrockSubAreaService(HomesteadAddonConfiguration config, HomesteadGateway gateway) {
        this.config = config;
        this.gateway = gateway;
    }


    public void openSubAreasList(Player player, long regionId, int page) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        RegionView region = requireRegion(player, regionId);
        if (region == null) {
            return;
        }
        List<SubAreaView> subAreas = gateway.subAreasOf(regionId);

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(
                config.apply(config.text("subareas.list-title"), Map.of("name", region.name())));
        if (subAreas.isEmpty()) {
            form.content(config.text("subareas.list-empty"));
        } else {
            form.content(config.text("subareas.list-content"));
        }

        Pagination pagination = new Pagination(subAreas.size(), page);
        for (int i = pagination.start; i < pagination.end; i++) {
            SubAreaView subArea = subAreas.get(i);
            form.button(config.apply(config.text("subareas.entry-button"), Map.of(
                            "name", subArea.name(),
                            "volume", String.valueOf(subArea.volume()),
                            "created", Formatting.date(subArea.createdAt()))),
                    fp -> openSubAreaMenu(player, subArea.id()));
        }
        pagination.addNav(form, p -> openSubAreasList(player, regionId, p));
        form.button(config.text("common.back-button"), fp -> navigate(fp, "hs_region_menu:" + regionId));
        form.send(new BukkitFormPlayer(player));
    }


    public void openSubAreaMenu(Player player, long subAreaId) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        SubAreaView subArea = requireSubArea(player, subAreaId);
        if (subArea == null) {
            return;
        }
        boolean manage = canManage(player, subArea.regionId());

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(
                config.apply(config.text("subareas.menu-title"), Map.of("name", subArea.name())));
        form.content(config.apply(config.text("subareas.menu-content"), Map.of(
                "name", subArea.name(),
                "volume", String.valueOf(subArea.volume()),
                "members", String.valueOf(gateway.subAreaMembers(subAreaId).size()))));

        form.button(config.text("subareas.button-members"), fp -> openSubAreaMembers(player, subAreaId, 1));
        form.button(config.text("subareas.button-flags"), fp -> navigate(fp, "hs_subarea_flags:" + subAreaId));
        if (manage) {
            form.button(config.text("subareas.button-rename"), fp -> showRenameForm(player, subAreaId));
            if (subArea.rented()) {
                form.button(config.text("subareas.button-end-rent"), fp -> endRent(player, subAreaId));
            }
            form.button(config.text("subareas.button-delete"), fp -> confirmDelete(player, subArea));
        }
        form.button(config.text("common.back-button"),
                fp -> openSubAreasList(player, subArea.regionId(), 1));
        form.send(new BukkitFormPlayer(player));
    }

    private void showRenameForm(Player player, long subAreaId) {
        BedrockGUIApi api = requireApi(player);
        SubAreaView subArea = requireSubArea(player, subAreaId);
        if (api == null || subArea == null || !requireManage(player, subArea.regionId())) {
            return;
        }
        String label = config.text("subareas.rename-label");
        api.createCustomForm(config.text("subareas.rename-title"))
                .input(label, config.text("subareas.rename-placeholder"), subArea.name())
                .onSubmit(results -> {
                    String name = string(results, label);
                    if (name.isBlank()) {
                        player.sendMessage(config.text("subareas.rename-invalid"));
                        return;
                    }
                    if (gateway.renameSubArea(subAreaId, name)) {
                        player.sendMessage(config.apply(config.text("subareas.rename-success"), Map.of("name", name)));
                    }
                    openSubAreaMenu(player, subAreaId);
                })
                .send(new BukkitFormPlayer(player));
    }

    private void endRent(Player player, long subAreaId) {
        SubAreaView subArea = requireSubArea(player, subAreaId);
        if (subArea == null || !requireManage(player, subArea.regionId())) {
            return;
        }
        gateway.endSubAreaRent(subAreaId);
        player.sendMessage(config.text("subareas.end-rent-success"));
        openSubAreaMenu(player, subAreaId);
    }

    private void confirmDelete(Player player, SubAreaView subArea) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) {
            return;
        }
        Map<String, String> ph = Map.of("name", subArea.name());
        api.createModalForm(config.apply(config.text("subareas.delete-title"), ph),
                        config.apply(config.text("subareas.delete-content"), ph))
                .button1(config.text("subareas.button-delete"), fp -> {
                    if (canManage(player, subArea.regionId()) && gateway.deleteSubArea(subArea.id())) {
                        player.sendMessage(config.apply(config.text("subareas.delete-success"), ph));
                    }
                    openSubAreasList(player, subArea.regionId(), 1);
                })
                .button2(config.text("subareas.confirm-no"), fp -> openSubAreaMenu(player, subArea.id()))
                .send(new BukkitFormPlayer(player));
    }


    public void openSubAreaMembers(Player player, long subAreaId, int page) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        SubAreaView subArea = requireSubArea(player, subAreaId);
        if (subArea == null) {
            return;
        }
        List<MemberView> members = gateway.subAreaMembers(subAreaId);
        boolean manage = canManage(player, subArea.regionId());

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(
                config.apply(config.text("subareas.members-title"), Map.of("name", subArea.name())));
        if (members.isEmpty()) {
            form.content(config.text("subareas.members-empty"));
        }

        Pagination pagination = new Pagination(members.size(), page);
        for (int i = pagination.start; i < pagination.end; i++) {
            MemberView member = members.get(i);
            form.button(config.apply(config.text("subareas.member-button"), Map.of("player", member.playerName())),
                    fp -> openSubAreaMemberActions(player, subAreaId, member.playerId()));
        }
        pagination.addNav(form, p -> openSubAreaMembers(player, subAreaId, p));
        if (manage) {
            form.button(config.text("subareas.button-add-member"), fp -> showAddMemberForm(player, subAreaId));
        }
        form.button(config.text("common.back-button"), fp -> openSubAreaMenu(player, subAreaId));
        form.send(new BukkitFormPlayer(player));
    }

    public void openSubAreaMemberActions(Player player, long subAreaId, UUID memberId) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        SubAreaView subArea = requireSubArea(player, subAreaId);
        if (subArea == null) {
            return;
        }
        MemberView member = findMember(subAreaId, memberId);
        if (member == null) {
            player.sendMessage(config.text("messages.member-not-found"));
            return;
        }
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(
                config.apply(config.text("subareas.member-actions-title"), Map.of("player", member.playerName())));
        form.content(config.text("subareas.member-actions-content"));
        form.button(config.text("subareas.button-member-flags"),
                fp -> navigate(fp, "hs_subarea_member_flags:" + HomesteadActionPayloads.regionMember(subAreaId, memberId)));
        if (canManage(player, subArea.regionId())) {
            form.button(config.text("subareas.button-remove-member"), fp -> confirmRemoveMember(player, subAreaId, member));
        }
        form.button(config.text("common.back-button"), fp -> openSubAreaMembers(player, subAreaId, 1));
        form.send(new BukkitFormPlayer(player));
    }

    private void showAddMemberForm(Player player, long subAreaId) {
        BedrockGUIApi api = requireApi(player);
        SubAreaView subArea = requireSubArea(player, subAreaId);
        if (api == null || subArea == null || !requireManage(player, subArea.regionId())) {
            return;
        }
        String label = config.text("subareas.add-member-label");
        api.createCustomForm(config.text("subareas.add-member-title"))
                .input(label, config.text("subareas.add-member-placeholder"), "")
                .onSubmit(results -> {
                    String name = string(results, label);
                    if (name.isBlank()) {
                        player.sendMessage(config.text("messages.invite-invalid"));
                        return;
                    }
                    if (gateway.addSubAreaMember(subAreaId, name)) {
                        player.sendMessage(config.apply(config.text("subareas.add-member-success"), Map.of("player", name)));
                    } else {
                        player.sendMessage(config.text("subareas.add-member-failed"));
                    }
                    openSubAreaMembers(player, subAreaId, 1);
                })
                .send(new BukkitFormPlayer(player));
    }

    private void confirmRemoveMember(Player player, long subAreaId, MemberView member) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) {
            return;
        }
        Map<String, String> ph = Map.of("player", member.playerName());
        api.createModalForm(config.apply(config.text("subareas.remove-member-title"), ph),
                        config.apply(config.text("subareas.remove-member-content"), ph))
                .button1(config.text("subareas.button-remove-member"), fp -> {
                    SubAreaView subArea = gateway.subArea(subAreaId).orElse(null);
                    if (subArea != null && canManage(player, subArea.regionId())
                            && gateway.removeSubAreaMember(subAreaId, member.playerId())) {
                        player.sendMessage(config.apply(config.text("subareas.remove-member-success"), ph));
                    }
                    openSubAreaMembers(player, subAreaId, 1);
                })
                .button2(config.text("subareas.confirm-no"), fp -> openSubAreaMemberActions(player, subAreaId, member.playerId()))
                .send(new BukkitFormPlayer(player));
    }


    private MemberView findMember(long subAreaId, UUID memberId) {
        for (MemberView member : gateway.subAreaMembers(subAreaId)) {
            if (member.playerId() != null && member.playerId().equals(memberId)) {
                return member;
            }
        }
        return null;
    }

    private boolean canManage(Player player, long regionId) {
        return player.hasPermission(BedrockRegionService.ADMIN_PERMISSION)
                || gateway.canManageSubAreas(regionId, player);
    }

    private boolean requireManage(Player player, long regionId) {
        if (!canManage(player, regionId)) {
            player.sendMessage(config.text("messages.no-permission"));
            return false;
        }
        return true;
    }

    private void navigate(FormPlayer fp, String actionString) {
        BedrockGUIApi api;
        try {
            api = BedrockGUIApi.getInstance();
        } catch (IllegalStateException e) {
            return;
        }
        api.executeActionString(fp, actionString,
                ActionSystem.ActionContext.builder().menuName("subareas").formType("homestead").build());
    }

    private String string(Map<String, Object> results, String label) {
        Object value = results.get(label.toLowerCase().replaceAll("\\s+", "_"));
        return value == null ? "" : value.toString().trim();
    }

    private SubAreaView requireSubArea(Player player, long subAreaId) {
        Optional<SubAreaView> subArea = gateway.subArea(subAreaId);
        if (subArea.isEmpty()) {
            player.sendMessage(config.text("messages.sub-area-not-found"));
            return null;
        }
        return subArea.get();
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

    private final class Pagination {
        final int current;
        final int totalPages;
        final int start;
        final int end;

        Pagination(int size, int page) {
            int perPage = config.itemsPerPage();
            this.totalPages = Math.max(1, (int) Math.ceil((double) size / perPage));
            this.current = Math.max(1, Math.min(page, totalPages));
            this.start = (current - 1) * perPage;
            this.end = Math.min(start + perPage, size);
        }

        void addNav(BedrockGUIApi.SimpleFormBuilder form, java.util.function.IntConsumer open) {
            if (current > 1) {
                form.button(config.text("common.previous-button"), fp -> open.accept(current - 1));
            }
            if (current < totalPages) {
                form.button(config.text("common.next-button"), fp -> open.accept(current + 1));
            }
        }
    }
}
