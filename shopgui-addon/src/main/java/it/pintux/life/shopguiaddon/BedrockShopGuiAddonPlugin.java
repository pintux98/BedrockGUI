package it.pintux.life.shopguiaddon;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.shopguiaddon.action.ExecuteShopGuiTransactionAction;
import it.pintux.life.shopguiaddon.action.OpenShopGuiItemAction;
import it.pintux.life.shopguiaddon.action.OpenShopGuiMainAction;
import it.pintux.life.shopguiaddon.action.OpenShopGuiShopAction;
import it.pintux.life.shopguiaddon.config.ShopGuiAddonConfiguration;
import it.pintux.life.shopguiaddon.listener.ShopGuiCommandListener;
import it.pintux.life.shopguiaddon.listener.ShopGuiInventoryListener;
import it.pintux.life.shopguiaddon.listener.ShopGuiLifecycleListener;
import it.pintux.life.shopguiaddon.service.BedrockShopGuiService;
import it.pintux.life.shopguiaddon.service.FloodgateBedrockPlayerDetector;
import it.pintux.life.shopguiaddon.service.ReflectiveShopGuiTransactionGateway;
import it.pintux.life.shopguiaddon.service.ShopGuiCatalogService;
import it.pintux.life.shopguiaddon.service.ShopGuiPlusHook;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class BedrockShopGuiAddonPlugin extends JavaPlugin {
    private ShopGuiAddonConfiguration configuration;
    private ShopGuiCatalogService catalogService;
    private ShopGuiPlusHook shopGuiPlusHook;
    private BedrockShopGuiService bedrockShopGuiService;

    @Override
    public void onEnable() {
        configuration = ShopGuiAddonConfiguration.load(this);
        saveExampleForms();
        catalogService = new ShopGuiCatalogService(getLogger());
        shopGuiPlusHook = new ShopGuiPlusHook(this, catalogService);
        bedrockShopGuiService = new BedrockShopGuiService(
                getLogger(),
                configuration,
                catalogService,
                new FloodgateBedrockPlayerDetector(),
                new ReflectiveShopGuiTransactionGateway(getLogger())
        );

        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new ShopGuiLifecycleListener(shopGuiPlusHook), this);
        pluginManager.registerEvents(new ShopGuiCommandListener(bedrockShopGuiService), this);
        pluginManager.registerEvents(new ShopGuiInventoryListener(this, bedrockShopGuiService), this);

        BedrockGUIApi api = BedrockGUIApi.getInstance();
        if (api != null) {
            api.registerActionHandler(new OpenShopGuiMainAction(bedrockShopGuiService));
            api.registerActionHandler(new OpenShopGuiShopAction(bedrockShopGuiService));
            api.registerActionHandler(new OpenShopGuiItemAction(bedrockShopGuiService));
            api.registerActionHandler(new ExecuteShopGuiTransactionAction(bedrockShopGuiService));
        } else {
            getLogger().warning("BedrockGUI API was not available during addon enable. Actions will be unavailable until BedrockGUI initializes.");
        }

        Bukkit.getScheduler().runTask(this, shopGuiPlusHook::bootstrapIfReady);
    }

    private void saveExampleForms() {
        File formsDirectory = new File(getDataFolder(), "forms");
        if (!formsDirectory.exists() && !formsDirectory.mkdirs()) {
            getLogger().warning("Unable to create addon forms directory at " + formsDirectory.getAbsolutePath());
            return;
        }

        saveResource("forms/shopguiplus_bedrock_hub.yml", false);
        saveResource("forms/shopguiplus_bedrock_shortcuts.yml", false);
    }
}
