package it.pintux.life.bedwarsaddon.menu;

import it.pintux.life.bedwarsaddon.config.BedwarsAddonConfiguration;
import it.pintux.life.bedwarsaddon.model.PartyInfo;
import it.pintux.life.bedwarsaddon.util.BedwarsActionPayloads;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Pure: builds party form buttons from a PartyInfo + config. No Bukkit / no BedrockGUIApi. */
public final class PartyMenuModel {
    private final BedwarsAddonConfiguration config;

    public PartyMenuModel(BedwarsAddonConfiguration config) {
        this.config = config;
    }

    public List<MenuButton> mainButtons(PartyInfo info) {
        List<MenuButton> buttons = new ArrayList<>();
        buttons.add(new MenuButton(config.partyAddButton(), "bw_party_add:"));
        if (info.hasParty()) {
            buttons.add(new MenuButton(config.partyLeaveButton(), "bw_party_leave:"));
            if (info.owner()) {
                buttons.add(new MenuButton(config.partyDisbandButton(), "bw_party_disband:"));
                buttons.add(new MenuButton(config.partyKickButton(), "bw_party_kick:"));
            }
        }
        buttons.add(new MenuButton(config.partyCloseButton(), "bw_party_close:"));
        return buttons;
    }

    public List<MenuButton> kickButtons(List<String> memberNames) {
        List<MenuButton> buttons = new ArrayList<>();
        for (String name : memberNames) {
            String label = config.render(config.partyKickEntryButton(), Map.of("player", name));
            buttons.add(new MenuButton(label, "bw_party_kickdo:" + BedwarsActionPayloads.encode(name)));
        }
        buttons.add(new MenuButton(config.partyBackButton(), "bw_party_main:"));
        return buttons;
    }
}
