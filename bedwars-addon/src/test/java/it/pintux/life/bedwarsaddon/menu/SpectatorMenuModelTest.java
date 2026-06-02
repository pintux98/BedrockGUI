package it.pintux.life.bedwarsaddon.menu;

import it.pintux.life.bedwarsaddon.config.BedwarsAddonConfiguration;
import it.pintux.life.bedwarsaddon.model.SpectateTarget;
import it.pintux.life.bedwarsaddon.util.BedwarsActionPayloads;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpectatorMenuModelTest {

    private BedwarsAddonConfiguration config() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("spectator.target-button", "{player}");
        yaml.set("spectator.close-button", "Close");
        Constructor<BedwarsAddonConfiguration> ctor =
                BedwarsAddonConfiguration.class.getDeclaredConstructor(YamlConfiguration.class);
        ctor.setAccessible(true);
        return ctor.newInstance(yaml);
    }

    @Test
    void targetButtonsOnePerTargetThenClose() throws Exception {
        SpectatorMenuModel model = new SpectatorMenuModel(config());
        List<MenuButton> buttons = model.targetButtons(List.of(
                new SpectateTarget("11111111-1111-1111-1111-111111111111", "Alex"),
                new SpectateTarget("22222222-2222-2222-2222-222222222222", "Steve")));

        assertEquals(3, buttons.size());
        assertEquals("Alex", buttons.get(0).label());
        assertEquals("bw_spec_tp:" + BedwarsActionPayloads.encode("11111111-1111-1111-1111-111111111111"),
                buttons.get(0).actionString());
        assertEquals("Steve", buttons.get(1).label());
        assertEquals("Close", buttons.get(2).label());
        assertEquals("bw_spec_close:", buttons.get(2).actionString());
    }
}
