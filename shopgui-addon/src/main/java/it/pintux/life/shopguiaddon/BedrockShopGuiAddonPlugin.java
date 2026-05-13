package it.pintux.life.shopguiaddon;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.shopguiaddon.action.ExecuteEconomyShopTransactionAction;
import it.pintux.life.shopguiaddon.action.ExecuteShopGuiTransactionAction;
import it.pintux.life.shopguiaddon.action.OpenEconomyShopItemAction;
import it.pintux.life.shopguiaddon.action.OpenEconomyShopMainAction;
import it.pintux.life.shopguiaddon.action.OpenEconomyShopShopAction;
import it.pintux.life.shopguiaddon.action.OpenShopGuiItemAction;
import it.pintux.life.shopguiaddon.action.OpenShopGuiMainAction;
import it.pintux.life.shopguiaddon.action.OpenShopGuiShopAction;
import it.pintux.life.shopguiaddon.backend.EconomyShopGuiBackend;
import it.pintux.life.shopguiaddon.backend.ShopBackend;
import it.pintux.life.shopguiaddon.backend.ShopBackendRouter;
import it.pintux.life.shopguiaddon.backend.ShopGuiPlusBackend;
import it.pintux.life.shopguiaddon.listener.EconomyShopLifecycleListener;
import it.pintux.life.shopguiaddon.config.ShopGuiAddonConfiguration;
import it.pintux.life.shopguiaddon.listener.ShopGuiCommandListener;
import it.pintux.life.shopguiaddon.listener.ShopGuiInventoryListener;
import it.pintux.life.shopguiaddon.listener.ShopGuiLifecycleListener;
import it.pintux.life.shopguiaddon.service.BedrockEconomyShopService;
import it.pintux.life.shopguiaddon.service.BedrockShopGuiService;
import it.pintux.life.shopguiaddon.service.EconomyShopCatalogService;
import it.pintux.life.shopguiaddon.service.EconomyShopGuiHook;
import it.pintux.life.shopguiaddon.service.FloodgateBedrockPlayerDetector;
import it.pintux.life.shopguiaddon.service.ReflectiveShopGuiTransactionGateway;
import it.pintux.life.shopguiaddon.service.ShopGuiCatalogService;
import it.pintux.life.shopguiaddon.service.ShopGuiPlusHook;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class BedrockShopGuiAddonPlugin extends JavaPlugin {
    private ShopGuiAddonConfiguration configuration;
    private ShopGuiCatalogService catalogService;
    private ShopGuiPlusHook shopGuiPlusHook;
    private BedrockShopGuiService bedrockShopGuiService;
    private EconomyShopCatalogService economyShopCatalogService;
    private EconomyShopGuiHook economyShopGuiHook;
    private BedrockEconomyShopService bedrockEconomyShopService;
    private ShopBackendRouter backendRouter;

    @Override
    public void onEnable() {
        configuration = ShopGuiAddonConfiguration.load(this);
        saveExampleForms();
        catalogService = new ShopGuiCatalogService(getLogger());
        shopGuiPlusHook = new ShopGuiPlusHook(this, catalogService);
        economyShopCatalogService = new EconomyShopCatalogService(getLogger());
        economyShopGuiHook = new EconomyShopGuiHook(this, economyShopCatalogService);
        bedrockShopGuiService = new BedrockShopGuiService(
                getLogger(),
                configuration,
                catalogService,
                new FloodgateBedrockPlayerDetector(),
                new ReflectiveShopGuiTransactionGateway(getLogger())
        );

        PluginManager pluginManager = Bukkit.getPluginManager();
        List<ShopBackend> backends = new ArrayList<>();
        backends.add(new ShopGuiPlusBackend(this, bedrockShopGuiService));
        pluginManager.registerEvents(new ShopGuiLifecycleListener(shopGuiPlusHook), this);

        if (isEconomyShopApiAvailable(pluginManager)) {
            economyShopCatalogService = new EconomyShopCatalogService(getLogger());
            economyShopGuiHook = new EconomyShopGuiHook(this, economyShopCatalogService);
            bedrockEconomyShopService = new BedrockEconomyShopService(
                    getLogger(),
                    configuration,
                    economyShopCatalogService,
                    new FloodgateBedrockPlayerDetector()
            );
            backends.add(new EconomyShopGuiBackend(this, bedrockEconomyShopService));
            pluginManager.registerEvents(new EconomyShopLifecycleListener(economyShopGuiHook), this);
        } else {
            getLogger().info("EconomyShopGUI backend disabled because the EconomyShopGUI API is not present.");
        }

        backendRouter = new ShopBackendRouter(backends);
        pluginManager.registerEvents(new ShopGuiCommandListener(backendRouter), this);
        pluginManager.registerEvents(new ShopGuiInventoryListener(backendRouter), this);

        BedrockGUIApi api = BedrockGUIApi.getInstance();
        if (api != null) {
            api.registerActionHandler(new OpenShopGuiMainAction(bedrockShopGuiService));
            api.registerActionHandler(new OpenShopGuiShopAction(bedrockShopGuiService));
            api.registerActionHandler(new OpenShopGuiItemAction(bedrockShopGuiService));
            api.registerActionHandler(new ExecuteShopGuiTransactionAction(bedrockShopGuiService));
            if (bedrockEconomyShopService != null) {
                api.registerActionHandler(new OpenEconomyShopMainAction(bedrockEconomyShopService));
                api.registerActionHandler(new OpenEconomyShopShopAction(bedrockEconomyShopService));
                api.registerActionHandler(new OpenEconomyShopItemAction(bedrockEconomyShopService));
                api.registerActionHandler(new ExecuteEconomyShopTransactionAction(bedrockEconomyShopService));
            }
        } else {
            getLogger().warning("BedrockGUI API was not available during addon enable. Actions will be unavailable until BedrockGUI initializes.");
        }

        Bukkit.getScheduler().runTask(this, shopGuiPlusHook::bootstrapIfReady);
        if (economyShopGuiHook != null) {
            Bukkit.getScheduler().runTask(this, economyShopGuiHook::bootstrapIfReady);
        }
        backendRouter.bootstrapAll();
    }

    private boolean isEconomyShopApiAvailable(PluginManager pluginManager) {
        Plugin economyShop = pluginManager.getPlugin("EconomyShopGUI");
        Plugin economyShopPremium = pluginManager.getPlugin("EconomyShopGUI-Premium");
        if (economyShop == null && economyShopPremium == null) {
            return false;
        }
        try {
            Class.forName("me.gypopo.economyshopgui.api.events.PreTransactionEvent", false, getClassLoader());
            Class.forName("me.gypopo.economyshopgui.api.EconomyShopGUIHook", false, getClassLoader());
            return true;
        } catch (Throwable throwable) {
            getLogger().warning("EconomyShopGUI plugin detected but its API classes are unavailable: " + throwable.getClass().getSimpleName());
            return false;
        }
    }

    private void saveExampleForms() {
        File formsDirectory = new File(getDataFolder(), "forms");
        if (!formsDirectory.exists() && !formsDirectory.mkdirs()) {
            getLogger().warning("Unable to create addon forms directory at " + formsDirectory.getAbsolutePath());
            return;
        }

        saveResource("forms/shopguiplus_bedrock_hub.yml", false);
        saveResource("forms/shopguiplus_bedrock_shortcuts.yml", false);
        saveResource("forms/economyshopgui_bedrock_hub.yml", false);
        saveResource("forms/economyshopgui_bedrock_shortcuts.yml", false);
    }
}
