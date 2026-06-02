package it.pintux.life.bedwarsaddon.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;

public final class BedwarsAddonConfiguration {
    public static final String FILE_NAME = "config.yml";

    private final boolean moduleShop;
    private final boolean moduleUpgrades;
    private final boolean moduleArena;
    private final boolean moduleStats;
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

    private final String upgradeTitle;
    private final String upgradeContent;
    private final String upgradeButton;
    private final String upgradeCloseButton;
    private final String upgradeProviderUnavailable;

    private final String arenaTitle;
    private final String arenaContent;
    private final String arenaButton;
    private final String arenaCloseButton;
    private final String arenaNoArenas;
    private final String arenaJoinFailed;
    private final String arenaProviderUnavailable;

    private final String statsTitle;
    private final String statsGuiTitleContains;
    private final String statsContent;
    private final String statsCloseButton;
    private final String statsProviderUnavailable;

    private final boolean soundsEnabled;
    private final String soundFormOpen;
    private final String soundPurchaseSuccess;
    private final String soundPurchaseFailed;
    private final float soundVolume;
    private final float soundPitch;

    private BedwarsAddonConfiguration(YamlConfiguration c) {
        this.moduleShop = c.getBoolean("modules.shop", true);
        this.moduleUpgrades = c.getBoolean("modules.upgrades", true);
        this.moduleArena = c.getBoolean("modules.arena", true);
        this.moduleStats = c.getBoolean("modules.stats", true);
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

        this.upgradeTitle = color(c.getString("upgrades.title", "&8Team Upgrades"));
        this.upgradeContent = color(c.getString("upgrades.content", "Buy upgrades for your team"));
        this.upgradeButton = c.getString("upgrades.upgrade-button", "&a{upgrade}");
        this.upgradeCloseButton = color(c.getString("upgrades.close-button", "&cClose"));
        this.upgradeProviderUnavailable = color(c.getString("upgrades.provider-unavailable", "&cUpgrades are currently unavailable."));

        this.arenaTitle = color(c.getString("arena.title", "&8Play BedWars"));
        this.arenaContent = color(c.getString("arena.content", "Select an arena"));
        this.arenaButton = c.getString("arena.arena-button", "&a{arena}\n&7{state} &8| &7{players}/{max}");
        this.arenaCloseButton = color(c.getString("arena.close-button", "&cClose"));
        this.arenaNoArenas = color(c.getString("arena.no-arenas", "&cNo arenas are available right now."));
        this.arenaJoinFailed = c.getString("arena.join-failed", "&cCould not join {arena}.");
        this.arenaProviderUnavailable = color(c.getString("arena.provider-unavailable", "&cArena joining is currently unavailable."));

        this.statsTitle = color(c.getString("stats.title", "&8Your Stats"));
        this.statsGuiTitleContains = c.getString("stats.gui-title-contains", "stat");
        this.statsContent = c.getString("stats.content", "Wins: {wins}");
        this.statsCloseButton = color(c.getString("stats.close-button", "&cClose"));
        this.statsProviderUnavailable = color(c.getString("stats.provider-unavailable", "&cStats are currently unavailable."));

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
    public boolean moduleUpgrades() { return moduleUpgrades; }
    public boolean moduleArena() { return moduleArena; }
    public boolean moduleStats() { return moduleStats; }
    public String statsTitle() { return statsTitle; }
    public String statsGuiTitleContains() { return statsGuiTitleContains; }
    public String statsContent() { return statsContent; }
    public String statsCloseButton() { return statsCloseButton; }
    public String statsProviderUnavailable() { return statsProviderUnavailable; }
    public String upgradeTitle() { return upgradeTitle; }
    public String upgradeContent() { return upgradeContent; }
    public String upgradeButton() { return upgradeButton; }
    public String upgradeCloseButton() { return upgradeCloseButton; }
    public String upgradeProviderUnavailable() { return upgradeProviderUnavailable; }
    public String arenaTitle() { return arenaTitle; }
    public String arenaContent() { return arenaContent; }
    public String arenaButton() { return arenaButton; }
    public String arenaCloseButton() { return arenaCloseButton; }
    public String arenaNoArenas() { return arenaNoArenas; }
    public String arenaJoinFailed() { return arenaJoinFailed; }
    public String arenaProviderUnavailable() { return arenaProviderUnavailable; }
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
