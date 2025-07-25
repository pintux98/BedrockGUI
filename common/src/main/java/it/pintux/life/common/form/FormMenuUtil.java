package it.pintux.life.common.form;

import it.pintux.life.common.actions.*;
import it.pintux.life.common.actions.handlers.*;
import it.pintux.life.common.form.obj.FormButton;
import it.pintux.life.common.form.obj.ConditionalButton;
import it.pintux.life.common.form.obj.FormMenu;
import it.pintux.life.common.platform.*;
import it.pintux.life.common.utils.FormConfig;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.PlaceholderUtil;
import it.pintux.life.common.utils.ConditionEvaluator;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import it.pintux.life.common.platform.PlatformFormSender;

import java.util.*;

public class FormMenuUtil {

    private static final Logger logger = Logger.getLogger(FormMenuUtil.class);
    private final Map<String, FormMenu> formMenus;
    private final FormConfig config;
    protected final MessageData messageData;
    private final ActionExecutor actionExecutor;
    private final ActionRegistry actionRegistry;
    private final PlatformCommandExecutor commandExecutor;
    private final PlatformSoundManager soundManager;
    private final PlatformEconomyManager economyManager;
    private final PlatformFormSender formSender;

    public FormMenuUtil(FormConfig config, MessageData messageData) {
        this(config, messageData, null, null, null, null);
    }
    
    public FormMenuUtil(FormConfig config, MessageData messageData, 
                       PlatformCommandExecutor commandExecutor,
                       PlatformSoundManager soundManager,
                       PlatformEconomyManager economyManager,
                       PlatformFormSender formSender) {
        this.config = config;
        formMenus = new HashMap<>();
        this.messageData = messageData;
        this.commandExecutor = commandExecutor;
        this.soundManager = soundManager;
        this.economyManager = economyManager;
        this.formSender = formSender;
        
        // Initialize action system
        this.actionRegistry = ActionRegistry.getInstance();
        this.actionExecutor = new ActionExecutor(actionRegistry);
        
        // Register default action handlers
        registerDefaultActionHandlers();
        
        loadFormMenus();
    }

    /**
     * Registers default action handlers
     */
    private void registerDefaultActionHandlers() {
        // Core action handlers
        actionRegistry.registerHandler(new CommandActionHandler());
        actionRegistry.registerHandler(new OpenFormActionHandler(this));
        actionRegistry.registerHandler(new MessageActionHandler());
        actionRegistry.registerHandler(new CloseActionHandler());
        actionRegistry.registerHandler(new DelayActionHandler());
        
        // Platform-dependent action handlers (only register if platform managers are available)
        if (commandExecutor != null) {
            actionRegistry.registerHandler(new ServerActionHandler(commandExecutor));
            //actionRegistry.registerHandler(new BroadcastActionHandler(commandExecutor));
        }
        
        if (soundManager != null) {
            actionRegistry.registerHandler(new SoundActionHandler(soundManager));
        }
        
        if (economyManager != null) {
            actionRegistry.registerHandler(new EconomyActionHandler(economyManager));
        }
        
        // Advanced action handlers that depend on the action executor
        actionRegistry.registerHandler(new ConditionalActionHandler(actionExecutor));
        actionRegistry.registerHandler(new RandomActionHandler(actionExecutor));
        
        logger.info("Registered " + actionRegistry.size() + " action handlers");
    }

    private void loadFormMenus() {
        for (String key : config.getKeys("menu")) {
            String command = config.getString("menu." + key + ".command");
            String permission = config.getString("menu." + key + ".permission");
            String type = config.getString("menu." + key + ".type", "SIMPLE");
            String title = config.getString("menu." + key + ".title", "Unknown");
            String description = config.getString("menu." + key + ".description");
            List<FormButton> buttons = new ArrayList<>();
            if (type.equalsIgnoreCase("SIMPLE") || type.equalsIgnoreCase("MODAL")) {
                for (String button : config.getKeys("menu." + key + ".buttons")) {
                    String text = config.getString("menu." + key + ".buttons." + button + ".text");
                    String image = config.getString("menu." + key + ".buttons." + button + ".image");
                    String onClick = config.getString("menu." + key + ".buttons." + button + ".onClick");
                    
                    // Check for conditional properties
                    String showCondition = config.getString("menu." + key + ".buttons." + button + ".show_condition");
                    String alternativeText = config.getString("menu." + key + ".buttons." + button + ".alternative_text");
                    String alternativeImage = config.getString("menu." + key + ".buttons." + button + ".alternative_image");
                    String alternativeOnClick = config.getString("menu." + key + ".buttons." + button + ".alternative_onClick");
                    
                    if (showCondition != null || alternativeText != null || alternativeImage != null || alternativeOnClick != null) {
                        // Create conditional button
                        ConditionalButton conditionalButton = new ConditionalButton(text, image, onClick, showCondition);
                        conditionalButton.setAlternativeText(alternativeText);
                        conditionalButton.setAlternativeImage(alternativeImage);
                        conditionalButton.setAlternativeOnClick(alternativeOnClick);
                        
                        // Load conditional property modifications
                        for (String condKey : config.getKeys("menu." + key + ".buttons." + button + ".conditions")) {
                            String condition = config.getString("menu." + key + ".buttons." + button + ".conditions." + condKey + ".condition");
                            String property = config.getString("menu." + key + ".buttons." + button + ".conditions." + condKey + ".property");
                            String value = config.getString("menu." + key + ".buttons." + button + ".conditions." + condKey + ".value");
                            
                            if (condition != null && property != null && value != null) {
                                conditionalButton.addConditionalProperty(condition, property, value);
                            }
                        }
                        
                        buttons.add(conditionalButton);
                    } else {
                        // Create regular button
                        buttons.add(new FormButton(text, image, onClick));
                    }
                }
                if (type.equalsIgnoreCase("MODAL")) {
                    if (buttons.size() != 2) {
                        logger.warn("Modal's must only have 2 buttons! Please modify menu." + key);
                        continue;
                    }
                }
            }

            Map<String, Map<String, Object>> components = new HashMap<>();
            if (type.equalsIgnoreCase("CUSTOM")) {
                for (String componentKey : config.getKeys("menu." + key + ".components")) {
                    Map<String, Object> component = config.getValues("menu." + key + ".components." + componentKey);
                    components.put(componentKey, component);
                }
            }

            List<String> globalActions = config.getStringList("menu." + key + ".global_actions");

            FormMenu menu = new FormMenu(command, permission, title, description, type, buttons, components, globalActions);
            formMenus.put(key.toLowerCase(), menu);
            logger.info("Loaded form menu: " + key + " type: " + type);
        }
    }

    public void openForm(FormPlayer player, String menuName, String[] args) {
        FormMenu menu = formMenus.get(menuName.toLowerCase());
        if (menu == null) {
            player.sendMessage(messageData.getValue(MessageData.MENU_NOT_FOUND, null, null));
            return;
        }

        if (menu.getPermission() != null && !player.hasPermission(menu.getPermission())) {
            player.sendMessage(messageData.getValue(MessageData.MENU_NOPEX, null, null));
            return;
        }

        if (menu.getFormCommand() != null && !validateCommandArguments(menu.getFormCommand(), args, player)) {
            return;
        }

        String type = menu.getFormType();

        Map<String, String> placeholders = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            placeholders.put(String.valueOf(i + 1), args[i]);
        }

        switch (type.toUpperCase()) {
            case "MODAL":
                openModalForm(player, menu, placeholders);
                break;
            case "SIMPLE":
                openSimpleForm(player, menu, placeholders);
                break;
            case "CUSTOM":
                openCustomForm(player, menu, placeholders, messageData);
                break;
        }
    }

    private void openModalForm(FormPlayer player, FormMenu formMenu, Map<String, String> placeholders) {
        String title = replacePlaceholders(formMenu.getFormTitle(), placeholders, player, messageData);

        List<FormButton> buttons = formMenu.getFormButtons();
        FormButton b1 = buttons.get(0);
        FormButton b2 = buttons.get(1);

        ModalForm.Builder formBuilder = ModalForm.builder()
                .title(title);

        String content = formMenu.getFormContent();
        if (content != null) {
            formBuilder.content(replacePlaceholders(content, placeholders, player, messageData));
        }
        
        // Create action context for condition evaluation
        ActionContext context = ActionContext.builder()
            .placeholders(placeholders)
            .placeholder("player", player.getName())
            .placeholder("uuid", player.getUniqueId().toString())
            .build();
        
        // Get effective button properties
        String button1Text, button2Text;
        String button1OnClick, button2OnClick;
        
        // Check if buttons should be shown based on conditions
        if (b1 instanceof ConditionalButton) {
            ConditionalButton cb1 = (ConditionalButton) b1;
            // Check if button has a show condition and evaluate it
            if (cb1.hasShowCondition() && 
                !ConditionEvaluator.evaluateCondition(player, cb1.getShowCondition(), context, messageData)) {
                logger.warn("Modal form button 1 has a show condition that evaluated to false. This is not fully supported in modal forms.");
                // For modal forms, we can't hide buttons, so we'll still show it but with modified properties
            }
            button1Text = getEffectiveButtonText(cb1, player, context, placeholders, messageData);
            button1OnClick = getEffectiveButtonOnClick(cb1, player, context);
        } else {
            button1Text = replacePlaceholders(b1.getText(), placeholders, player, messageData);
            button1OnClick = b1.getOnClick();
        }
        
        if (b2 instanceof ConditionalButton) {
            ConditionalButton cb2 = (ConditionalButton) b2;
            // Check if button has a show condition and evaluate it
            if (cb2.hasShowCondition() && 
                !ConditionEvaluator.evaluateCondition(player, cb2.getShowCondition(), context, messageData)) {
                logger.warn("Modal form button 2 has a show condition that evaluated to false. This is not fully supported in modal forms.");
                // For modal forms, we can't hide buttons, so we'll still show it but with modified properties
            }
            button2Text = getEffectiveButtonText(cb2, player, context, placeholders, messageData);
            button2OnClick = getEffectiveButtonOnClick(cb2, player, context);
        } else {
            button2Text = replacePlaceholders(b2.getText(), placeholders, player, messageData);
            button2OnClick = b2.getOnClick();
        }

        formBuilder
                .button1(button1Text)
                .button2(button2Text)
                .validResultHandler((formResponse, modalResponse) -> {
                    if (modalResponse.clickedButtonId() == 0) {
                        if (button1OnClick != null) {
                            handleOnClick(player, button1OnClick, placeholders, messageData);
                        }
                    } else {
                        if (button2OnClick != null) {
                            handleOnClick(player, button2OnClick, placeholders, messageData);
                        }
                    }
                })
                .build();

        if (formSender != null) {
            formSender.sendForm(player, formBuilder.build());
        } else {
            logger.warn("FormSender is null, cannot send modal form to player: " + player.getName());
        }
    }

    protected void openSimpleForm(FormPlayer player, FormMenu formMenu, Map<String, String> placeholders) {
        String title = replacePlaceholders(formMenu.getFormTitle(), placeholders, player, messageData);

        List<FormButton> buttons = formMenu.getFormButtons();
        SimpleForm.Builder formBuilder = SimpleForm.builder().title(title);

        String content = formMenu.getFormContent();
        if (content != null) {
            formBuilder.content(replacePlaceholders(content, placeholders, player, messageData));
        }

        List<String> onClickActions = new ArrayList<>();
        
        // Create action context for condition evaluation
        ActionContext context = ActionContext.builder()
            .placeholders(placeholders)
            .placeholder("player", player.getName())
            .placeholder("uuid", player.getUniqueId().toString())
            .build();
        
        for (FormButton button : buttons) {
            // Handle conditional buttons
            if (button instanceof ConditionalButton) {
                ConditionalButton conditionalButton = (ConditionalButton) button;
                
                // Check if button has a show condition and evaluate it
                if (conditionalButton.hasShowCondition() && 
                    !ConditionEvaluator.evaluateCondition(player, conditionalButton.getShowCondition(), context, messageData)) {
                    // Button should be hidden, skip it
                    continue;
                }
                
                // Evaluate conditional properties and get effective values
                String effectiveText = getEffectiveButtonText(conditionalButton, player, context, placeholders, messageData);
                String effectiveImage = getEffectiveButtonImage(conditionalButton, player, context);
                String effectiveOnClick = getEffectiveButtonOnClick(conditionalButton, player, context);
                
                // Add button with effective properties
                if (effectiveImage != null) {
                    formBuilder.button(effectiveText, FormImage.Type.URL, effectiveImage);
                } else {
                    formBuilder.button(effectiveText);
                }
                
                if (effectiveOnClick != null) {
                    onClickActions.add(effectiveOnClick);
                }
            } else {
                // Handle regular buttons
                String buttonText = replacePlaceholders(button.getText(), placeholders, player, messageData);
                if (button.getImage() != null) {
                    formBuilder.button(buttonText, FormImage.Type.URL, button.getImage());
                } else {
                    formBuilder.button(buttonText);
                }
                if (button.getOnClick() != null) {
                    onClickActions.add(button.getOnClick());
                }
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
            logger.warn("FormSender is null, cannot send simple form to player: " + player.getName());
        }
    }

    private void openCustomForm(FormPlayer player, FormMenu formMenu, Map<String, String> placeholders, MessageData messageData) {
        String title = replacePlaceholders(formMenu.getFormTitle(), placeholders, player, messageData);
        CustomForm.Builder formBuilder = CustomForm.builder().title(title);

        // Create action context for condition evaluation
        ActionContext context = ActionContext.builder()
            .placeholders(placeholders)
            .placeholder("player", player.getName())
            .placeholder("uuid", player.getUniqueId().toString())
            .build();

        Map<Integer, String> componentActions = new HashMap<>();
        Map<String, Object> componentResults = new HashMap<>();
        int[] componentIndex = {0};

        for (String componentKey : formMenu.getComponents().keySet()) {
            Map<String, Object> component = formMenu.getComponents().get(componentKey);
            String type = (String) component.get("type");

            switch (type.toLowerCase()) {
                case "input":
                    String inputText = replacePlaceholders((String) component.get("text"), placeholders, player, messageData);
                    String placeholder = (String) component.get("placeholder");
                    String defaultValue = (String) component.get("default");
                    formBuilder.input(inputText, placeholder, defaultValue);
                    componentActions.put(componentIndex[0], (String) component.get("action"));
                    componentResults.put(componentKey, "");
                    break;
                case "slider":
                    String sliderText = replacePlaceholders((String) component.get("text"), placeholders, player, messageData);
                    int min = (int) component.get("min");
                    int max = (int) component.get("max");
                    int step = (int) component.get("step");
                    int defaultSlider = (int) component.get("default");
                    formBuilder.slider(sliderText, min, max, step, defaultSlider);
                    componentActions.put(componentIndex[0], (String) component.get("action"));
                    componentResults.put(componentKey, 0);
                    break;
                case "dropdown":
                    String dropdownText = replacePlaceholders((String) component.get("text"), placeholders, player, messageData);
                    @SuppressWarnings("unchecked")
                    List<String> options = (List<String>) component.get("options");
                    int defaultDropdown = (int) component.get("default");
                    formBuilder.dropdown(dropdownText, options, defaultDropdown);
                    componentActions.put(componentIndex[0], (String) component.get("action"));
                    componentResults.put(componentKey, "");
                    break;
                case "toggle":
                    String toggleText = replacePlaceholders((String) component.get("text"), placeholders, player, messageData);
                    boolean defaultToggle = (boolean) component.get("default");
                    formBuilder.toggle(toggleText, defaultToggle);
                    componentActions.put(componentIndex[0], (String) component.get("action"));
                    componentResults.put(componentKey, false);
                    break;
            }

            componentIndex[0]++;
        }

        formBuilder.validResultHandler((formResponse, customFormResponse) -> {
            componentIndex[0] = 0;

            for (String componentKey : formMenu.getComponents().keySet()) {
                Map<String, Object> component = formMenu.getComponents().get(componentKey);
                String type = (String) component.get("type");
                String action = componentActions.get(componentIndex[0]);

                String result = "";
                switch (type.toLowerCase()) {
                    case "input":
                        result = customFormResponse.asInput(componentIndex[0]);
                        componentResults.put(componentKey, result);
                        break;
                    case "slider":
                        int sliderResult = (int) customFormResponse.asSlider(componentIndex[0]);
                        result = String.valueOf(sliderResult);
                        componentResults.put(componentKey, sliderResult);
                        break;
                    case "dropdown":
                        int dropdownResult = customFormResponse.asDropdown(componentIndex[0]);
                        @SuppressWarnings("unchecked")
                        List<String> options = (List<String>) component.get("options");
                        result = options.get(dropdownResult);
                        componentResults.put(componentKey, result);
                        break;
                    case "toggle":
                        boolean toggleResult = customFormResponse.asToggle(componentIndex[0]);
                        result = String.valueOf(toggleResult);
                        componentResults.put(componentKey, toggleResult);
                        break;
                }

                if (action != null) {
                    handleCustomAction(player, action, result);
                }

                componentIndex[0]++;
            }

            List<String> globalActions = formMenu.getGlobalActions();
            if (globalActions != null) {
                for (String globalAction : globalActions) {
                    String finalAction = globalAction;
                    for (Map.Entry<String, Object> entry : componentResults.entrySet()) {
                        finalAction = finalAction.replace("$" + entry.getKey(), entry.getValue().toString());
                    }

                    handleCustomAction(player, finalAction, null);
                }
            }
        });

        CustomForm form = formBuilder.build();
        if (formSender != null) {
            formSender.sendForm(player, form);
        } else {
            logger.warn("FormSender is null, cannot send custom form to player: " + player.getName());
        }
    }

    protected void handleOnClick(FormPlayer player, String onClickAction, Map<String, String> placeholders, MessageData messageData) {
        if (onClickAction == null || onClickAction.trim().isEmpty()) {
            logger.warn("Empty onClick action for player: " + player.getName());
            return;
        }
        
        onClickAction = replacePlaceholders(onClickAction.trim().replaceAll("\\s+", " "), placeholders, player, messageData);

        // Create action context
        ActionContext context = ActionContext.builder()
            .placeholders(placeholders)
            .placeholder("player", player.getName())
            .placeholder("uuid", player.getUniqueId().toString())
            .build();

        // Parse and execute action
        ActionExecutor.Action action = actionExecutor.parseAction(onClickAction);
        if (action == null) {
            logger.warn("Failed to parse onClick action: " + onClickAction);
            player.sendMessage("Invalid action format");
            return;
        }

        ActionResult result = actionExecutor.executeAction(player, action.getType(), action.getValue(), context);
        logger.warn(result.toString());
        if (result.isFailure()) {
            logger.warn("Action execution failed for player " + player.getName() + ": " + result.getMessage());
            if (result.getMessage() != null) {
                player.sendMessage("Action failed: " + result.getMessage());
            }
        } else {
            logger.debug("Successfully executed action for player " + player.getName() + ": " + onClickAction);
        }
    }

    private boolean validateCommandArguments(String command, String[] args, FormPlayer player) {
        if (command == null || command.isEmpty()) {
            return true;
        }
        int requiredArgs = countPlaceholders(command);
        if (args.length < requiredArgs) {
            player.sendMessage(messageData.getValue(MessageData.MENU_ARGS, Map.of("args", requiredArgs), player));
            return false;
        }
        return true;
    }


    private void handleCustomAction(FormPlayer player, String action, String value) {
        if (action == null || action.trim().isEmpty()) {
            logger.warn("Empty custom action for player: " + player.getName());
            return;
        }
        
        // Replace $1 placeholder with the component value
        if (value != null) {
            action = action.replace("$1", value);
        }
        
        // Create action context with component value
        ActionContext.Builder contextBuilder = ActionContext.builder()
            .placeholder("player", player.getName())
            .placeholder("uuid", player.getUniqueId().toString());
            
        if (value != null) {
            contextBuilder.placeholder("value", value)
                         .placeholder("1", value); // Support both {value} and {1}
        }
        
        ActionContext context = contextBuilder.build();
        
        // Parse and execute action
        ActionExecutor.Action parsedAction = actionExecutor.parseAction(action);
        if (parsedAction == null) {
            logger.warn("Failed to parse custom action: " + action);
            player.sendMessage("Invalid action format");
            return;
        }
        
        ActionResult result = actionExecutor.executeAction(player, parsedAction.getType(), parsedAction.getValue(), context);
        
        if (result.isFailure()) {
            logger.warn("Custom action execution failed for player " + player.getName() + ": " + result.getMessage());
            if (result.getMessage() != null) {
                player.sendMessage("Action failed: " + result.getMessage());
            }
        } else {
            logger.debug("Successfully executed custom action for player " + player.getName() + ": " + action);
        }
    }

    private int countPlaceholders(String command) {
        return PlaceholderUtil.countNumberedPlaceholders(command);
    }

    protected String replacePlaceholders(String text, Map<String, String> placeholders, FormPlayer player, MessageData messageData) {
        return PlaceholderUtil.processPlaceholders(text, placeholders, player, messageData);
    }

    /**
     * Checks if a menu exists
     * @param menuName the menu name to check
     * @return true if menu exists, false otherwise
     */
    public boolean hasMenu(String menuName) {
        if (menuName == null) {
            return false;
        }
        return formMenus.containsKey(menuName.toLowerCase());
    }
    
    /**
     * Opens a menu for a player
     * @param player the player
     * @param menuName the menu name
     */
    public void openMenu(FormPlayer player, String menuName) {
        openForm(player, menuName, new String[0]);
    }
    
    /**
     * Registers a custom action handler
     * @param handler the action handler to register
     */
    public void registerActionHandler(ActionHandler handler) {
        actionRegistry.registerHandler(handler);
        logger.info("Registered custom action handler: " + handler.getActionType());
    }
    
    /**
     * Unregisters an action handler
     * @param actionType the action type to unregister
     * @return true if handler was removed, false if not found
     */
    public boolean unregisterActionHandler(String actionType) {
        boolean removed = actionRegistry.unregisterHandler(actionType);
        if (removed) {
            logger.info("Unregistered action handler: " + actionType);
        }
        return removed;
    }
    
    /**
     * Gets the action registry for advanced usage
     * @return the action registry
     */
    public ActionRegistry getActionRegistry() {
        return actionRegistry;
    }
    
    /**
     * Gets the action executor for advanced usage
     * @return the action executor
     */
    public ActionExecutor getActionExecutor() {
        return actionExecutor;
    }
    
    /**
     * Shuts down the FormMenuUtil and cleans up resources
     */
    public void shutdown() {
        if (actionExecutor != null) {
            actionExecutor.shutdown();
        }
        logger.info("FormMenuUtil shutdown completed");
    }

    public Map<String, FormMenu> getFormMenus() {
        return formMenus;
    }
    
    /**
     * Gets the effective text for a conditional button based on conditions
     */
    private String getEffectiveButtonText(FormButton button, FormPlayer player, ActionContext context, Map<String, String> placeholders, MessageData messageData) {
        if (button instanceof ConditionalButton) {
            ConditionalButton conditionalButton = (ConditionalButton) button;
            
            // Check conditional properties for text modifications
            String matchedCondition = null;
            for (Map.Entry<String, ConditionalButton.ConditionalProperty> entry : conditionalButton.getConditionalProperties().entrySet()) {
                String condition = entry.getKey();
                ConditionalButton.ConditionalProperty property = entry.getValue();
                
                if ("text".equals(property.getProperty()) && ConditionEvaluator.evaluateCondition(player, condition, context, messageData)) {
                    matchedCondition = condition;
                    break;
                }
            }
            
            // Get effective text using the matched condition
            String effectiveText = conditionalButton.getEffectiveText(matchedCondition);
            return replacePlaceholders(effectiveText, placeholders, player, messageData);
        }
        
        // Use default text for regular buttons
        return replacePlaceholders(button.getText(), placeholders, player, messageData);
    }
    
    /**
     * Gets the effective image for a conditional button based on conditions
     */
    private String getEffectiveButtonImage(FormButton button, FormPlayer player, ActionContext context) {
        if (button instanceof ConditionalButton) {
            ConditionalButton conditionalButton = (ConditionalButton) button;
            
            // Check conditional properties for image modifications
            String matchedCondition = null;
            for (Map.Entry<String, ConditionalButton.ConditionalProperty> entry : conditionalButton.getConditionalProperties().entrySet()) {
                String condition = entry.getKey();
                ConditionalButton.ConditionalProperty property = entry.getValue();
                
                if ("image".equals(property.getProperty()) && ConditionEvaluator.evaluateCondition(player, condition, context, messageData)) {
                    matchedCondition = condition;
                    break;
                }
            }
            
            // Get effective image using the matched condition
            return conditionalButton.getEffectiveImage(matchedCondition);
        }
        
        // Use default image for regular buttons
        return button.getImage();
    }
    
    /**
     * Gets the effective onClick action for a conditional button based on conditions
     */
    private String getEffectiveButtonOnClick(FormButton button, FormPlayer player, ActionContext context) {
        if (button instanceof ConditionalButton) {
            ConditionalButton conditionalButton = (ConditionalButton) button;
            
            // Check conditional properties for onClick modifications
            String matchedCondition = null;
            for (Map.Entry<String, ConditionalButton.ConditionalProperty> entry : conditionalButton.getConditionalProperties().entrySet()) {
                String condition = entry.getKey();
                ConditionalButton.ConditionalProperty property = entry.getValue();
                
                if ("onClick".equals(property.getProperty()) && ConditionEvaluator.evaluateCondition(player, condition, context, messageData)) {
                    matchedCondition = condition;
                    break;
                }
            }
            
            // Get effective onClick using the matched condition
            return conditionalButton.getEffectiveOnClick(matchedCondition);
        }
        
        // Use default onClick for regular buttons
        return button.getOnClick();
    }
}