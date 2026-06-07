package it.pintux.life.bedwarsaddon.service;

import it.pintux.life.bedwarsaddon.api.BedrockPlayerDetector;
import it.pintux.life.bedwarsaddon.config.BedwarsAddonConfiguration;
import it.pintux.life.bedwarsaddon.menu.ArenaMenuModel;
import it.pintux.life.bedwarsaddon.menu.MenuButton;
import it.pintux.life.bedwarsaddon.model.ArenaInfo;
import it.pintux.life.bedwarsaddon.util.BedrockSoundFeedback;
import it.pintux.life.bedwarsaddon.util.BukkitFormPlayer;
import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.api.BedrockGUIApi;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Map;

public final class BedrockArenaService {
    private final BedwarsAddonConfiguration config;
    private final ArenaCatalogService catalog;
    private final ArenaMenuModel menuModel;
    private final BedrockPlayerDetector detector;
    private final BedrockSoundFeedback sound;

    public BedrockArenaService(BedwarsAddonConfiguration config, ArenaCatalogService catalog,
                               BedrockPlayerDetector detector, BedrockSoundFeedback sound) {
        this.config = config;
        this.catalog = catalog;
        this.detector = detector;
        this.sound = sound;
        this.menuModel = new ArenaMenuModel(config);
    }

    public boolean shouldHandle(Player player) {
        return player != null && detector.isBedrockPlayer(player);
    }

    public boolean ownsInventory(Inventory inventory) {
        return catalog.ownsInventory(inventory);
    }

    public void openMain(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!catalog.isReady()) { player.sendMessage(config.arenaProviderUnavailable()); return; }

        List<ArenaInfo> arenas = catalog.getArenas();
        if (arenas.isEmpty()) { player.sendMessage(config.arenaNoArenas()); return; }

        List<MenuButton> buttons = menuModel.arenaButtons(arenas);
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(config.arenaTitle());
        form.content(config.arenaContent());
        for (MenuButton button : buttons) {
            String action = button.actionString();
            form.button(button.label(), fp -> api.executeActionString(fp, action, ctx("arena-main")));
        }
        sound.playFormOpen(player);
        form.send(new BukkitFormPlayer(player));
    }

    public void join(Player player, String arenaName) {
        if (!catalog.isReady()) { player.sendMessage(config.arenaProviderUnavailable()); return; }
        boolean ok = catalog.join(player, arenaName);
        if (!ok) {
            player.sendMessage(config.render(config.arenaJoinFailed(), Map.of("arena", arenaName)));
        }
        // On success BedWars moves the player into the arena; nothing else to do.
    }

    private BedrockGUIApi requireApi(Player player) {
        try {
            return BedrockGUIApi.getInstance();
        } catch (IllegalStateException e) {
            player.sendMessage(config.arenaProviderUnavailable());
            return null;
        }
    }

    private ActionSystem.ActionContext ctx(String source) {
        return ActionSystem.ActionContext.builder()
                .menuName(source)
                .formType("bedrock-bw-arena")
                .build();
    }
}
