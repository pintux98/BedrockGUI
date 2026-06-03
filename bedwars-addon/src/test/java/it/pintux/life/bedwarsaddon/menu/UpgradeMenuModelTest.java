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
        yaml.set("upgrades.upgrade-button", "{upgrade} - {cost} {currency}");
        yaml.set("upgrades.upgrade-button-unaffordable", "X {upgrade} - {cost} {currency}");
        yaml.set("upgrades.upgrade-button-maxed", "MAX {upgrade}");
        yaml.set("upgrades.upgrade-button-nocost", "{upgrade}");
        yaml.set("upgrades.close-button", "Close");
        Constructor<BedwarsAddonConfiguration> ctor =
                BedwarsAddonConfiguration.class.getDeclaredConstructor(YamlConfiguration.class);
        ctor.setAccessible(true);
        return ctor.newInstance(yaml);
    }

    @Test
    void templatesByStateThenClose() throws Exception {
        UpgradeMenuModel model = new UpgradeMenuModel(config());
        List<MenuButton> b = model.upgradeButtons(List.of(
                new UpgradeContent("0|sharpness", "Sharpened Swords", 4, "Diamond", true, false),
                new UpgradeContent("1|prot", "Reinforced Armor", 8, "Diamond", false, false),
                new UpgradeContent("2|forge", "Iron Forge", 0, "", true, true),
                new UpgradeContent("3|trap", "Its a trap!", 0, "", true, false)));

        assertEquals(5, b.size());
        assertEquals("Sharpened Swords - 4 Diamond", b.get(0).label());
        assertEquals("bw_upgrade_buy:" + BedwarsActionPayloads.encode("0|sharpness"), b.get(0).actionString());
        assertEquals("X Reinforced Armor - 8 Diamond", b.get(1).label()); // unaffordable
        assertEquals("MAX Iron Forge", b.get(2).label());                 // maxed
        assertEquals("Its a trap!", b.get(3).label());                    // no cost known
        assertEquals("Close", b.get(4).label());
        assertEquals("bw_upgrade_close:", b.get(4).actionString());
    }
}
