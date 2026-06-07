package it.pintux.life.bedwarsaddon.service;

import it.pintux.life.bedwarsaddon.api.BedrockPlayerDetector;
import it.pintux.life.bedwarsaddon.config.BedwarsAddonConfiguration;
import it.pintux.life.bedwarsaddon.menu.MenuButton;
import it.pintux.life.bedwarsaddon.menu.PartyMenuModel;
import it.pintux.life.bedwarsaddon.model.PartyInfo;
import it.pintux.life.bedwarsaddon.util.BedrockSoundFeedback;
import it.pintux.life.bedwarsaddon.util.BukkitFormPlayer;
import it.pintux.life.bedwarsaddon.util.FormPlayerResolver;
import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.api.BedrockGUIApi;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BedrockPartyService {
    private final BedwarsAddonConfiguration config;
    private final PartyCatalogService catalog;
    private final PartyMenuModel menuModel;
    private final BedrockPlayerDetector detector;
    private final BedrockSoundFeedback sound;

    public BedrockPartyService(BedwarsAddonConfiguration config, PartyCatalogService catalog,
                               BedrockPlayerDetector detector, BedrockSoundFeedback sound) {
        this.config = config;
        this.catalog = catalog;
        this.detector = detector;
        this.sound = sound;
        this.menuModel = new PartyMenuModel(config);
    }

    public boolean shouldHandle(Player player) {
        return player != null && detector.isBedrockPlayer(player);
    }

    public void openMain(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!catalog.isReady()) { player.sendMessage(config.partyProviderUnavailable()); return; }

        PartyInfo info = catalog.getParty(player);
        String content = info.hasParty()
                ? config.render(config.partyContent(), Map.of(
                        "owner", info.ownerName(),
                        "size", String.valueOf(info.members().size())))
                : config.partyNoParty();

        List<MenuButton> buttons = menuModel.mainButtons(info);
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(config.partyTitle());
        form.content(content);
        for (MenuButton button : buttons) {
            String action = button.actionString();
            form.button(button.label(), fp -> api.executeActionString(fp, action, ctx()));
        }
        sound.playFormOpen(player);
        form.send(new BukkitFormPlayer(player));
    }

    public void showAddInput(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!catalog.isReady()) { player.sendMessage(config.partyProviderUnavailable()); return; }

        api.createCustomForm(config.partyAddInputTitle())
                .input(config.partyAddInputLabel(), "", "")
                .onSubmit((fp, results) -> {
                    Player bp = FormPlayerResolver.resolve(fp);
                    if (bp == null) return;
                    String name = results.isEmpty() ? "" : String.valueOf(results.values().iterator().next());
                    if (name == null || name.isBlank()) { openMain(bp); return; }
                    name = name.trim();
                    boolean ok = catalog.add(bp, name);
                    bp.sendMessage(config.render(ok ? config.partyAdded() : config.partyPlayerNotFound(),
                            Map.of("player", name)));
                    openMain(bp);
                })
                .send(new BukkitFormPlayer(player));
    }

    public void leave(Player player) {
        if (!catalog.isReady()) { player.sendMessage(config.partyProviderUnavailable()); return; }
        catalog.leave(player);
        openMain(player);
    }

    public void disband(Player player) {
        if (!catalog.isReady()) { player.sendMessage(config.partyProviderUnavailable()); return; }
        PartyInfo info = catalog.getParty(player);
        if (!info.owner()) { player.sendMessage(config.partyNotOwner()); return; }
        catalog.disband(player);
        openMain(player);
    }

    public void openKick(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!catalog.isReady()) { player.sendMessage(config.partyProviderUnavailable()); return; }
        PartyInfo info = catalog.getParty(player);
        if (!info.hasParty()) { player.sendMessage(config.partyNoParty()); return; }
        if (!info.owner()) { player.sendMessage(config.partyNotOwner()); return; }

        // Exclude the owner from the kick list.
        List<String> kickable = new ArrayList<>();
        for (String name : info.members()) {
            if (!name.equalsIgnoreCase(info.ownerName())) kickable.add(name);
        }

        List<MenuButton> buttons = menuModel.kickButtons(kickable);
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(config.partyTitle());
        form.content(config.partyKickButton());
        for (MenuButton button : buttons) {
            String action = button.actionString();
            form.button(button.label(), fp -> api.executeActionString(fp, action, ctx()));
        }
        form.send(new BukkitFormPlayer(player));
    }

    public void kick(Player player, String targetName) {
        if (!catalog.isReady()) { player.sendMessage(config.partyProviderUnavailable()); return; }
        boolean ok = catalog.kick(player, targetName);
        if (ok) {
            player.sendMessage(config.render(config.partyKicked(), Map.of("player", targetName)));
        }
        openMain(player);
    }

    private BedrockGUIApi requireApi(Player player) {
        try {
            return BedrockGUIApi.getInstance();
        } catch (IllegalStateException e) {
            player.sendMessage(config.partyProviderUnavailable());
            return null;
        }
    }

    private ActionSystem.ActionContext ctx() {
        return ActionSystem.ActionContext.builder()
                .menuName("party")
                .formType("bedrock-bw-party")
                .build();
    }
}
