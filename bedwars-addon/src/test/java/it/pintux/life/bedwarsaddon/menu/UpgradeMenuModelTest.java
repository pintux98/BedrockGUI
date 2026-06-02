package it.pintux.life.bedwarsaddon.menu;

import it.pintux.life.bedwarsaddon.config.BedwarsAddonConfiguration;
import it.pintux.life.bedwarsaddon.model.UpgradeContent;
import it.pintux.life.bedwarsaddon.util.BedwarsActionPayloads;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UpgradeMenuModelTest {

    private BedwarsAddonConfiguration config() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("upgrades.upgrade-button", "{upgrade}");
        yaml.set("upgrades.close-button", "Close");
        Constructor<BedwarsAddonConfiguration> ctor =
                BedwarsAddonConfiguration.class.getDeclaredConstructor(YamlConfiguration.class);
        ctor.setAccessible(true);
        return ctor.newInstance(yaml);
    }

    @Test
    void upgradeButtonsOnePerUpgradeThenClose() throws Exception {
        UpgradeMenuModel model = new UpgradeMenuModel(config());
        List<MenuButton> buttons = model.upgradeButtons(List.of(
                new UpgradeContent("sharpness", "Sharpened Swords"),
                new UpgradeContent("haste", "Maniac Miner")));

        assertEquals(3, buttons.size());
        assertEquals("Sharpened Swords", buttons.get(0).label());
        assertEquals("bw_upgrade_buy:" + BedwarsActionPayloads.encode("sharpness"), buttons.get(0).actionString());
        assertEquals("Maniac Miner", buttons.get(1).label());
        assertEquals("Close", buttons.get(2).label());
        assertEquals("bw_upgrade_close:", buttons.get(2).actionString());
    }
}
