package it.pintux.life.bedwarsaddon.menu;

import it.pintux.life.bedwarsaddon.config.BedwarsAddonConfiguration;
import it.pintux.life.bedwarsaddon.model.SpectateTarget;
import it.pintux.life.bedwarsaddon.util.BedwarsActionPayloads;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Pure: builds spectator-teleporter form buttons from DTOs + config. No Bukkit / no BedrockGUIApi. */
public final class SpectatorMenuModel {
    private final BedwarsAddonConfiguration config;

    public SpectatorMenuModel(BedwarsAddonConfiguration config) {
        this.config = config;
    }

    public List<MenuButton> targetButtons(List<SpectateTarget> targets) {
        List<MenuButton> buttons = new ArrayList<>();
        for (SpectateTarget target : targets) {
            String label = config.render(config.spectatorTargetButton(), Map.of("player", target.name()));
            buttons.add(new MenuButton(label, "bw_spec_tp:" + BedwarsActionPayloads.encode(target.uuid())));
        }
        buttons.add(new MenuButton(config.spectatorCloseButton(), "bw_spec_close:"));
        return buttons;
    }
}
