package it.pintux.life.bedwarsaddon.menu;

import it.pintux.life.bedwarsaddon.config.BedwarsAddonConfiguration;
import it.pintux.life.bedwarsaddon.model.ShopCategory;
import it.pintux.life.bedwarsaddon.model.ShopContent;
import it.pintux.life.bedwarsaddon.util.BedwarsActionPayloads;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopMenuModelTest {

    // Build a configuration from an in-memory YAML (no plugin/disk needed).
    private BedwarsAddonConfiguration config() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("shop.category-button", "{category}");
        yaml.set("shop.item-button", "{item} - {cost} {currency}");
        yaml.set("shop.item-button-unaffordable", "X {item} - {cost} {currency}");
        yaml.set("shop.back-button", "Back");
        yaml.set("shop.close-button", "Close");
        Constructor<BedwarsAddonConfiguration> ctor =
                BedwarsAddonConfiguration.class.getDeclaredConstructor(YamlConfiguration.class);
        ctor.setAccessible(true);
        return ctor.newInstance(yaml);
    }

    @Test
    void categoryButtonsOnePerCategoryThenClose() throws Exception {
        ShopMenuModel model = new ShopMenuModel(config());
        List<MenuButton> buttons = model.categoryButtons(List.of(
                new ShopCategory("blocks", "Blocks", 1),
                new ShopCategory("melee", "Melee", 2)));

        assertEquals(3, buttons.size());
        assertEquals("Blocks", buttons.get(0).label());
        assertEquals("bw_shop_cat:" + BedwarsActionPayloads.encode("blocks"), buttons.get(0).actionString());
        assertEquals("Melee", buttons.get(1).label());
        assertEquals("Close", buttons.get(2).label());
        assertEquals("bw_shop_close:", buttons.get(2).actionString());
    }

    @Test
    void itemButtonsUseAffordableVsUnaffordableTemplateThenBack() throws Exception {
        ShopMenuModel model = new ShopMenuModel(config());
        List<MenuButton> buttons = model.contentButtons(List.of(
                new ShopContent("wool", "Wool", 4, "Iron", true, ""),
                new ShopContent("sword", "Sword", 7, "Gold", false, "II")));

        assertEquals(3, buttons.size());
        assertEquals("Wool - 4 Iron", buttons.get(0).label());
        assertEquals("bw_shop_buy:" + BedwarsActionPayloads.encode("wool"), buttons.get(0).actionString());
        assertEquals("X Sword - 7 Gold", buttons.get(1).label());
        assertEquals("Back", buttons.get(2).label());
        assertEquals("bw_shop_main:", buttons.get(2).actionString());
    }
}
