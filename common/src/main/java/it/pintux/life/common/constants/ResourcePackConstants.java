package it.pintux.life.common.constants;

/**
 * Constants for resource pack management
 */
public final class ResourcePackConstants {
    
    // Configuration keys
    public static final String CONFIG_ENABLED = "resource_packs.enabled";
    public static final String CONFIG_DEFAULT_PACK = "resource_packs.default_pack";
    public static final String CONFIG_PER_MENU_PACKS = "resource_packs.per_menu_packs";
    
    // Default values
    public static final String DEFAULT_PACK_NAME = "bedrockgui_default.mcpack";
    public static final String DEFAULT_PACK_IDENTIFIER = "default";
    public static final boolean DEFAULT_ENABLED = false;
    
    // File paths
    public static final String PACK_DIRECTORY = "plugins/BedrockGUI/packs/";
    public static final String CONFIG_DIRECTORY = "plugins/BedrockGUI/";
    
    // Timing constants
    public static final long PACK_LOAD_DELAY_MS = 500L;
    public static final long PLAYER_JOIN_DELAY_TICKS = 20L; // 1 second
    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final long RETRY_DELAY_MS = 1000L;
    
    // Limits
    public static final int MAX_PACKS_PER_PLAYER = 10;
    public static final long MAX_PACK_SIZE_BYTES = 100 * 1024 * 1024; // 100MB
    
    // Image handling
    public static final String TEXTURE_PREFIX = "textures/";
    public static final String PACK_PREFIX = "pack://";
    public static final String HTTP_PREFIX = "http://";
    public static final String HTTPS_PREFIX = "https://";
    
    private ResourcePackConstants() {
        // Utility class - prevent instantiation
    }
}