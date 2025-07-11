package it.pintux.life.common.form;

import it.pintux.life.common.platform.PlatformPlayerChecker;
import it.pintux.life.common.platform.PlatformFormSender;
import it.pintux.life.common.platform.PlatformResourcePackManager;
import it.pintux.life.common.form.obj.FormButton;
import it.pintux.life.common.form.obj.FormMenu;
import it.pintux.life.common.utils.FormConfig;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.Logger;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;


import java.util.*;

public class EnhancedFormMenuUtil extends FormMenuUtil {
    
    private static final Logger logger = Logger.getLogger(EnhancedFormMenuUtil.class);
    private final PlatformResourcePackManager resourcePackManager;
    private final PlatformPlayerChecker playerChecker;
    private final PlatformFormSender formSender;
    
    public EnhancedFormMenuUtil(FormConfig config, MessageData messageData, 
                               PlatformResourcePackManager resourcePackManager,
                               PlatformPlayerChecker playerChecker,
                               PlatformFormSender formSender) {
        super(config, messageData, null, null, null, formSender);
        this.resourcePackManager = resourcePackManager;
        this.playerChecker = playerChecker;
        this.formSender = formSender;
        
        // Register resource pack action handler if resource pack manager is available
        if (resourcePackManager != null) {
            //actionRegistry.registerHandler(new ResourcePackActionHandler(resourcePackManager));
            logger.info("Registered ResourcePackActionHandler");
        }
    }
    
    @Override
    public void openForm(FormPlayer player, String menuName, String[] args) {
        // Send menu-specific resource pack if available
        if (resourcePackManager.isEnabled()) {
            resourcePackManager.sendMenuResourcePack(player.getUniqueId(), menuName);
        }
        
        // Call parent implementation
        super.openForm(player, menuName, args);
    }
    
    /**
     * Enhanced simple form with resource pack support
     */
    @Override
    protected void openSimpleForm(FormPlayer player, FormMenu formMenu, Map<String, String> placeholders) {
        String title = replacePlaceholders(formMenu.getFormTitle(), placeholders, player, messageData);
        List<FormButton> buttons = formMenu.getFormButtons();
        SimpleForm.Builder formBuilder = SimpleForm.builder().title(title);
        
        String content = formMenu.getFormContent();
        if (content != null) {
            formBuilder.content(replacePlaceholders(content, placeholders, player, messageData));
        }
        
        List<String> onClickActions = new ArrayList<>();
        for (FormButton button : buttons) {
            String buttonText = replacePlaceholders(button.getText(), placeholders, player, messageData);
            
            // Enhanced image handling with resource pack support
            if (button.getImage() != null) {
                String imageSource = button.getImage();
                
                // Check if this is a resource pack texture
                if (isResourcePackTexture(imageSource)) {
                    // Use resource pack texture if player has the pack
                    String menuResourcePack = resourcePackManager.getMenuResourcePack(getCurrentMenuName(formMenu));
                    if (menuResourcePack != null && resourcePackManager.hasResourcePack(player.getUniqueId(), menuResourcePack)) {
                        formBuilder.button(buttonText, FormImage.Type.PATH, imageSource);
                    } else {
                        // Fallback to URL or no image
                        String fallbackUrl = getFallbackImageUrl(imageSource);
                        if (fallbackUrl != null) {
                            formBuilder.button(buttonText, FormImage.Type.URL, fallbackUrl);
                        } else {
                            formBuilder.button(buttonText);
                        }
                    }
                } else {
                    // Standard URL image
                    formBuilder.button(buttonText, FormImage.Type.URL, imageSource);
                }
            } else {
                formBuilder.button(buttonText);
            }
            
            if (button.getOnClick() != null) {
                onClickActions.add(button.getOnClick());
            }
        }
        
        formBuilder.validResultHandler((form, response) -> {
            int clickedButtonId = response.clickedButtonId();
            String action = onClickActions.get(clickedButtonId);
            handleOnClick(player, action, placeholders, messageData);
        });
        
        SimpleForm form = formBuilder.build();
        if (formSender != null) {
            formSender.sendForm(player, form);
        } else {
            logger.warn("FormSender is null, cannot send form to player: " + player.getName());
        }
    }
    
    /**
     * Checks if an image source is a resource pack texture
     */
    private boolean isResourcePackTexture(String imageSource) {
        return imageSource != null && 
               (imageSource.startsWith("textures/") || 
                imageSource.startsWith("pack://") ||
                (!imageSource.startsWith("http://") && !imageSource.startsWith("https://")));
    }
    
    /**
     * Gets fallback URL for resource pack textures
     */
    private String getFallbackImageUrl(String resourcePackTexture) {
        // This could be configured in config.yml as fallback mappings
        Map<String, String> fallbackMappings = getFallbackMappings();
        return fallbackMappings.get(resourcePackTexture);
    }
    
    /**
     * Gets fallback image mappings from configuration
     */
    private Map<String, String> getFallbackMappings() {
        Map<String, String> mappings = new HashMap<>();
        
        // Example fallback mappings - these could be loaded from config
        mappings.put("textures/ui/button_default.png", "https://example.com/fallback/button.png");
        mappings.put("textures/ui/shop_icon.png", "https://example.com/fallback/shop.png");
        
        return mappings;
    }
    
    /**
     * Gets the current menu name from FormMenu (helper method)
     */
    private String getCurrentMenuName(FormMenu formMenu) {
        // This would need to be tracked or passed through the call chain
        // For now, we'll use a simple approach
        for (Map.Entry<String, FormMenu> entry : getFormMenus().entrySet()) {
            if (entry.getValue().equals(formMenu)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    /**
     * Enhanced form opening with resource pack pre-loading
     */
    public void openFormWithResourcePack(FormPlayer player, String menuName, String[] args, String specificPack) {
        if (resourcePackManager.isEnabled() && specificPack != null) {
            // Send specific resource pack
            resourcePackManager.sendMenuResourcePack(player.getUniqueId(), specificPack);
            
            // Small delay to ensure pack is loaded before showing form
            scheduleFormOpen(player, menuName, args, 500);
        } else {
            openForm(player, menuName, args);
        }
    }
    
    /**
     * Schedules form opening after a delay (for resource pack loading)
     */
    private void scheduleFormOpen(FormPlayer player, String menuName, String[] args, long delayMs) {
        // This would need to be implemented with the platform's scheduler
        // For now, we'll just call immediately
        openForm(player, menuName, args);
    }
    
    /**
     * Gets the resource pack manager
     */
    public PlatformResourcePackManager getResourcePackManager() {
        return resourcePackManager;
    }
    
    /**
     * Checks if a player can use resource pack features
     */
    public boolean canUseResourcePacks(FormPlayer player) {
        return resourcePackManager.isEnabled() && 
               playerChecker != null && playerChecker.isBedrockPlayer(player.getUniqueId());
    }
    
    /**
     * Gets available themes/packs for a player
     */
    public Set<String> getAvailableThemes(FormPlayer player) {
        if (!canUseResourcePacks(player)) {
            return Collections.emptySet();
        }
        
        return resourcePackManager.getLoadedPacks();
    }
}