package it.pintux.life.homesteadaddon.service;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.homesteadaddon.config.HomesteadAddonConfiguration;
import it.pintux.life.homesteadaddon.gateway.HomesteadGateway;
import it.pintux.life.homesteadaddon.model.BanView;
import it.pintux.life.homesteadaddon.model.InviteView;
import it.pintux.life.homesteadaddon.model.MemberView;
import it.pintux.life.homesteadaddon.model.RegionView;
import it.pintux.life.homesteadaddon.util.BukkitFormPlayer;
import it.pintux.life.homesteadaddon.util.Formatting;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class BedrockMemberService {
    private final HomesteadAddonConfiguration config;
    private final HomesteadGateway gateway;

    public BedrockMemberService(HomesteadAddonConfiguration config, HomesteadGateway gateway) {
        this.config = config;
        this.gateway = gateway;
    }


    public void openPlayersManagement(Player player, long regionId) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        RegionView region = requireRegion(player, regionId);
        if (region == null) {
            return;
        }
        if (!canView(player, regionId)) {
            player.sendMessage(config.text("players.no-access"));
            return;
        }
        boolean manage = canManage(player, regionId);

        Map<String, String> ph = Map.of("name", region.name());
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(config.apply(config.text("players.hub-title"), ph));
        form.content(config.text("players.hub-content"));

        form.button(config.apply(config.text("players.button-members"),
                        Map.of("count", String.valueOf(gateway.membersOf(regionId).size()))),
                fp -> openMembersList(player, regionId, 1));

        if (manage) {
            form.button(config.apply(config.text("players.button-invited"),
                            Map.of("count", String.valueOf(gateway.invitesOf(regionId).size()))),
                    fp -> openInvitesList(player, regionId, 1));
            form.button(config.apply(config.text("players.button-banned"),
                            Map.of("count", String.valueOf(gateway.bansOf(regionId).size()))),
                    fp -> openBansList(player, regionId, 1));
            form.button(config.text("players.button-invite"), fp -> showInviteForm(player, regionId));
        }

        form.button(config.text("common.back-button"), fp -> backToRegionMenu(api, fp, regionId));
        form.send(new BukkitFormPlayer(player));
    }


    public void openMembersList(Player player, long regionId, int page) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        RegionView region = requireRegion(player, regionId);
        if (region == null || !canView(player, regionId)) {
            if (region != null) player.sendMessage(config.text("players.no-access"));
            return;
        }
        List<MemberView> members = gateway.membersOf(regionId);

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(
                config.apply(config.text("players.members-title"), Map.of("name", region.name())));
        if (members.isEmpty()) {
            form.content(config.text("players.members-empty"));
        }

        Pagination pagination = new Pagination(members.size(), page);
        for (int i = pagination.start; i < pagination.end; i++) {
            MemberView member = members.get(i);
            form.button(config.apply(config.text("players.member-button"), Map.of(
                            "player", member.playerName(),
                            "joined", Formatting.date(member.joinedAt()))),
                    fp -> openPlayerInfo(player, regionId, member.playerId()));
        }
        pagination.addNav(form, p -> openMembersList(player, regionId, p));
        form.button(config.text("common.back-button"), fp -> openPlayersManagement(player, regionId));
        form.send(new BukkitFormPlayer(player));
    }


    public void openPlayerInfo(Player player, long regionId, UUID memberId) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        RegionView region = requireRegion(player, regionId);
        if (region == null || !canView(player, regionId)) {
            if (region != null) player.sendMessage(config.text("players.no-access"));
            return;
        }
        MemberView member = findMember(regionId, memberId);
        if (member == null) {
            player.sendMessage(config.text("messages.member-not-found"));
            return;
        }

        Map<String, String> ph = Map.of(
                "player", member.playerName(),
                "joined", Formatting.date(member.joinedAt()));
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(config.apply(config.text("players.info-title"), ph));
        form.content(config.apply(config.text("players.info-content"), ph));

        if (canManage(player, regionId)) {
            form.button(config.text("players.button-member-flags"), fp ->
                    api.executeActionString(fp, "hs_member_flags:"
                            + it.pintux.life.homesteadaddon.util.HomesteadActionPayloads.regionMember(regionId, member.playerId()),
                            menuContext()));
            form.button(config.text("players.button-control-flags"), fp ->
                    api.executeActionString(fp, "hs_control_flags:"
                            + it.pintux.life.homesteadaddon.util.HomesteadActionPayloads.regionMember(regionId, member.playerId()),
                            menuContext()));
            form.button(config.text("players.button-kick"), fp -> confirmKick(player, regionId, member));
        }
        form.button(config.text("common.back-button"), fp -> openMembersList(player, regionId, 1));
        form.send(new BukkitFormPlayer(player));
    }

    private static ActionSystem.ActionContext menuContext() {
        return ActionSystem.ActionContext.builder().menuName("player-info").formType("homestead").build();
    }


    public void openInvitesList(Player player, long regionId, int page) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        RegionView region = requireRegion(player, regionId);
        if (region == null || !requireManage(player, regionId)) {
            return;
        }
        List<InviteView> invites = gateway.invitesOf(regionId);

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(
                config.apply(config.text("players.invited-title"), Map.of("name", region.name())));
        if (invites.isEmpty()) {
            form.content(config.text("players.invited-empty"));
        }

        Pagination pagination = new Pagination(invites.size(), page);
        for (int i = pagination.start; i < pagination.end; i++) {
            InviteView invite = invites.get(i);
            form.button(config.apply(config.text("players.invite-entry-button"), Map.of(
                            "player", invite.playerName(),
                            "invited", Formatting.date(invite.invitedAt()))),
                    fp -> confirmRevoke(player, regionId, invite));
        }
        pagination.addNav(form, p -> openInvitesList(player, regionId, p));
        form.button(config.text("common.back-button"), fp -> openPlayersManagement(player, regionId));
        form.send(new BukkitFormPlayer(player));
    }


    public void openBansList(Player player, long regionId, int page) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        RegionView region = requireRegion(player, regionId);
        if (region == null || !requireManage(player, regionId)) {
            return;
        }
        List<BanView> bans = gateway.bansOf(regionId);

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(
                config.apply(config.text("players.banned-title"), Map.of("name", region.name())));
        if (bans.isEmpty()) {
            form.content(config.text("players.banned-empty"));
        }

        Pagination pagination = new Pagination(bans.size(), page);
        for (int i = pagination.start; i < pagination.end; i++) {
            BanView ban = bans.get(i);
            String reason = ban.reason() == null || ban.reason().isBlank() ? "-" : ban.reason();
            form.button(config.apply(config.text("players.ban-entry-button"), Map.of(
                            "player", ban.playerName(),
                            "reason", reason)),
                    fp -> confirmUnban(player, regionId, ban));
        }
        pagination.addNav(form, p -> openBansList(player, regionId, p));
        form.button(config.text("players.button-ban"), fp -> showBanForm(player, regionId, null));
        form.button(config.text("common.back-button"), fp -> openPlayersManagement(player, regionId));
        form.send(new BukkitFormPlayer(player));
    }


    public void showInviteForm(Player player, long regionId) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player) || !requireManage(player, regionId)) {
            return;
        }
        String label = config.text("players.invite-input-label");
        api.createCustomForm(config.text("players.invite-input-title"))
                .input(label, config.text("players.invite-input-placeholder"), "")
                .onSubmit(results -> {
                    String name = string(results, label);
                    if (name.isBlank()) {
                        player.sendMessage(config.text("messages.invite-invalid"));
                        return;
                    }
                    if (gateway.invitePlayer(regionId, name)) {
                        player.sendMessage(config.apply(config.text("messages.invite-success"), Map.of("player", name)));
                    } else {
                        player.sendMessage(config.text("messages.invite-failed"));
                    }
                })
                .send(new BukkitFormPlayer(player));
    }

    private void showBanForm(Player player, long regionId, String playerName) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player) || !requireManage(player, regionId)) {
            return;
        }
        String nameLabel = config.text("players.ban-input-name-label");
        String reasonLabel = config.text("players.ban-input-reason-label");
        api.createCustomForm(config.text("players.ban-input-title"))
                .input(nameLabel, config.text("players.ban-input-name-placeholder"), playerName == null ? "" : playerName)
                .input(reasonLabel, "", "")
                .onSubmit(results -> {
                    String name = string(results, nameLabel);
                    String reason = string(results, reasonLabel);
                    if (name.isBlank()) {
                        player.sendMessage(config.text("messages.invite-invalid"));
                        return;
                    }
                    if (gateway.banPlayer(regionId, name, reason)) {
                        player.sendMessage(config.apply(config.text("messages.ban-success"), Map.of("player", name)));
                    } else {
                        player.sendMessage(config.text("messages.ban-failed"));
                    }
                })
                .send(new BukkitFormPlayer(player));
    }


    private void confirmKick(Player player, long regionId, MemberView member) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) {
            return;
        }
        Map<String, String> ph = Map.of("player", member.playerName());
        api.createModalForm(config.apply(config.text("players.kick-title"), ph),
                        config.apply(config.text("players.kick-content"), ph))
                .button1(config.text("players.button-kick"), fp -> {
                    if (canManage(player, regionId) && gateway.kickMember(regionId, member.playerId())) {
                        player.sendMessage(config.apply(config.text("messages.kick-success"), ph));
                    }
                    openMembersList(player, regionId, 1);
                })
                .button2(config.text("players.confirm-no"), fp -> openPlayerInfo(player, regionId, member.playerId()))
                .send(new BukkitFormPlayer(player));
    }

    private void confirmRevoke(Player player, long regionId, InviteView invite) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) {
            return;
        }
        Map<String, String> ph = Map.of("player", invite.playerName());
        api.createModalForm(config.text("players.revoke-title"),
                        config.apply(config.text("players.revoke-content"), ph))
                .button1(config.text("players.button-revoke"), fp -> {
                    if (canManage(player, regionId) && gateway.revokeInvite(invite.inviteId())) {
                        player.sendMessage(config.text("messages.revoke-success"));
                    }
                    openInvitesList(player, regionId, 1);
                })
                .button2(config.text("players.confirm-no"), fp -> openInvitesList(player, regionId, 1))
                .send(new BukkitFormPlayer(player));
    }

    private void confirmUnban(Player player, long regionId, BanView ban) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) {
            return;
        }
        Map<String, String> ph = Map.of("player", ban.playerName());
        api.createModalForm(config.apply(config.text("players.unban-title"), ph),
                        config.apply(config.text("players.unban-content"), ph))
                .button1(config.text("players.button-unban"), fp -> {
                    if (canManage(player, regionId) && gateway.unbanPlayer(regionId, ban.playerId())) {
                        player.sendMessage(config.apply(config.text("messages.unban-success"), ph));
                    }
                    openBansList(player, regionId, 1);
                })
                .button2(config.text("players.confirm-no"), fp -> openBansList(player, regionId, 1))
                .send(new BukkitFormPlayer(player));
    }


    private MemberView findMember(long regionId, UUID memberId) {
        for (MemberView member : gateway.membersOf(regionId)) {
            if (member.playerId() != null && member.playerId().equals(memberId)) {
                return member;
            }
        }
        return null;
    }

    private boolean canView(Player player, long regionId) {
        return player.hasPermission(BedrockRegionService.ADMIN_PERMISSION)
                || gateway.isOwner(regionId, player)
                || gateway.isMember(regionId, player);
    }

    private boolean canManage(Player player, long regionId) {
        return player.hasPermission(BedrockRegionService.ADMIN_PERMISSION)
                || gateway.canManageMembers(regionId, player);
    }

    private boolean requireManage(Player player, long regionId) {
        if (!canManage(player, regionId)) {
            player.sendMessage(config.text("players.no-access"));
            return false;
        }
        return true;
    }

    private void backToRegionMenu(BedrockGUIApi api, it.pintux.life.common.utils.FormPlayer fp, long regionId) {
        api.executeActionString(fp, "hs_region_menu:" + regionId, ActionSystem.ActionContext.builder()
                .menuName("players-hub").formType("homestead").build());
    }

    private String string(Map<String, Object> results, String label) {
        Object value = results.get(componentName(label));
        return value == null ? "" : value.toString().trim();
    }

    private static String componentName(String text) {
        return text.toLowerCase().replaceAll("\\s+", "_");
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
