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
    private final boolean moduleSpectator;
    private final boolean moduleParty;
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
    private final String statsRefreshButton;
    private final String statsProviderUnavailable;

    private final String spectatorTitle;
    private final String spectatorContent;
    private final String spectatorGuiTitleContains;
    private final String spectatorTargetButton;
    private final String spectatorCloseButton;
    private final String spectatorNoTargets;
    private final String spectatorProviderUnavailable;

    private final String partyTitle;
    private final String partyContent;
    private final String partyMemberButton;
    private final String partyAddButton;
    private final String partyLeaveButton;
    private final String partyDisbandButton;
    private final String partyKickButton;
    private final String partyKickEntryButton;
    private final String partyBackButton;
    private final String partyCloseButton;
    private final String partyAddInputTitle;
    private final String partyAddInputLabel;
    private final String partyNoParty;
    private final String partyNotOwner;
    private final String partyAdded;
    private final String partyPlayerNotFound;
    private final String partyKicked;
    private final String partyProviderUnavailable;

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
        this.moduleSpectator = c.getBoolean("modules.spectator", true);
        this.moduleParty = c.getBoolean("modules.party", true);
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
        this.statsContent = c.getString("stats.content",
                "&aWins: &f{wins}  &cLosses: &f{losses}\n&aKills: &f{kills}  &aFinal Kills: &f{final_kills}\n"
                        + "&cDeaths: &f{deaths}  &cFinal Deaths: &f{final_deaths}\n&eBeds Broken: &f{beds}\n"
                        + "&7Games Played: &f{games}\n&7K/D: &f{kd}  &7W/L: &f{wl}");
        this.statsCloseButton = color(c.getString("stats.close-button", "&cClose"));
        this.statsRefreshButton = color(c.getString("stats.refresh-button", "&aRefresh"));
        this.statsProviderUnavailable = color(c.getString("stats.provider-unavailable", "&cStats are currently unavailable."));

        this.spectatorTitle = color(c.getString("spectator.title", "&8Teleporter"));
        this.spectatorContent = color(c.getString("spectator.content", "Teleport to a player"));
        this.spectatorGuiTitleContains = c.getString("spectator.gui-title-contains", "teleport");
        this.spectatorTargetButton = c.getString("spectator.target-button", "&a{player}");
        this.spectatorCloseButton = color(c.getString("spectator.close-button", "&cClose"));
        this.spectatorNoTargets = color(c.getString("spectator.no-targets", "&cNo players to teleport to."));
        this.spectatorProviderUnavailable = color(c.getString("spectator.provider-unavailable", "&cThe teleporter is currently unavailable."));

        this.partyTitle = color(c.getString("party.title", "&8Party"));
        this.partyContent = c.getString("party.content", "Owner: {owner}\nMembers: {size}");
        this.partyMemberButton = c.getString("party.member-button", "&f{player}");
        this.partyAddButton = color(c.getString("party.add-button", "&aAdd Player"));
        this.partyLeaveButton = color(c.getString("party.leave-button", "&eLeave Party"));
        this.partyDisbandButton = color(c.getString("party.disband-button", "&cDisband Party"));
        this.partyKickButton = color(c.getString("party.kick-button", "&cKick a Player"));
        this.partyKickEntryButton = c.getString("party.kick-entry-button", "&c{player}");
        this.partyBackButton = color(c.getString("party.back-button", "&7Back"));
        this.partyCloseButton = color(c.getString("party.close-button", "&cClose"));
        this.partyAddInputTitle = color(c.getString("party.add-input-title", "&8Add Player"));
        this.partyAddInputLabel = color(c.getString("party.add-input-label", "Player name"));
        this.partyNoParty = color(c.getString("party.no-party", "&cYou are not in a party."));
        this.partyNotOwner = color(c.getString("party.not-owner", "&cOnly the party owner can do that."));
        this.partyAdded = c.getString("party.added", "&aAdded {player} to the party.");
        this.partyPlayerNotFound = c.getString("party.player-not-found", "&cPlayer {player} not found.");
        this.partyKicked = c.getString("party.kicked", "&aKicked {player}.");
        this.partyProviderUnavailable = color(c.getString("party.provider-unavailable", "&cParties are currently unavailable."));

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
    public String statsRefreshButton() { return statsRefreshButton; }
    public String statsProviderUnavailable() { return statsProviderUnavailable; }
    public boolean moduleSpectator() { return moduleSpectator; }
    public boolean moduleParty() { return moduleParty; }
    public String spectatorTitle() { return spectatorTitle; }
    public String spectatorContent() { return spectatorContent; }
    public String spectatorGuiTitleContains() { return spectatorGuiTitleContains; }
    public String spectatorTargetButton() { return spectatorTargetButton; }
    public String spectatorCloseButton() { return spectatorCloseButton; }
    public String spectatorNoTargets() { return spectatorNoTargets; }
    public String spectatorProviderUnavailable() { return spectatorProviderUnavailable; }
    public String partyTitle() { return partyTitle; }
    public String partyContent() { return partyContent; }
    public String partyMemberButton() { return partyMemberButton; }
    public String partyAddButton() { return partyAddButton; }
    public String partyLeaveButton() { return partyLeaveButton; }
    public String partyDisbandButton() { return partyDisbandButton; }
    public String partyKickButton() { return partyKickButton; }
    public String partyKickEntryButton() { return partyKickEntryButton; }
    public String partyBackButton() { return partyBackButton; }
    public String partyCloseButton() { return partyCloseButton; }
    public String partyAddInputTitle() { return partyAddInputTitle; }
    public String partyAddInputLabel() { return partyAddInputLabel; }
    public String partyNoParty() { return partyNoParty; }
    public String partyNotOwner() { return partyNotOwner; }
    public String partyAdded() { return partyAdded; }
    public String partyPlayerNotFound() { return partyPlayerNotFound; }
    public String partyKicked() { return partyKicked; }
    public String partyProviderUnavailable() { return partyProviderUnavailable; }
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
