package it.pintux.life.essentialsaddon;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.essentialsaddon.action.*;
import it.pintux.life.essentialsaddon.api.*;
import it.pintux.life.essentialsaddon.backend.*;
import it.pintux.life.essentialsaddon.command.EssentialsAddonCommand;
import it.pintux.life.essentialsaddon.config.EssentialsAddonConfiguration;
import it.pintux.life.essentialsaddon.listener.*;
import it.pintux.life.essentialsaddon.provider.*;
import it.pintux.life.essentialsaddon.service.*;
import it.pintux.life.essentialsaddon.util.BedrockSoundFeedback;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class BedrockEssentialsAddonPlugin extends JavaPlugin {
    private EssentialsAddonConfiguration configuration;
    private WarpCatalogService warpCatalogService;
    private KitCatalogService kitCatalogService;
    private HomeCatalogService homeCatalogService;
    private TpaCatalogService tpaCatalogService;
    private BedrockEssentialsService bedrockEssentialsService;
    private BedrockHomeService bedrockHomeService;
    private BedrockTpaService bedrockTpaService;
    private ShopGuiCatalogService shopCatalogService;
    private ShopGuiPlusHook shopGuiPlusHook;
    private BedrockShopGuiService bedrockShopGuiService;
    private EconomyShopCatalogService economyShopCatalogService;
    private EconomyShopGuiHook economyShopGuiHook;
    private BedrockEconomyShopService bedrockEconomyShopService;
    private ShopBackendRouter backendRouter;
    private BedrockSoundFeedback soundFeedback;
    private BedrockHubService hubService;
    private BedrockPlayerDetector detector;
    private PetCatalogService petCatalogService;
    private BedrockPetService bedrockPetService;

    @Override
    public void onDisable() {
        warpCatalogService = null;
        kitCatalogService = null;
        homeCatalogService = null;
        tpaCatalogService = null;
        bedrockEssentialsService = null;
        bedrockHomeService = null;
        bedrockTpaService = null;
        shopCatalogService = null;
        shopGuiPlusHook = null;
        bedrockShopGuiService = null;
        economyShopCatalogService = null;
        economyShopGuiHook = null;
        bedrockEconomyShopService = null;
        backendRouter = null;
        hubService = null;
        petCatalogService = null;
        bedrockPetService = null;
    }

    @Override
    public void onEnable() {
        configuration = EssentialsAddonConfiguration.load(this);
        detector = new FloodgateBedrockPlayerDetector();
        soundFeedback = new BedrockSoundFeedback();
        soundFeedback.configure(
                configuration.soundsEnabled(),
                configuration.soundFormOpen(),
                configuration.soundShopPurchaseSuccess(),
                configuration.soundShopPurchaseFailed(),
                configuration.soundVolume(),
                configuration.soundPitch()
        );

        boolean anyModule = configuration.moduleWarps() || configuration.moduleKits()
                || configuration.moduleHomes() || configuration.moduleTpa()
                || configuration.moduleShopGuiPlus() || configuration.moduleEconomyShopGui()
                || configuration.actionsWarps() || configuration.actionsKits()
                || configuration.actionsHomes() || configuration.actionsTpa()
                || configuration.actionsShopGuiPlus() || configuration.actionsEconomyShopGui()
                || configuration.moduleMyPet() || configuration.actionsMyPet();

        if (!anyModule) {
            getLogger().info("All modules disabled. Enable one in config.yml then run /essentialsaddon reload.");
            return;
        }

        hubService = new BedrockHubService(configuration, detector, soundFeedback);

        PluginManager pluginManager = Bukkit.getPluginManager();

        if (configuration.moduleWarps() || configuration.moduleKits()
                || configuration.actionsWarps() || configuration.actionsKits()) {
            initWarpsAndKits(pluginManager);
        }
        if (configuration.moduleHomes() || configuration.actionsHomes()) {
            initHomes(pluginManager);
        }
        if (configuration.moduleTpa() || configuration.actionsTpa()) {
            initTpa(pluginManager);
        }
        if (configuration.moduleShopGuiPlus() || configuration.actionsShopGuiPlus()) {
            initShopGuiPlus(pluginManager);
        }
        if (configuration.moduleEconomyShopGui() || configuration.actionsEconomyShopGui()) {
            initEconomyShopGui(pluginManager);
        }
        if (configuration.moduleMyPet() || configuration.actionsMyPet()) {
            initMyPet(pluginManager);
        }

        if (backendRouter != null) {
            registerShopListeners(pluginManager);
        }

        getCommand("essentialsaddon").setExecutor(new EssentialsAddonCommand(this));
        getCommand("essentialsaddon").setTabCompleter(new EssentialsAddonCommand(this));

        BedrockGUIApi api = getApiSafely();
        if (api != null) {
            registerActions(api);
        }

        Bukkit.getScheduler().runTask(this, () -> {
            if (warpCatalogService != null) warpCatalogService.refresh();
            if (kitCatalogService != null) kitCatalogService.refresh();
            if (homeCatalogService != null) homeCatalogService.refresh();
            if (tpaCatalogService != null) tpaCatalogService.refresh();
            if (shopGuiPlusHook != null) shopGuiPlusHook.bootstrapIfReady();
            if (economyShopGuiHook != null) economyShopGuiHook.bootstrapIfReady();
            if (backendRouter != null) backendRouter.bootstrapAll();
        });
    }

    private void initWarpsAndKits(PluginManager pluginManager) {
        Map<String, Supplier<WarpProvider>> warpFactories = new LinkedHashMap<>();
        Map<String, Supplier<KitProvider>> kitFactories = new LinkedHashMap<>();

        registerProvider(warpFactories, "Essentials", EssentialsXWarpProvider::new);
        registerProvider(warpFactories, "CMI", CMIWarpProvider::new);
        registerProvider(kitFactories, "Essentials", EssentialsXKitProvider::new);
        registerProvider(kitFactories, "CMI", CMIKitProvider::new);

        warpCatalogService = new WarpCatalogService(getLogger());
        kitCatalogService = new KitCatalogService(getLogger());

        WarpProvider activeWarp = configuration.moduleWarps() ? pickProvider(warpFactories) : null;
        if (activeWarp != null) {
            warpCatalogService.setProvider(activeWarp);
            getLogger().info("Warp provider: " + activeWarp.getProviderId());
        }

        KitProvider activeKit = configuration.moduleKits() ? pickProvider(kitFactories) : null;
        if (activeKit != null) {
            kitCatalogService.setProvider(activeKit);
            getLogger().info("Kit provider: " + activeKit.getProviderId());
        }

        bedrockEssentialsService = new BedrockEssentialsService(
                getLogger(), configuration, warpCatalogService, kitCatalogService, detector
        );

        EssentialsCommandListener commandListener = new EssentialsCommandListener(bedrockEssentialsService);
        if (bedrockHomeService != null) commandListener.setHomeService(bedrockHomeService);
        if (bedrockTpaService != null) commandListener.setTpaService(bedrockTpaService);
        if (configuration.moduleWarps() || configuration.moduleKits()) {
            pluginManager.registerEvents(commandListener, this);
        }
    }

    private void initHomes(PluginManager pluginManager) {
        Map<String, Supplier<HomeProvider>> homeFactories = new LinkedHashMap<>();
        registerProvider(homeFactories, "Essentials", EssentialsXHomeProvider::new);
        registerProvider(homeFactories, "CMI", CMIHomeProvider::new);
        registerProvider(homeFactories, "HuskHomes", HuskHomesHomeProvider::new);

        homeCatalogService = new HomeCatalogService(getLogger());
        HomeProvider activeHome = pickProvider(homeFactories);
        if (activeHome != null) {
            homeCatalogService.setProvider(activeHome);
            getLogger().info("Home provider: " + activeHome.getProviderId());
            bedrockHomeService = new BedrockHomeService(configuration, homeCatalogService, detector);
        }
    }

    private void initTpa(PluginManager pluginManager) {
        Map<String, Supplier<TpaProvider>> tpaFactories = new LinkedHashMap<>();
        registerProvider(tpaFactories, "Essentials", EssentialsXTpaProvider::new);
        registerProvider(tpaFactories, "CMI", CMITpaProvider::new);
        registerProvider(tpaFactories, "HuskHomes", HuskHomesTpaProvider::new);

        tpaCatalogService = new TpaCatalogService(getLogger());
        TpaProvider activeTpa = pickProvider(tpaFactories);
        if (activeTpa != null) {
            tpaCatalogService.setProvider(activeTpa);
            getLogger().info("TPA provider: " + activeTpa.getProviderId());
            bedrockTpaService = new BedrockTpaService(configuration, tpaCatalogService, detector);
        }
    }

    private void initShopGuiPlus(PluginManager pluginManager) {
        shopCatalogService = new ShopGuiCatalogService(getLogger());
        shopGuiPlusHook = new ShopGuiPlusHook(this, shopCatalogService);
        bedrockShopGuiService = new BedrockShopGuiService(
                getLogger(), configuration, shopCatalogService, detector,
                new ReflectiveShopGuiTransactionGateway(getLogger()), soundFeedback
        );

        List<ShopBackend> backends = new ArrayList<>();
        backends.add(new ShopGuiPlusBackend(this, bedrockShopGuiService));

        if (configuration.moduleShopGuiPlus()) {
            pluginManager.registerEvents(new ShopGuiLifecycleListener(shopGuiPlusHook), this);
        }

        if (backendRouter == null) backendRouter = new ShopBackendRouter(backends);
        else backendRouter.addBackends(backends);
    }

    private void registerShopListeners(PluginManager pluginManager) {
        if (backendRouter != null) {
            pluginManager.registerEvents(new ShopGuiCommandListener(backendRouter), this);
            pluginManager.registerEvents(new ShopGuiInventoryListener(backendRouter), this);
        }
    }

    private void initEconomyShopGui(PluginManager pluginManager) {
        if (!isEconomyShopApiAvailable(pluginManager)) {
            getLogger().info("EconomyShopGUI backend disabled because the EconomyShopGUI API is not present.");
            return;
        }

        economyShopCatalogService = new EconomyShopCatalogService(getLogger());
        economyShopGuiHook = new EconomyShopGuiHook(this, economyShopCatalogService);
        bedrockEconomyShopService = new BedrockEconomyShopService(
                getLogger(), configuration, economyShopCatalogService, detector, soundFeedback
        );

        List<ShopBackend> backends = new ArrayList<>();
        backends.add(new EconomyShopGuiBackend(this, bedrockEconomyShopService));

        if (configuration.moduleEconomyShopGui()) {
            pluginManager.registerEvents(new EconomyShopLifecycleListener(economyShopGuiHook), this);
        }

        if (backendRouter == null) backendRouter = new ShopBackendRouter(backends);
        else backendRouter.addBackends(backends);
    }

    private void initMyPet(PluginManager pluginManager) {
        if (!MyPetProvider.isAvailable(this)) {
            getLogger().info("MyPet module enabled but the MyPet API is not present. Skipping.");
            return;
        }
        petCatalogService = new PetCatalogService(this, getLogger());
        petCatalogService.setProvider(new MyPetProvider(this));
        bedrockPetService = new BedrockPetService(getLogger(), configuration, petCatalogService, detector);
        getLogger().info("Pet provider: mypet");

        if (configuration.moduleMyPet()) {
            pluginManager.registerEvents(new PetCommandListener(bedrockPetService), this);
        }
    }

    private <T> void registerProvider(Map<String, Supplier<T>> factories, String pluginName, Supplier<T> factory) {
        if (Bukkit.getPluginManager().getPlugin(pluginName) != null) {
            factories.put(pluginName, factory);
        }
    }

    private boolean isEconomyShopApiAvailable(PluginManager pluginManager) {
        Plugin economyShop = pluginManager.getPlugin("EconomyShopGUI");
        Plugin economyShopPremium = pluginManager.getPlugin("EconomyShopGUI-Premium");
        if (economyShop == null && economyShopPremium == null) return false;
        try {
            Class.forName("me.gypopo.economyshopgui.api.events.PreTransactionEvent", false, getClassLoader());
            Class.forName("me.gypopo.economyshopgui.api.EconomyShopGUIHook", false, getClassLoader());
            return true;
        } catch (Throwable throwable) {
            getLogger().warning("EconomyShopGUI plugin detected but its API classes are unavailable: " + throwable.getClass().getSimpleName());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T pickProvider(Map<String, Supplier<T>> factories) {
        for (Map.Entry<String, Supplier<T>> entry : factories.entrySet()) {
            try {
                T provider = entry.getValue().get();
                if (provider != null) return provider;
            } catch (Throwable e) {
                getLogger().warning("Failed to initialize " + entry.getKey() + " provider: " + e.getClass().getSimpleName());
            }
        }
        return null;
    }

    private BedrockGUIApi getApiSafely() {
        try {
            return BedrockGUIApi.getInstance();
        } catch (IllegalStateException e) {
            getLogger().warning("BedrockGUI-Paper not found. Actions will be unavailable until it loads.");
            return null;
        }
    }

    private void registerActions(BedrockGUIApi api) {
        if (hubService != null) {
            api.registerActionHandler(new OpenEssentialsHubAction(hubService));
        }

        if ((configuration.moduleWarps() || configuration.actionsWarps()) && bedrockEssentialsService != null
                && warpCatalogService != null && warpCatalogService.getProvider() != null) {
            api.registerActionHandler(new HubWarpAction(bedrockEssentialsService));
            api.registerActionHandler(new OpenWarpMainAction(bedrockEssentialsService));
            api.registerActionHandler(new WarpTeleportAction(bedrockEssentialsService));
        }
        if ((configuration.moduleKits() || configuration.actionsKits()) && bedrockEssentialsService != null
                && kitCatalogService != null && kitCatalogService.getProvider() != null) {
            api.registerActionHandler(new HubKitAction(bedrockEssentialsService));
            api.registerActionHandler(new OpenKitMainAction(bedrockEssentialsService));
            api.registerActionHandler(new KitClaimAction(bedrockEssentialsService));
        }
        if ((configuration.moduleHomes() || configuration.actionsHomes()) && bedrockHomeService != null) {
            api.registerActionHandler(new HubHomeAction(bedrockHomeService));
            api.registerActionHandler(new OpenHomeMainAction(bedrockHomeService));
            api.registerActionHandler(new HomeTeleportAction(bedrockHomeService));
            api.registerActionHandler(new HomeSetAction(bedrockHomeService));
            api.registerActionHandler(new HomeDeleteAction(bedrockHomeService));
        }
        if ((configuration.moduleTpa() || configuration.actionsTpa()) && bedrockTpaService != null) {
            api.registerActionHandler(new HubTpaAction(bedrockTpaService));
            api.registerActionHandler(new OpenTpaMainAction(bedrockTpaService));
        }
        if ((configuration.moduleShopGuiPlus() || configuration.actionsShopGuiPlus()) && bedrockShopGuiService != null) {
            api.registerActionHandler(new OpenShopGuiMainAction(bedrockShopGuiService));
            api.registerActionHandler(new OpenShopGuiShopAction(bedrockShopGuiService));
            api.registerActionHandler(new OpenShopGuiItemAction(bedrockShopGuiService));
            api.registerActionHandler(new ExecuteShopGuiTransactionAction(bedrockShopGuiService));
        }
        if ((configuration.moduleEconomyShopGui() || configuration.actionsEconomyShopGui()) && bedrockEconomyShopService != null) {
            api.registerActionHandler(new OpenEconomyShopMainAction(bedrockEconomyShopService));
            api.registerActionHandler(new OpenEconomyShopShopAction(bedrockEconomyShopService));
            api.registerActionHandler(new OpenEconomyShopItemAction(bedrockEconomyShopService));
            api.registerActionHandler(new ExecuteEconomyShopTransactionAction(bedrockEconomyShopService));
        }
        if ((configuration.moduleMyPet() || configuration.actionsMyPet()) && bedrockPetService != null
                && petCatalogService != null && petCatalogService.getProvider() != null) {
            api.registerActionHandler(new HubPetAction(bedrockPetService));
            api.registerActionHandler(new OpenPetListAction(bedrockPetService));
            api.registerActionHandler(new OpenPetShopAction(bedrockPetService));
            api.registerActionHandler(new PetInfoAction(bedrockPetService));
            api.registerActionHandler(new PetCallAction(bedrockPetService));
            api.registerActionHandler(new PetSendAwayAction(bedrockPetService));
            api.registerActionHandler(new OpenPetSkilltreeAction(bedrockPetService));
            api.registerActionHandler(new PetSetSkilltreeAction(bedrockPetService));
            api.registerActionHandler(new BuyPetShopAction(bedrockPetService));
        }
        getLogger().info("Registered essentials addon actions with BedrockGUI API");
    }

    public void reloadConfiguration() {
        configuration = EssentialsAddonConfiguration.load(this);
        if (soundFeedback != null) {
            soundFeedback.configure(
                    configuration.soundsEnabled(),
                    configuration.soundFormOpen(),
                    configuration.soundShopPurchaseSuccess(),
                    configuration.soundShopPurchaseFailed(),
                    configuration.soundVolume(),
                    configuration.soundPitch()
            );
        }
        if (bedrockEssentialsService != null) {
            bedrockEssentialsService = new BedrockEssentialsService(
                    getLogger(), configuration, warpCatalogService, kitCatalogService, detector
            );
        }
        if (bedrockHomeService != null) {
            bedrockHomeService = new BedrockHomeService(configuration, homeCatalogService, detector);
        }
        if (bedrockTpaService != null) {
            bedrockTpaService = new BedrockTpaService(configuration, tpaCatalogService, detector);
        }
        if (bedrockShopGuiService != null) {
            bedrockShopGuiService = new BedrockShopGuiService(
                    getLogger(), configuration, shopCatalogService, detector,
                    new ReflectiveShopGuiTransactionGateway(getLogger()), soundFeedback
            );
        }
        if (bedrockEconomyShopService != null) {
            bedrockEconomyShopService = new BedrockEconomyShopService(
                    getLogger(), configuration, economyShopCatalogService, detector, soundFeedback
            );
        }
        if (hubService != null) {
            hubService = new BedrockHubService(configuration, detector, soundFeedback);
        }
        if (bedrockPetService != null) {
            bedrockPetService = new BedrockPetService(getLogger(), configuration, petCatalogService, detector);
        }
    }

    public WarpCatalogService getWarpCatalogService() { return warpCatalogService; }
    public KitCatalogService getKitCatalogService() { return kitCatalogService; }
    public HomeCatalogService getHomeCatalogService() { return homeCatalogService; }
    public TpaCatalogService getTpaCatalogService() { return tpaCatalogService; }
    public BedrockHubService getHubService() { return hubService; }
}
