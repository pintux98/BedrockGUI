package it.pintux.life.bedwarsaddon.menu;

import it.pintux.life.bedwarsaddon.config.BedwarsAddonConfiguration;
import it.pintux.life.bedwarsaddon.model.ArenaInfo;
import it.pintux.life.bedwarsaddon.util.BedwarsActionPayloads;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Pure: builds arena-selector form buttons from DTOs + config. No Bukkit / no BedrockGUIApi. */
public final class ArenaMenuModel {
    private final BedwarsAddonConfiguration config;

    public ArenaMenuModel(BedwarsAddonConfiguration config) {
        this.config = config;
    }

    public List<MenuButton> arenaButtons(List<ArenaInfo> arenas) {
        List<MenuButton> buttons = new ArrayList<>();
        for (ArenaInfo arena : arenas) {
            String label = config.render(config.arenaButton(), Map.of(
                    "arena", arena.displayName(),
                    "state", arena.state(),
                    "players", String.valueOf(arena.players()),
                    "max", String.valueOf(arena.max()),
                    "group", arena.group()));
            buttons.add(new MenuButton(label, "bw_arena_join:" + BedwarsActionPayloads.encode(arena.name())));
        }
        buttons.add(new MenuButton(config.arenaCloseButton(), "bw_arena_close:"));
        return buttons;
    }
}
