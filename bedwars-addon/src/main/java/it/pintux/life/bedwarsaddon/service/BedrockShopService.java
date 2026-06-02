package it.pintux.life.bedwarsaddon.service;

import it.pintux.life.bedwarsaddon.api.BedrockPlayerDetector;
import it.pintux.life.bedwarsaddon.config.BedwarsAddonConfiguration;
import it.pintux.life.bedwarsaddon.menu.MenuButton;
import it.pintux.life.bedwarsaddon.menu.ShopMenuModel;
import it.pintux.life.bedwarsaddon.model.ShopContent;
import it.pintux.life.bedwarsaddon.util.BedrockSoundFeedback;
import it.pintux.life.bedwarsaddon.util.BukkitFormPlayer;
import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.api.BedrockGUIApi;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class BedrockShopService {
    private final BedwarsAddonConfiguration config;
    private final ShopCatalogService catalog;
    private final ShopMenuModel menuModel;
    private final BedrockPlayerDetector detector;
    private final BedrockSoundFeedback sound;
    /** Last category each player was browsing, so a purchase re-opens the same list. */
    private final ConcurrentMap<UUID, String> lastCategory = new ConcurrentHashMap<>();

    public BedrockShopService(BedwarsAddonConfiguration config, ShopCatalogService catalog,
                              BedrockPlayerDetector detector, BedrockSoundFeedback sound) {
        this.config = config;
        this.catalog = catalog;
        this.detector = detector;
        this.sound = sound;
        this.menuModel = new ShopMenuModel(config);
    }

    public boolean shouldHandle(Player player) {
        return player != null && detector.isBedrockPlayer(player);
    }

    public void openMain(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!catalog.isReady()) { player.sendMessage(config.shopProviderUnavailable()); return; }

        List<MenuButton> buttons = menuModel.categoryButtons(catalog.getCategories(player));
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(config.shopTitle());
        form.content(config.shopContent());
        for (MenuButton button : buttons) {
            String action = button.actionString();
            form.button(button.label(), fp -> api.executeActionString(fp, action, ctx("shop-main")));
        }
        sound.playFormOpen(player);
        form.send(new BukkitFormPlayer(player));
    }

    public void openCategory(Player player, String categoryId) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!catalog.isReady()) { player.sendMessage(config.shopProviderUnavailable()); return; }

        lastCategory.put(player.getUniqueId(), categoryId);
        List<ShopContent> contents = catalog.getCategoryContents(player, categoryId);
        List<MenuButton> buttons = menuModel.contentButtons(contents);
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(config.shopTitle());
        form.content(config.shopContent());
        for (MenuButton button : buttons) {
            String action = button.actionString();
            form.button(button.label(), fp -> api.executeActionString(fp, action, ctx("shop-cat")));
        }
        form.send(new BukkitFormPlayer(player));
    }

    public void buy(Player player, String contentId) {
        if (!catalog.isReady()) { player.sendMessage(config.shopProviderUnavailable()); return; }
        // BedWars' execute() performs the buy and sends its OWN messages/sounds (purchased,
        // insufficient funds, etc.) — we deliberately do not send any of our own here.
        catalog.purchase(player, contentId);
        // Re-open the category the player was browsing so the list reflects new money/tier state.
        String last = lastCategory.get(player.getUniqueId());
        if (last != null) {
            openCategory(player, last);
        } else {
            openMain(player);
        }
    }

    private BedrockGUIApi requireApi(Player player) {
        try {
            return BedrockGUIApi.getInstance();
        } catch (IllegalStateException e) {
            player.sendMessage(config.shopProviderUnavailable());
            return null;
        }
    }

    private ActionSystem.ActionContext ctx(String source) {
        return ActionSystem.ActionContext.builder()
                .menuName(source)
                .formType("bedrock-bw-shop")
                .build();
    }
}
