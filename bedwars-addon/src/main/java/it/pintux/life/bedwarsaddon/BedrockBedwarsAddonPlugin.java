package it.pintux.life.bedwarsaddon;

import it.pintux.life.bedwarsaddon.action.ArenaJoinAction;
import it.pintux.life.bedwarsaddon.action.OpenArenaMainAction;
import it.pintux.life.bedwarsaddon.action.OpenPartyMainAction;
import it.pintux.life.bedwarsaddon.action.OpenSpectatorAction;
import it.pintux.life.bedwarsaddon.action.PartyAddAction;
import it.pintux.life.bedwarsaddon.action.PartyDisbandAction;
import it.pintux.life.bedwarsaddon.action.PartyKickAction;
import it.pintux.life.bedwarsaddon.action.PartyKickDoAction;
import it.pintux.life.bedwarsaddon.action.PartyLeaveAction;
import it.pintux.life.bedwarsaddon.action.SpectatorTeleportAction;
import it.pintux.life.bedwarsaddon.action.OpenShopCategoryAction;
import it.pintux.life.bedwarsaddon.action.OpenShopMainAction;
import it.pintux.life.bedwarsaddon.action.OpenStatsAction;
import it.pintux.life.bedwarsaddon.action.OpenUpgradeMainAction;
import it.pintux.life.bedwarsaddon.action.ShopBuyAction;
import it.pintux.life.bedwarsaddon.action.UpgradeBuyAction;
import it.pintux.life.bedwarsaddon.api.BedrockPlayerDetector;
import it.pintux.life.bedwarsaddon.command.BedwarsAddonCommand;
import it.pintux.life.bedwarsaddon.config.BedwarsAddonConfiguration;
import it.pintux.life.bedwarsaddon.listener.MenuInterceptListener;
import it.pintux.life.bedwarsaddon.listener.ShopOpenListener;
import it.pintux.life.bedwarsaddon.provider.BedWars2023ArenaProvider;
import it.pintux.life.bedwarsaddon.provider.BedWars2023PartyProvider;
import it.pintux.life.bedwarsaddon.provider.BedWars2023ShopProvider;
import it.pintux.life.bedwarsaddon.provider.BedWars2023SpectatorProvider;
import it.pintux.life.bedwarsaddon.provider.BedWars2023StatsProvider;
import it.pintux.life.bedwarsaddon.provider.BedWars2023UpgradeProvider;
import it.pintux.life.bedwarsaddon.provider.BedWarsApiAccess;
import it.pintux.life.bedwarsaddon.provider.FloodgateBedrockPlayerDetector;
import it.pintux.life.bedwarsaddon.service.ArenaCatalogService;
import it.pintux.life.bedwarsaddon.service.BedrockArenaService;
import it.pintux.life.bedwarsaddon.service.ArenaCatalogService;
import it.pintux.life.bedwarsaddon.service.BedrockArenaService;
import it.pintux.life.bedwarsaddon.service.BedrockPartyService;
import it.pintux.life.bedwarsaddon.service.BedrockShopService;
import it.pintux.life.bedwarsaddon.service.BedrockSpectatorService;
import it.pintux.life.bedwarsaddon.service.BedrockStatsService;
import it.pintux.life.bedwarsaddon.service.BedrockUpgradeService;
import it.pintux.life.bedwarsaddon.service.PartyCatalogService;
import it.pintux.life.bedwarsaddon.service.ShopCatalogService;
import it.pintux.life.bedwarsaddon.service.SpectatorCatalogService;
import it.pintux.life.bedwarsaddon.service.StatsCatalogService;
import it.pintux.life.bedwarsaddon.service.UpgradeCatalogService;
import it.pintux.life.bedwarsaddon.util.BedrockSoundFeedback;
import it.pintux.life.common.api.BedrockGUIApi;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class BedrockBedwarsAddonPlugin extends JavaPlugin {
    private BedwarsAddonConfiguration configuration;
    private BedrockPlayerDetector detector;
    private BedrockSoundFeedback soundFeedback;
    private BedWarsApiAccess apiAccess;
    private ShopCatalogService shopCatalogService;
    private BedrockShopService bedrockShopService;
    private UpgradeCatalogService upgradeCatalogService;
    private BedrockUpgradeService bedrockUpgradeService;
    private ArenaCatalogService arenaCatalogService;
    private BedrockArenaService bedrockArenaService;
    private StatsCatalogService statsCatalogService;
    private BedrockStatsService bedrockStatsService;
    private SpectatorCatalogService spectatorCatalogService;
    private BedrockSpectatorService bedrockSpectatorService;
    private PartyCatalogService partyCatalogService;
    private BedrockPartyService bedrockPartyService;

    @Override
    public void onEnable() {
        configuration = BedwarsAddonConfiguration.load(this);
        if (!configuration.moduleShop() && !configuration.moduleUpgrades() && !configuration.moduleArena()) {
            getLogger().info("All modules disabled. Enable one in config.yml then /bedwarsaddon reload.");
            return;
        }

        detector = new FloodgateBedrockPlayerDetector();
        soundFeedback = new BedrockSoundFeedback();
        soundFeedback.configure(configuration.soundsEnabled(), configuration.soundFormOpen(),
                configuration.soundPurchaseSuccess(), configuration.soundPurchaseFailed(),
                configuration.soundVolume(), configuration.soundPitch());

        apiAccess = new BedWarsApiAccess();
        boolean bedwarsPresent = Bukkit.getPluginManager().getPlugin("BedWars2023") != null;
        if (!bedwarsPresent) {
            getLogger().warning("BedWars2023 not found; modules inactive until it is installed.");
        }

        PluginManager pm = Bukkit.getPluginManager();

        if (configuration.moduleShop()) {
            shopCatalogService = new ShopCatalogService(getLogger());
            if (bedwarsPresent) {
                shopCatalogService.setProvider(new BedWars2023ShopProvider(getLogger(), apiAccess));
                getLogger().info("Shop provider: BedWars2023");
            }
            bedrockShopService = new BedrockShopService(configuration, shopCatalogService, detector, soundFeedback);
            pm.registerEvents(new ShopOpenListener(this, bedrockShopService), this);
        }

        if (configuration.moduleUpgrades()) {
            upgradeCatalogService = new UpgradeCatalogService(getLogger());
            if (bedwarsPresent) {
                upgradeCatalogService.setProvider(new BedWars2023UpgradeProvider(getLogger(), apiAccess));
                getLogger().info("Upgrade provider: BedWars2023");
            }
            bedrockUpgradeService = new BedrockUpgradeService(configuration, upgradeCatalogService, detector, soundFeedback);
        }

        if (configuration.moduleArena()) {
            arenaCatalogService = new ArenaCatalogService(getLogger());
            if (bedwarsPresent) {
                arenaCatalogService.setProvider(new BedWars2023ArenaProvider(getLogger(), apiAccess));
                getLogger().info("Arena provider: BedWars2023");
            }
            bedrockArenaService = new BedrockArenaService(configuration, arenaCatalogService, detector, soundFeedback);
        }

        if (configuration.moduleStats()) {
            statsCatalogService = new StatsCatalogService(getLogger());
            if (bedwarsPresent) {
                statsCatalogService.setProvider(new BedWars2023StatsProvider(apiAccess));
                getLogger().info("Stats provider: BedWars2023");
            }
            bedrockStatsService = new BedrockStatsService(configuration, statsCatalogService, detector, soundFeedback);
        }

        if (configuration.moduleSpectator()) {
            spectatorCatalogService = new SpectatorCatalogService(getLogger());
            if (bedwarsPresent) {
                spectatorCatalogService.setProvider(new BedWars2023SpectatorProvider(getLogger(), apiAccess));
                getLogger().info("Spectator provider: BedWars2023");
            }
            bedrockSpectatorService = new BedrockSpectatorService(configuration, spectatorCatalogService, detector, soundFeedback);
        }

        if (configuration.moduleParty()) {
            partyCatalogService = new PartyCatalogService(getLogger());
            if (bedwarsPresent) {
                partyCatalogService.setProvider(new BedWars2023PartyProvider(apiAccess));
                getLogger().info("Party provider: BedWars2023");
            }
            bedrockPartyService = new BedrockPartyService(configuration, partyCatalogService, detector, soundFeedback);
        }

        if (bedrockArenaService != null || bedrockUpgradeService != null
                || bedrockStatsService != null || bedrockSpectatorService != null) {
            pm.registerEvents(new MenuInterceptListener(this, bedrockArenaService, bedrockUpgradeService,
                    bedrockStatsService, bedrockSpectatorService), this);
        }

        getCommand("bedwarsaddon").setExecutor(new BedwarsAddonCommand(this));
        getCommand("bedwarsaddon").setTabCompleter(new BedwarsAddonCommand(this));

        BedrockGUIApi api = getApiSafely();
        if (api != null) {
            if (bedrockShopService != null) {
                api.registerActionHandler(new OpenShopMainAction(bedrockShopService));
                api.registerActionHandler(new OpenShopCategoryAction(bedrockShopService));
                api.registerActionHandler(new ShopBuyAction(bedrockShopService));
            }
            if (bedrockUpgradeService != null) {
                api.registerActionHandler(new OpenUpgradeMainAction(bedrockUpgradeService));
                api.registerActionHandler(new UpgradeBuyAction(bedrockUpgradeService));
            }
            if (bedrockArenaService != null) {
                api.registerActionHandler(new OpenArenaMainAction(bedrockArenaService));
                api.registerActionHandler(new ArenaJoinAction(bedrockArenaService));
            }
            if (bedrockStatsService != null) {
                api.registerActionHandler(new OpenStatsAction(bedrockStatsService));
            }
            if (bedrockSpectatorService != null) {
                api.registerActionHandler(new OpenSpectatorAction(bedrockSpectatorService));
                api.registerActionHandler(new SpectatorTeleportAction(bedrockSpectatorService));
            }
            if (bedrockPartyService != null) {
                api.registerActionHandler(new OpenPartyMainAction(bedrockPartyService));
                api.registerActionHandler(new PartyAddAction(bedrockPartyService));
                api.registerActionHandler(new PartyLeaveAction(bedrockPartyService));
                api.registerActionHandler(new PartyDisbandAction(bedrockPartyService));
                api.registerActionHandler(new PartyKickAction(bedrockPartyService));
                api.registerActionHandler(new PartyKickDoAction(bedrockPartyService));
            }
            getLogger().info("Registered bedwars addon actions with BedrockGUI API");
        }
    }

    @Override
    public void onDisable() {
        shopCatalogService = null;
        bedrockShopService = null;
        upgradeCatalogService = null;
        bedrockUpgradeService = null;
        arenaCatalogService = null;
        bedrockArenaService = null;
        statsCatalogService = null;
        bedrockStatsService = null;
        spectatorCatalogService = null;
        bedrockSpectatorService = null;
        partyCatalogService = null;
        bedrockPartyService = null;
    }

    /** Opens the Bedrock party form (used by /bedwarsaddon party). */
    public void openParty(Player player) {
        if (bedrockPartyService != null) {
            bedrockPartyService.openMain(player);
        } else {
            player.sendMessage("Party module is disabled.");
        }
    }

    public void reloadConfiguration() {
        configuration = BedwarsAddonConfiguration.load(this);
        if (soundFeedback != null) {
            soundFeedback.configure(configuration.soundsEnabled(), configuration.soundFormOpen(),
                    configuration.soundPurchaseSuccess(), configuration.soundPurchaseFailed(),
                    configuration.soundVolume(), configuration.soundPitch());
        }
        if (shopCatalogService != null) {
            bedrockShopService = new BedrockShopService(configuration, shopCatalogService, detector, soundFeedback);
        }
        if (upgradeCatalogService != null) {
            bedrockUpgradeService = new BedrockUpgradeService(configuration, upgradeCatalogService, detector, soundFeedback);
        }
        if (arenaCatalogService != null) {
            bedrockArenaService = new BedrockArenaService(configuration, arenaCatalogService, detector, soundFeedback);
        }
        if (statsCatalogService != null) {
            bedrockStatsService = new BedrockStatsService(configuration, statsCatalogService, detector, soundFeedback);
        }
        if (spectatorCatalogService != null) {
            bedrockSpectatorService = new BedrockSpectatorService(configuration, spectatorCatalogService, detector, soundFeedback);
        }
        if (partyCatalogService != null) {
            bedrockPartyService = new BedrockPartyService(configuration, partyCatalogService, detector, soundFeedback);
        }
    }

    private BedrockGUIApi getApiSafely() {
        try {
            return BedrockGUIApi.getInstance();
        } catch (IllegalStateException e) {
            getLogger().warning("BedrockGUI-Paper not found. Shop actions unavailable until it loads.");
            return null;
        }
    }
}
