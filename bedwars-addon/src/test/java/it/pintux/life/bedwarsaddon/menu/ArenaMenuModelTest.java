package it.pintux.life.bedwarsaddon.menu;

import it.pintux.life.bedwarsaddon.config.BedwarsAddonConfiguration;
import it.pintux.life.bedwarsaddon.model.ArenaInfo;
import it.pintux.life.bedwarsaddon.util.BedwarsActionPayloads;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArenaMenuModelTest {

    private BedwarsAddonConfiguration config() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("arena.arena-button", "{arena} {state} {players}/{max}");
        yaml.set("arena.close-button", "Close");
        Constructor<BedwarsAddonConfiguration> ctor =
                BedwarsAddonConfiguration.class.getDeclaredConstructor(YamlConfiguration.class);
        ctor.setAccessible(true);
        return ctor.newInstance(yaml);
    }

    @Test
    void arenaButtonsOnePerArenaThenClose() throws Exception {
        ArenaMenuModel model = new ArenaMenuModel(config());
        List<MenuButton> buttons = model.arenaButtons(List.of(
                new ArenaInfo("solo_1", "Solo Islands", "waiting", 3, 8, "Solo"),
                new ArenaInfo("duo_2", "Duo Castle", "playing", 16, 16, "Duo")));

        assertEquals(3, buttons.size());
        assertEquals("Solo Islands waiting 3/8", buttons.get(0).label());
        assertEquals("bw_arena_join:" + BedwarsActionPayloads.encode("solo_1"), buttons.get(0).actionString());
        assertEquals("Duo Castle playing 16/16", buttons.get(1).label());
        assertEquals("Close", buttons.get(2).label());
        assertEquals("bw_arena_close:", buttons.get(2).actionString());
    }
}
