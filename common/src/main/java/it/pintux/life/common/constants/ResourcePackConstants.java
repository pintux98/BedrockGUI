package it.pintux.life.common.constants;

/**
 * Constants for resource pack management
 */
public class ResourcePackConstants {
    
    // Configuration keys
    public static final String CONFIG_ENABLED = "resource_packs.enabled";
    public static final String CONFIG_DEFAULT_PACK = "resource_packs.default_pack";
    public static final String CONFIG_FORCE_PACKS = "resource_packs.force_packs";
    public static final String CONFIG_PACKS_SECTION = "resource_packs.packs";
    public static final String CONFIG_MENU_PACKS = "resource_packs.menu_packs";
    
    // Default values
    public static final boolean DEFAULT_ENABLED = false;
    public static final String DEFAULT_PACK_ID = "ui_enhanced";
    public static final boolean DEFAULT_FORCE_PACKS = false;
    
    // File paths and extensions
    public static final String PACK_EXTENSION = ".mcpack";
    public static final String RESOURCE_PACK_FOLDER = "resource_packs";
    public static final String TEMP_FOLDER = "temp";
    public static final String PACK_DIRECTORY = "packs/";
    public static final String CONFIG_DIRECTORY = "config/";
    
    // Timing constants (in milliseconds)
    public static final long PACK_SEND_DELAY = 1000L;
    public static final long PACK_LOAD_TIMEOUT = 30000L;
    public static final long PLAYER_JOIN_DELAY = 2000L;
    
    // Limits
    public static final int MAX_PACKS_PER_PLAYER = 10;
    public static final int MAX_PACK_SIZE_MB = 100;
    public static final int MAX_CONCURRENT_DOWNLOADS = 5;
    
    // Image source prefixes for validation
    public static final String TEXTURE_PREFIX = "textures/";
    public static final String PACK_PREFIX = "pack://";
    public static final String HTTP_PREFIX = "http://";
    public static final String HTTPS_PREFIX = "https://";
    public static final String FILE_PREFIX = "file://";
    
    // Resource pack types
    public static final String TYPE_TEXTURE = "texture";
    public static final String TYPE_BEHAVIOR = "behavior";
    public static final String TYPE_MIXED = "mixed";
    
    // Status constants
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_DOWNLOADING = "downloading";
    public static final String STATUS_LOADED = "loaded";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_REMOVED = "removed";
    
    // Error messages
    public static final String ERROR_PACK_NOT_FOUND = "Resource pack not found";
    public static final String ERROR_PACK_INVALID = "Invalid resource pack format";
    public static final String ERROR_PACK_TOO_LARGE = "Resource pack too large";
    public static final String ERROR_DOWNLOAD_FAILED = "Failed to download resource pack";
    public static final String ERROR_PLAYER_NOT_FOUND = "Player not found";
    public static final String ERROR_NOT_SUPPORTED = "Resource packs not supported on this platform";
    
    // Success messages
    public static final String SUCCESS_PACK_SENT = "Resource pack sent successfully";
    public static final String SUCCESS_PACK_REMOVED = "Resource pack removed successfully";
    public static final String SUCCESS_PACKS_CLEARED = "All resource packs cleared";
    
    // Action types
    public static final String ACTION_SEND = "send";
    public static final String ACTION_REMOVE = "remove";
    public static final String ACTION_CHECK = "check";
    public static final String ACTION_LIST = "list";
    public static final String ACTION_MENU = "menu";
    
    // Default resource pack configurations
    public static final String[] DEFAULT_PACKS = {
        "ui_enhanced",
        "dark_theme", 
        "custom_icons",
        "admin_tools"
    };
    
    // Menu-specific pack mappings
    public static final String MENU_MAIN_HUB = "main_hub";
    public static final String MENU_ADMIN_PANEL = "admin_panel";
    public static final String MENU_SHOP = "shop_interface";
    public static final String MENU_SETTINGS = "settings_menu";
    
    private ResourcePackConstants() {
        // Utility class - prevent instantiation
    }
}