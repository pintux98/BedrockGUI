package it.pintux.life.bedwarsaddon.menu;

import it.pintux.life.bedwarsaddon.config.BedwarsAddonConfiguration;
import it.pintux.life.bedwarsaddon.model.UpgradeContent;
import it.pintux.life.bedwarsaddon.util.BedwarsActionPayloads;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Pure: builds upgrade form buttons from DTOs + config. No Bukkit / no BedrockGUIApi. */
public final class UpgradeMenuModel {
    private final BedwarsAddonConfiguration config;

    public UpgradeMenuModel(BedwarsAddonConfiguration config) {
        this.config = config;
    }

    public List<MenuButton> upgradeButtons(List<UpgradeContent> upgrades) {
        List<MenuButton> buttons = new ArrayList<>();
        for (UpgradeContent upgrade : upgrades) {
            String label = config.render(config.upgradeButton(), Map.of("upgrade", upgrade.name()));
            buttons.add(new MenuButton(label, "bw_upgrade_buy:" + BedwarsActionPayloads.encode(upgrade.id())));
        }
        buttons.add(new MenuButton(config.upgradeCloseButton(), "bw_upgrade_close:"));
        return buttons;
    }
}
