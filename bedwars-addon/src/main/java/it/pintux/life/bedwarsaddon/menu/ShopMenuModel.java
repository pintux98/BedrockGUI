package it.pintux.life.bedwarsaddon.menu;

import it.pintux.life.bedwarsaddon.config.BedwarsAddonConfiguration;
import it.pintux.life.bedwarsaddon.model.ShopCategory;
import it.pintux.life.bedwarsaddon.model.ShopContent;
import it.pintux.life.bedwarsaddon.util.BedwarsActionPayloads;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Pure: builds ordered form buttons from shop DTOs + config. No Bukkit / no BedrockGUIApi. */
public final class ShopMenuModel {
    private final BedwarsAddonConfiguration config;

    public ShopMenuModel(BedwarsAddonConfiguration config) {
        this.config = config;
    }

    public List<MenuButton> categoryButtons(List<ShopCategory> categories) {
        List<MenuButton> buttons = new ArrayList<>();
        for (ShopCategory category : categories) {
            String label = config.render(config.shopCategoryButton(), Map.of("category", category.name()));
            buttons.add(new MenuButton(label, "bw_shop_cat:" + BedwarsActionPayloads.encode(category.id())));
        }
        buttons.add(new MenuButton(config.shopCloseButton(), "bw_shop_close:"));
        return buttons;
    }

    public List<MenuButton> contentButtons(List<ShopContent> contents) {
        List<MenuButton> buttons = new ArrayList<>();
        for (ShopContent content : contents) {
            String template = content.affordable() ? config.shopItemButton() : config.shopItemButtonUnaffordable();
            String label = config.render(template, Map.of(
                    "item", content.name(),
                    "cost", String.valueOf(content.cost()),
                    "currency", content.currency(),
                    "tier", content.tier()));
            buttons.add(new MenuButton(label, "bw_shop_buy:" + BedwarsActionPayloads.encode(content.id())));
        }
        buttons.add(new MenuButton(config.shopBackButton(), "bw_shop_main:"));
        return buttons;
    }
}
