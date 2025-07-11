package it.pintux.life.common;

import it.pintux.life.common.form.EnhancedFormMenuUtil;
import it.pintux.life.common.form.FormMenuUtil;
import it.pintux.life.common.platform.PlatformResourcePackManager;
import it.pintux.life.common.utils.FormConfig;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;
import it.pintux.life.common.utils.MessageData;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BedrockGuiAPI {
    
    private static final Logger logger = Logger.getLogger(BedrockGuiAPI.class);
    private static BedrockGuiAPI instance;
    private final Map<String, EnhancedFormMenuUtil> formUtils;
    private PlatformResourcePackManager resourcePackManager;
    private FormConfig config;
    private MessageData messageData;
    
    private BedrockGuiAPI() {
        this.formUtils = new ConcurrentHashMap<>();
        this.resourcePackManager = null; // Will be injected by platform implementation
    }
    
    public static BedrockGuiAPI getInstance() {
        if (instance == null) {
            synchronized (BedrockGuiAPI.class) {
                if (instance == null) {
                    instance = new BedrockGuiAPI();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initializes the API with configuration
     */
    public void initialize(FormConfig config, MessageData messageData) {
        this.config = config;
        this.messageData = messageData;
    }
    
    /**
     * Gets or creates an enhanced form utility for a server/context
     * Note: This method requires platform-specific implementations to be injected
     */
    public EnhancedFormMenuUtil getFormUtil(String serverContext) {
        throw new UnsupportedOperationException("Enhanced form utilities must be created with platform-specific implementations. Use createFormUtil() instead.");
    }
    
    /**
     * Gets the default form utility
     */
    public EnhancedFormMenuUtil getFormUtil() {
        return getFormUtil("default");
    }
    
    /**
     * Opens a form for a player
     */
    public void openForm(FormPlayer player, String menuName, String... args) {
        getFormUtil().openForm(player, menuName, args);
    }
    
    /**
     * Opens a form with a specific resource pack
     */
    public void openFormWithPack(FormPlayer player, String menuName, String packName, String... args) {
        getFormUtil().openFormWithResourcePack(player, menuName, args, packName);
    }
    
    /**
     * Sends a resource pack to a player
     */
    public void sendResourcePack(UUID playerUuid, String packIdentifier) {
        getFormUtil().getResourcePackManager().sendMenuResourcePack(playerUuid, packIdentifier);
    }
    
    /**
     * Handles player join for resource pack loading
     */
    public void onPlayerJoin(UUID playerUuid) {
        for (EnhancedFormMenuUtil util : formUtils.values()) {
            //util.getResourcePackManager().onPlayerJoin(playerUuid);
        }
    }
    
    /**
     * Handles player disconnect for cleanup
     */
    public void onPlayerDisconnect(UUID playerUuid) {
        for (EnhancedFormMenuUtil util : formUtils.values()) {
            //util.getResourcePackManager().onPlayerDisconnect(playerUuid);
        }
    }
    
    /**
     * Checks if resource packs are enabled
     */
    public boolean isResourcePacksEnabled() {
        return !formUtils.isEmpty() && 
               formUtils.values().iterator().next().getResourcePackManager().isEnabled();
    }
    
    /**
     * Gets available themes for a player
     */
    public Set<String> getAvailableThemes(FormPlayer player) {
        return getFormUtil().getAvailableThemes(player);
    }
    
    /**
     * Checks if a player can use resource pack features
     */
    public boolean canUseResourcePacks(FormPlayer player) {
        return getFormUtil().canUseResourcePacks(player);
    }
    
    /**
     * Reloads all resource packs and configurations
     * Note: Reload functionality must be implemented by platform-specific managers
     */
    public void reload() {
        // Platform-specific reload logic should be implemented in the platform modules
        logger.info("Reload requested - platform implementations should handle this");
    }
    
    /**
     * Gets the resource pack manager for a specific context
     */
    public PlatformResourcePackManager getResourcePackManager(String serverContext) {
        return resourcePackManager;
    }
    
    /**
     * Gets the default resource pack manager
     */
    public PlatformResourcePackManager getResourcePackManager() {
        return resourcePackManager;
    }
    
    /**
     * Sets the platform resource pack manager (called by platform implementations)
     */
    public void setResourcePackManager(PlatformResourcePackManager manager) {
        this.resourcePackManager = manager;
    }
    
    /**
     * Creates a form utility without resource pack support (legacy)
     */
    public FormMenuUtil createLegacyFormUtil(FormConfig config, MessageData messageData) {
        return new FormMenuUtil(config, messageData);
    }
    
    /**
     * Registers a custom resource pack
     */
    public void registerResourcePack(String identifier, String fileName) {
        for (EnhancedFormMenuUtil util : formUtils.values()) {
            // This would need to be implemented in ResourcePackManager
            // util.getResourcePackManager().registerPack(identifier, fileName);
        }
    }
    
    /**
     * Gets configuration
     */
    public FormConfig getConfig() {
        return config;
    }
    
    /**
     * Gets message data
     */
    public MessageData getMessageData() {
        return messageData;
    }
}