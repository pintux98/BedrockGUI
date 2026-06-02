package it.pintux.life.bedwarsaddon.service;

import it.pintux.life.bedwarsaddon.api.BedrockPlayerDetector;
import it.pintux.life.bedwarsaddon.config.BedwarsAddonConfiguration;
import it.pintux.life.bedwarsaddon.menu.MenuButton;
import it.pintux.life.bedwarsaddon.menu.UpgradeMenuModel;
import it.pintux.life.bedwarsaddon.model.UpgradeContent;
import it.pintux.life.bedwarsaddon.util.BedrockSoundFeedback;
import it.pintux.life.bedwarsaddon.util.BukkitFormPlayer;
import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.api.BedrockGUIApi;
import org.bukkit.entity.Player;

import java.util.List;

public final class BedrockUpgradeService {
    private final BedwarsAddonConfiguration config;
    private final UpgradeCatalogService catalog;
    private final UpgradeMenuModel menuModel;
    private final BedrockPlayerDetector detector;
    private final BedrockSoundFeedback sound;

    public BedrockUpgradeService(BedwarsAddonConfiguration config, UpgradeCatalogService catalog,
                                 BedrockPlayerDetector detector, BedrockSoundFeedback sound) {
        this.config = config;
        this.catalog = catalog;
        this.detector = detector;
        this.sound = sound;
        this.menuModel = new UpgradeMenuModel(config);
    }

    public boolean shouldHandle(Player player) {
        return player != null && detector.isBedrockPlayer(player);
    }

    public boolean isWatching(Player player) {
        return catalog.isWatching(player);
    }

    public void stopWatching(Player player) {
        catalog.stopWatching(player);
    }

    public void openMain(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!catalog.isReady()) { player.sendMessage(config.upgradeProviderUnavailable()); return; }

        List<UpgradeContent> upgrades = catalog.getUpgrades(player);
        List<MenuButton> buttons = menuModel.upgradeButtons(upgrades);
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(config.upgradeTitle());
        form.content(config.upgradeContent());
        for (MenuButton button : buttons) {
            String action = button.actionString();
            form.button(button.label(), fp -> api.executeActionString(fp, action, ctx("upgrade-main")));
        }
        sound.playFormOpen(player);
        form.send(new BukkitFormPlayer(player));
    }

    public void buy(Player player, String upgradeId) {
        if (!catalog.isReady()) { player.sendMessage(config.upgradeProviderUnavailable()); return; }
        // BedWars applies the upgrade and sends its own messages — we add none.
        catalog.purchase(player, upgradeId);
        openMain(player); // refresh tiers/availability
    }

    private BedrockGUIApi requireApi(Player player) {
        try {
            return BedrockGUIApi.getInstance();
        } catch (IllegalStateException e) {
            player.sendMessage(config.upgradeProviderUnavailable());
            return null;
        }
    }

    private ActionSystem.ActionContext ctx(String source) {
        return ActionSystem.ActionContext.builder()
                .menuName(source)
                .formType("bedrock-bw-upgrade")
                .build();
    }
}
