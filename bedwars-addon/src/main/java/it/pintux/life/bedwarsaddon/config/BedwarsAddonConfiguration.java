package it.pintux.life.bedwarsaddon.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;

public final class BedwarsAddonConfiguration {
    public static final String FILE_NAME = "config.yml";

    private final boolean moduleShop;
    private final String shopTitle;
    private final String shopContent;
    private final String shopCategoryButton;
    private final String shopItemButton;
    private final String shopItemButtonUnaffordable;
    private final String shopBackButton;
    private final String shopCloseButton;
    private final String shopPurchaseSuccess;
    private final String shopPurchaseFailed;
    private final String shopNotInGame;
    private final String shopProviderUnavailable;

    private final boolean soundsEnabled;
    private final String soundFormOpen;
    private final String soundPurchaseSuccess;
    private final String soundPurchaseFailed;
    private final float soundVolume;
    private final float soundPitch;

    private BedwarsAddonConfiguration(YamlConfiguration c) {
        this.moduleShop = c.getBoolean("modules.shop", true);
        this.shopTitle = color(c.getString("shop.title", "&8Shop"));
        this.shopContent = color(c.getString("shop.content", "Select a category"));
        this.shopCategoryButton = c.getString("shop.category-button", "&a{category}");
        this.shopItemButton = c.getString("shop.item-button", "&f{item}\n&7{cost} {currency}");
        this.shopItemButtonUnaffordable = c.getString("shop.item-button-unaffordable", "&c{item}\n&7{cost} {currency}");
        this.shopBackButton = color(c.getString("shop.back-button", "&7Back"));
        this.shopCloseButton = color(c.getString("shop.close-button", "&cClose"));
        this.shopPurchaseSuccess = c.getString("shop.purchase-success", "&aPurchased {item}.");
        this.shopPurchaseFailed = c.getString("shop.purchase-failed", "&cCould not purchase {item}: {reason}");
        this.shopNotInGame = color(c.getString("shop.not-in-game", "&cYou must be in a game to use the shop."));
        this.shopProviderUnavailable = color(c.getString("shop.provider-unavailable", "&cThe shop is currently unavailable."));

        this.soundsEnabled = c.getBoolean("sounds.enabled", true);
        this.soundFormOpen = c.getString("sounds.form-open", "UI_BUTTON_CLICK");
        this.soundPurchaseSuccess = c.getString("sounds.purchase-success", "ENTITY_PLAYER_LEVELUP");
        this.soundPurchaseFailed = c.getString("sounds.purchase-failed", "BLOCK_NOTE_BLOCK_PLING");
        this.soundVolume = (float) c.getDouble("sounds.volume", 1.0);
        this.soundPitch = (float) c.getDouble("sounds.pitch", 1.0);
    }

    public static BedwarsAddonConfiguration load(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        return new BedwarsAddonConfiguration(yaml);
    }

    private static String color(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }

    /** Replace {key} tokens then translate color codes. */
    public String render(String template, Map<String, String> values) {
        String out = template == null ? "" : template;
        for (Map.Entry<String, String> e : values.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        }
        return color(out);
    }

    public boolean moduleShop() { return moduleShop; }
    public String shopTitle() { return shopTitle; }
    public String shopContent() { return shopContent; }
    public String shopCategoryButton() { return shopCategoryButton; }
    public String shopItemButton() { return shopItemButton; }
    public String shopItemButtonUnaffordable() { return shopItemButtonUnaffordable; }
    public String shopBackButton() { return shopBackButton; }
    public String shopCloseButton() { return shopCloseButton; }
    public String shopPurchaseSuccess() { return shopPurchaseSuccess; }
    public String shopPurchaseFailed() { return shopPurchaseFailed; }
    public String shopNotInGame() { return shopNotInGame; }
    public String shopProviderUnavailable() { return shopProviderUnavailable; }
    public boolean soundsEnabled() { return soundsEnabled; }
    public String soundFormOpen() { return soundFormOpen; }
    public String soundPurchaseSuccess() { return soundPurchaseSuccess; }
    public String soundPurchaseFailed() { return soundPurchaseFailed; }
    public float soundVolume() { return soundVolume; }
    public float soundPitch() { return soundPitch; }
}
