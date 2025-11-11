package it.pintux.life.common.form;
import it.pintux.life.common.actions.ActionSystem;

import it.pintux.life.common.actions.*;
import it.pintux.life.common.actions.handlers.*;
import it.pintux.life.common.form.obj.ConditionalButton;
import it.pintux.life.common.form.obj.FormButton;
import it.pintux.life.common.form.obj.FormMenu;
import it.pintux.life.common.platform.*;
import it.pintux.life.common.utils.ConditionEvaluator;
import it.pintux.life.common.utils.FormConfig;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.PlaceholderUtil;
import it.pintux.life.common.utils.ConfigValidator;
import it.pintux.life.common.utils.ErrorHandlingUtil;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;

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
    private final PlatformTitleManager titleManager;
    private final PlatformPluginManager pluginManager;
    private final PlatformPlayerManager playerManager;


    private DelayActionHandler delayActionHandler;

    public FormConfig getConfig() {
        return config;
    }

    public FormMenuUtil(FormConfig config, MessageData messageData,
                       PlatformCommandExecutor commandExecutor,
                       PlatformSoundManager soundManager,
                       PlatformEconomyManager economyManager,
                       PlatformFormSender formSender,
                       PlatformTitleManager titleManager,
                       PlatformPluginManager pluginManager,
                       PlatformPlayerManager playerManager) {
        this.config = config;
        formMenus = new HashMap<>();
        this.messageData = messageData;
        this.commandExecutor = commandExecutor;
        this.soundManager = soundManager;
        this.economyManager = economyManager;
        this.formSender = formSender;
        this.titleManager = titleManager;
        this.pluginManager = pluginManager;
        this.playerManager = playerManager;


        this.actionRegistry = ActionRegistry.getInstance();
        this.actionExecutor = new ActionExecutor(actionRegistry);


        if (pluginManager != null) {
            ConditionEvaluator.setPluginManager(pluginManager);
        }


        registerDefaultActionHandlers();

        loadFormMenus();


        validateConfiguration();
    }


    private void registerDefaultActionHandlers() {

        actionRegistry.registerHandler(new CommandActionHandler());
        actionRegistry.registerHandler(new OpenFormActionHandler(this));
        actionRegistry.registerHandler(new MessageActionHandler(playerManager));
        this.delayActionHandler = new DelayActionHandler(actionExecutor);
        actionRegistry.registerHandler(delayActionHandler);


        if (commandExecutor != null) {
            actionRegistry.registerHandler(new ServerActionHandler(commandExecutor));
            actionRegistry.registerHandler(new BroadcastActionHandler(commandExecutor));
            actionRegistry.registerHandler(new InventoryActionHandler(commandExecutor));
        }

        if (soundManager != null) {
            actionRegistry.registerHandler(new SoundActionHandler(soundManager));
        }

        if (economyManager != null) {
            actionRegistry.registerHandler(new EconomyActionHandler(economyManager));
        }

        if (titleManager != null && titleManager.isSupported()) {
            actionRegistry.registerHandler(new TitleActionHandler(titleManager));
            actionRegistry.registerHandler(new ActionBarActionHandler(titleManager));
        }


        actionRegistry.registerHandler(new ConditionalActionHandler(actionExecutor));
        actionRegistry.registerHandler(new RandomActionHandler(actionExecutor));

        actionRegistry.registerHandler(new OpenUrlActionHandler(playerManager));

        logger.info("Registered " + actionRegistry.size() + " action handlers");
    }

    private void loadFormMenus() {
        for (String key : config.getKeys("forms")) {
            String command = config.getString("forms." + key + ".command");
            String commandIntercept = config.getString("forms." + key + ".command_intercept");
            String permission = config.getString("forms." + key + ".permission");
            String type = config.getString("forms." + key + ".type", "SIMPLE");
            String title = config.getString("forms." + key + ".title", "Unknown");
            String description = config.getString("forms." + key + ".description");
            List<FormButton> buttons = new ArrayList<>();
            if (type.equalsIgnoreCase("SIMPLE") || type.equalsIgnoreCase("MODAL")) {
                for (String button : config.getKeys("forms." + key + ".buttons")) {
                    String text = config.getString("forms." + key + ".buttons." + button + ".text");
                    String image = config.getString("forms." + key + ".buttons." + button + ".image");


                    String onClick = null;
                    try {
                        List<String> onClickList = config.getStringList("forms." + key + ".buttons." + button + ".onClick");
                        if (onClickList != null && !onClickList.isEmpty()) {

                            onClick = "[" + String.join(", ", onClickList) + "]";
                        }
                    } catch (Exception e) {

                        onClick = config.getString("forms." + key + ".buttons." + button + ".onClick");
                    }


                    if (onClick == null) {
                        onClick = config.getString("forms." + key + ".buttons." + button + ".onClick");
                    }


                    String priorityStr = config.getString("forms." + key + ".buttons." + button + ".priority");
                    Integer priority = null;
                    if (priorityStr != null) {
                        try {
                            priority = Integer.parseInt(priorityStr);
                        } catch (NumberFormatException e) {

                        }
                    }
                    String viewRequirement = config.getString("forms." + key + ".buttons." + button + ".view_requirement");


                    String showCondition = config.getString("forms." + key + ".buttons." + button + ".show_condition");
                    String alternativeText = config.getString("forms." + key + ".buttons." + button + ".alternative_text");
                    String alternativeImage = config.getString("forms." + key + ".buttons." + button + ".alternative_image");
                    String alternativeOnClick = config.getString("forms." + key + ".buttons." + button + ".alternative_onClick");


                    if (showCondition != null || alternativeText != null || alternativeImage != null || alternativeOnClick != null) {

                        ConditionalButton conditionalButton = new ConditionalButton(text, image, onClick, showCondition);
                        conditionalButton.setAlternativeText(alternativeText);
                        conditionalButton.setAlternativeImage(alternativeImage);
                        conditionalButton.setAlternativeOnClick(alternativeOnClick);


                        if (onClick != null && !onClick.trim().isEmpty()) {
                            ActionSystem.ActionDefinition actionDef = convertOnClickToActionDefinition(onClick);
                            conditionalButton.setAction(actionDef);
                        }


                        for (String condKey : config.getKeys("forms." + key + ".buttons." + button + ".conditions")) {
                            String condition = config.getString("forms." + key + ".buttons." + button + ".conditions." + condKey + ".condition");
                            String property = config.getString("forms." + key + ".buttons." + button + ".conditions." + condKey + ".property");
                            String value = config.getString("forms." + key + ".buttons." + button + ".conditions." + condKey + ".value");

                            if (condition != null && property != null && value != null) {
                                conditionalButton.addConditionalProperty(condition, property, value);
                            }
                        }

                        buttons.add(conditionalButton);
                    } else {

                        FormButton formButton = new FormButton(text, image, onClick);


                        if (onClick != null && !onClick.trim().isEmpty()) {
                            ActionSystem.ActionDefinition actionDef = convertOnClickToActionDefinition(onClick);
                            formButton.setAction(actionDef);
                        }

                        buttons.add(formButton);
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
                for (String componentKey : config.getKeys("forms." + key + ".components")) {
                    Map<String, Object> component = config.getValues("forms." + key + ".components." + componentKey);
                    components.put(componentKey, component);
                }
            }

            List<String> globalActions = config.getStringList("forms." + key + ".global_actions");

            FormMenu menu = new FormMenu(command, commandIntercept, permission, title, description, type, buttons, components, globalActions);
            formMenus.put(key.toLowerCase(), menu);
            logger.info("Loaded form: " + key + " type: " + type);
        }
    }


    public void reloadFormMenus() {
        logger.info("Reloading form menus from configuration...");


        formMenus.clear();


        loadFormMenus();


        validateConfiguration();

        logger.info("Successfully reloaded " + formMenus.size() + " form menus");
    }

    /**
     * Updates the MessageData instance used by this FormMenuUtil.
     * This is called during reload to ensure the FormMenuUtil uses the latest message configuration.
     * 
     * @param newMessageData The new MessageData instance with reloaded configuration
     */
    public void updateMessageData(MessageData newMessageData) {
        if (newMessageData != null) {
            try {
                // Use reflection to update the final messageData field
                java.lang.reflect.Field messageDataField = this.getClass().getDeclaredField("messageData");
                messageDataField.setAccessible(true);
                
                // Remove the final modifier temporarily
                java.lang.reflect.Field modifiersField = java.lang.reflect.Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(messageDataField, messageDataField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
                
                // Set the new MessageData instance
                messageDataField.set(this, newMessageData);
                
                logger.info("MessageData updated successfully in FormMenuUtil during reload");
            } catch (Exception e) {
                logger.warn("Failed to update MessageData in FormMenuUtil: " + e.getMessage());
            }
        }
    }

    /**
     * Updates the FormConfig instance used by this FormMenuUtil.
     * This allows reloading fresh configuration without recreating the FormMenuUtil.
     */
    public void updateFormConfig(FormConfig newConfig) {
        if (newConfig != null) {
            try {
                java.lang.reflect.Field configField = this.getClass().getDeclaredField("config");
                configField.setAccessible(true);
                java.lang.reflect.Field modifiersField = java.lang.reflect.Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(configField, configField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
                configField.set(this, newConfig);
                logger.info("FormConfig updated successfully in FormMenuUtil during reload");
            } catch (Exception e) {
                logger.warn("Failed to update FormConfig in FormMenuUtil: " + e.getMessage());
            }
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


        ActionSystem.ActionContext context = PlaceholderUtil.createContextWithBuiltinPlaceholders(player, placeholders, messageData);


        String button1Text, button2Text;
        String button1OnClick, button2OnClick;


        if (b1 instanceof ConditionalButton) {
            ConditionalButton cb1 = (ConditionalButton) b1;

            if (cb1.hasShowCondition() &&
                !ConditionEvaluator.evaluateCondition(player, cb1.getShowCondition(), context, messageData)) {
                logger.warn("Modal form button 1 has a show condition that evaluated to false. This is not fully supported in modal forms.");

            }
            button1Text = getEffectiveButtonText(cb1, player, context, placeholders, messageData);
            button1OnClick = getEffectiveButtonOnClick(cb1, player, context);
        } else {
            button1Text = replacePlaceholders(b1.getText(), placeholders, player, messageData);
            button1OnClick = b1.getOnClick();
        }

        if (b2 instanceof ConditionalButton) {
            ConditionalButton cb2 = (ConditionalButton) b2;

            if (cb2.hasShowCondition() &&
                !ConditionEvaluator.evaluateCondition(player, cb2.getShowCondition(), context, messageData)) {
                logger.warn("Modal form button 2 has a show condition that evaluated to false. This is not fully supported in modal forms.");

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
            ErrorHandlingUtil.sendFormWithFallback(
                player,
                () -> formSender.sendForm(player, formBuilder.build()),
                "Â§eModal form could not be displayed. Please use the command interface or contact an administrator."
            );
        } else {
            logger.warn("FormSender is null, cannot send modal form to player: " + player.getName());
            player.sendMessage("Â§cForm system is unavailable. Please try again later.");
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


        ActionSystem.ActionContext context = PlaceholderUtil.createContextWithBuiltinPlaceholders(player, placeholders, messageData);


        List<FormButton> processedButtons = buttons;

        for (FormButton button : processedButtons) {

            if (button instanceof ConditionalButton) {
                ConditionalButton conditionalButton = (ConditionalButton) button;


                if (conditionalButton.hasShowCondition() &&
                    !ConditionEvaluator.evaluateCondition(player, conditionalButton.getShowCondition(), context, messageData)) {

                    continue;
                }


                String effectiveText = getEffectiveButtonText(conditionalButton, player, context, placeholders, messageData);
                String effectiveImage = getEffectiveButtonImage(conditionalButton, player, context);
                String effectiveOnClick = getEffectiveButtonOnClick(conditionalButton, player, context);


                if (effectiveImage != null) {
                    formBuilder.button(effectiveText, FormImage.Type.URL, effectiveImage);
                } else {
                    formBuilder.button(effectiveText);
                }

                if (effectiveOnClick != null) {
                    onClickActions.add(effectiveOnClick);
                }
            } else {

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
            ErrorHandlingUtil.sendFormWithFallback(
                player,
                () -> formSender.sendForm(player, form),
                "Â§eMenu '" + formMenu.getFormTitle() + "' could not be displayed. Please use commands or contact an administrator."
            );
        } else {
            logger.warn("FormSender is null, cannot send simple form to player: " + player.getName());
            player.sendMessage("Â§cForm system is unavailable. Please try again later.");
        }
    }

    private void openCustomForm(FormPlayer player, FormMenu formMenu, Map<String, String> placeholders, MessageData messageData) {
        String title = replacePlaceholders(formMenu.getFormTitle(), placeholders, player, messageData);
        CustomForm.Builder formBuilder = CustomForm.builder().title(title);


        ActionSystem.ActionContext context = PlaceholderUtil.createContextWithBuiltinPlaceholders(player, placeholders, messageData);

        Map<Integer, String> componentActions = new HashMap<>();
        Map<String, Object> componentResults = new HashMap<>();
        int[] componentIndex = {0};

        for (String componentKey : formMenu.getComponents().keySet()) {
            Map<String, Object> component = formMenu.getComponents().get(componentKey);
            String type = (String) component.get("type");

            switch (type.toLowerCase()) {
                case "input":
                    String inputTextRaw = (String) component.get("text");
                    String inputText = inputTextRaw != null ? replacePlaceholders(inputTextRaw, placeholders, player, messageData) : "";
                    String placeholder = (String) component.get("placeholder");
                    String defaultValue = (String) component.get("default");
                    formBuilder.input(inputText, placeholder, defaultValue);
                    componentActions.put(componentIndex[0], (String) component.get("action"));
                    componentResults.put(componentKey, "");
                    break;
                case "slider":
                    String sliderTextRaw = (String) component.get("text");
                    String sliderText = sliderTextRaw != null ? replacePlaceholders(sliderTextRaw, placeholders, player, messageData) : "";
                    int min = (int) component.get("min");
                    int max = (int) component.get("max");
                    int step = (int) component.get("step");
                    int defaultSlider = (int) component.get("default");
                    formBuilder.slider(sliderText, min, max, step, defaultSlider);
                    componentActions.put(componentIndex[0], (String) component.get("action"));
                    componentResults.put(componentKey, 0);
                    break;
                case "dropdown":
                    String dropdownTextRaw = (String) component.get("text");
                    String dropdownText = dropdownTextRaw != null ? replacePlaceholders(dropdownTextRaw, placeholders, player, messageData) : "";
                    @SuppressWarnings("unchecked")
                    List<String> options = (List<String>) component.get("options");
                    int defaultDropdown = (int) component.get("default");
                    formBuilder.dropdown(dropdownText, options, defaultDropdown);
                    componentActions.put(componentIndex[0], (String) component.get("action"));
                    componentResults.put(componentKey, "");
                    break;
                case "toggle":
                    String toggleTextRaw = (String) component.get("text");
                    String toggleText = toggleTextRaw != null ? replacePlaceholders(toggleTextRaw, placeholders, player, messageData) : "";
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
            ErrorHandlingUtil.sendFormWithFallback(
                player,
                () -> formSender.sendForm(player, form),
                "Â§eCustom form '" + formMenu.getFormTitle() + "' could not be displayed. Please use alternative methods or contact an administrator."
            );
        } else {
            logger.warn("FormSender is null, cannot send custom form to player: " + player.getName());
            player.sendMessage("Â§cForm system is unavailable. Please try again later.");
        }
    }

    protected void handleOnClick(FormPlayer player, String onClickAction, Map<String, String> placeholders, MessageData messageData) {
        if (onClickAction == null || onClickAction.trim().isEmpty()) {
            logger.warn("Empty onClick action for player: " + player.getName());
            return;
        }

        onClickAction = replacePlaceholders(onClickAction.trim().replaceAll("\\s+", " "), placeholders, player, messageData);


        ActionSystem.ActionContext context = PlaceholderUtil.createContextWithBuiltinPlaceholders(player, placeholders, messageData);


        if (onClickAction.startsWith("[") && onClickAction.endsWith("]")) {
            handleMultipleActions(player, onClickAction, context);
        } else {

            handleSingleAction(player, onClickAction, context);
        }
    }


    private void handleSingleAction(FormPlayer player, String onClickAction, ActionSystem.ActionContext context) {

        ActionSystem.Action action = actionExecutor.parseAction(onClickAction);
        if (action == null) {
            logger.warn("Failed to parse onClick action: " + onClickAction);
            player.sendMessage("Invalid action format");
            return;
        }

        ActionSystem.ActionResult result = actionExecutor.executeAction(player, action.getActionDefinition(), context);
        logger.debug(result.toString());
        if (result.isFailure()) {
            logger.warn("Action execution failed for player " + player.getName() + ": " + result.message());
            if (result.message() != null) {
                player.sendMessage("Action failed: " + result.message());
            }
        } else {
            logger.debug("Successfully executed action for player " + player.getName() + ": " + onClickAction);
        }
    }


    private void handleMultipleActions(FormPlayer player, String onClickAction, ActionSystem.ActionContext context) {
        try {

            String actionsString = onClickAction.substring(1, onClickAction.length() - 1);
            String[] actionStrings = actionsString.split(",");

            List<ActionSystem.Action> actions = new ArrayList<>();


            for (String actionString : actionStrings) {
                String trimmed = actionString.trim();
                if (!trimmed.isEmpty()) {
                    ActionSystem.Action action = actionExecutor.parseAction(trimmed);
                    if (action != null) {
                        actions.add(action);
                    } else {
                        logger.warn("Failed to parse action in multi-action sequence: " + trimmed);
                        player.sendMessage("Invalid action in sequence: " + trimmed);
                        return;
                    }
                }
            }

            if (actions.isEmpty()) {
                logger.warn("No valid actions found in multi-action sequence for player: " + player.getName());
                player.sendMessage("No valid actions found in sequence");
                return;
            }


            List<ActionSystem.ActionResult> results = actionExecutor.executeActions(player, actions, context);


            boolean hasFailures = false;
            for (int i = 0; i < results.size(); i++) {
                ActionSystem.ActionResult result = results.get(i);
                if (result.isFailure()) {
                    hasFailures = true;
                    logger.warn("Action " + (i + 1) + " failed for player " + player.getName() + ": " + result.message());
                }
            }

            if (hasFailures) {
                player.sendMessage("Some actions in the sequence failed. Check logs for details.");
            } else {
                logger.debug("Successfully executed all actions in sequence for player " + player.getName());
            }

        } catch (Exception e) {
            logger.error("Error executing multi-action sequence for player " + player.getName(), e);
            player.sendMessage("Error executing action sequence: " + e.getMessage());
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


        if (value != null) {
            action = action.replace("$1", value);
        }


        Map<String, String> customPlaceholders = new HashMap<>();
        if (value != null) {
            customPlaceholders.put("value", value);
            customPlaceholders.put("1", value);
        }

        ActionSystem.ActionContext context = PlaceholderUtil.createContextWithBuiltinPlaceholders(player, customPlaceholders, messageData);


        ActionSystem.Action parsedAction = actionExecutor.parseAction(action);
        if (parsedAction == null) {
            logger.warn("Failed to parse custom action: " + action);
            player.sendMessage("Invalid action format");
            return;
        }

        ActionSystem.ActionResult result = actionExecutor.executeAction(player, parsedAction.getActionDefinition(), context);

        if (result.isFailure()) {
            logger.warn("Custom action execution failed for player " + player.getName() + ": " + result.message());
            if (result.message() != null) {
                player.sendMessage("Action failed: " + result.message());
            }
        } else {
            logger.debug("Successfully executed custom action for player " + player.getName() + ": " + action);
        }
    }

    private int countPlaceholders(String command) {
        return PlaceholderUtil.countNumberedPlaceholders(command);
    }

    protected String replacePlaceholders(String text, Map<String, String> placeholders, FormPlayer player, MessageData messageData) {
        if (text == null) {
            return null;
        }

        String result = text;


        if (placeholders != null && !placeholders.isEmpty()) {
            result = PlaceholderUtil.processDynamicPlaceholders(result, placeholders);
        }


        if (messageData != null && result.contains("%")) {
            result = messageData.replaceVariables(result, null, player);
        }

        // Always apply color codes translation at the end so '&' and '&x' hex sequences render
        if (messageData != null) {
            result = messageData.applyColor(result);
        }

        return result;
    }


    public boolean hasMenu(String menuName) {
        if (menuName == null) {
            return false;
        }
        return formMenus.containsKey(menuName.toLowerCase());
    }


    public void openMenu(FormPlayer player, String menuName) {
        openForm(player, menuName, new String[0]);
    }


    public void registerActionHandler(ActionSystem.ActionHandler handler) {
        actionRegistry.registerHandler(handler);
        logger.info("Registered custom action handler: " + handler.getActionType());
    }


    public boolean unregisterActionHandler(String actionType) {
        boolean removed = actionRegistry.unregisterHandler(actionType);
        if (removed) {
            logger.info("Unregistered action handler: " + actionType);
        }
        return removed;
    }


    public ActionRegistry getActionRegistry() {
        return actionRegistry;
    }


    public ActionExecutor getActionExecutor() {
        return actionExecutor;
    }


    public void shutdown() {
        if (actionExecutor != null) {
            actionExecutor.shutdown();
        }


        if (delayActionHandler != null) {
            delayActionHandler.shutdown();
        }
        logger.info("FormMenuUtil shutdown completed");
    }


    private void validateConfiguration() {
        ConfigValidator validator = new ConfigValidator(messageData, actionRegistry);
        ConfigValidator.ValidationResult result = validator.validateConfiguration(formMenus);


        if (result.hasErrors()) {
            logger.warn("Configuration validation found " + result.getErrors().size() + " errors:");
            for (String error : result.getErrors()) {
                logger.warn("  - " + error);
            }
        }


        if (result.hasWarnings()) {
            logger.info("Configuration validation found " + result.getWarnings().size() + " warnings:");
            for (String warning : result.getWarnings()) {
                logger.info("  - " + warning);
            }
        }

        if (result.isValid()) {
            logger.info("Configuration validation completed successfully");
        } else {
            logger.warn("Configuration validation completed with errors. Some features may not work properly.");
        }
    }

    public Map<String, FormMenu> getFormMenus() {
        return formMenus;
    }


    private String getEffectiveButtonText(FormButton button, FormPlayer player, ActionSystem.ActionContext context, Map<String, String> placeholders, MessageData messageData) {
        if (button instanceof ConditionalButton) {
            ConditionalButton conditionalButton = (ConditionalButton) button;


            String matchedCondition = null;
            for (Map.Entry<String, ConditionalButton.ConditionalProperty> entry : conditionalButton.getConditionalProperties().entrySet()) {
                String condition = entry.getKey();
                ConditionalButton.ConditionalProperty property = entry.getValue();

                if ("text".equals(property.getProperty()) && ConditionEvaluator.evaluateCondition(player, condition, context, messageData)) {
                    matchedCondition = condition;
                    break;
                }
            }


            String effectiveText = conditionalButton.getEffectiveText(matchedCondition);
            return replacePlaceholders(effectiveText, placeholders, player, messageData);
        }


        return replacePlaceholders(button.getText(), placeholders, player, messageData);
    }


    private String getEffectiveButtonImage(FormButton button, FormPlayer player, ActionSystem.ActionContext context) {
        if (button instanceof ConditionalButton) {
            ConditionalButton conditionalButton = (ConditionalButton) button;


            String matchedCondition = null;
            for (Map.Entry<String, ConditionalButton.ConditionalProperty> entry : conditionalButton.getConditionalProperties().entrySet()) {
                String condition = entry.getKey();
                ConditionalButton.ConditionalProperty property = entry.getValue();

                if ("image".equals(property.getProperty()) && ConditionEvaluator.evaluateCondition(player, condition, context, messageData)) {
                    matchedCondition = condition;
                    break;
                }
            }


            return conditionalButton.getEffectiveImage(matchedCondition);
        }


        return button.getImage();
    }


    private ActionSystem.ActionDefinition convertOnClickToActionDefinition(String onClick) {
        if (onClick == null || onClick.trim().isEmpty()) {
            return null;
        }

        ActionSystem.ActionDefinition actionDef = new ActionSystem.ActionDefinition();


        if (onClick.startsWith("[") && onClick.endsWith("]")) {

            String actionsString = onClick.substring(1, onClick.length() - 1);
            String[] actionStrings = actionsString.split(",");

            for (String actionString : actionStrings) {
                String trimmed = actionString.trim();
                if (!trimmed.isEmpty()) {
                    parseAndAddAction(actionDef, trimmed);
                }
            }
        } else {

            parseAndAddAction(actionDef, onClick);
        }

        return actionDef;
    }


    private void parseAndAddAction(ActionSystem.ActionDefinition actionDef, String actionString) {
        String trimmed = actionString.trim();


        if (trimmed.contains("{") && trimmed.contains("}")) {

            int braceIndex = trimmed.indexOf('{');
            if (braceIndex > 0) {
                String actionType = trimmed.substring(0, braceIndex).trim();

                actionDef.addAction(actionType, trimmed);
                return;
            }
        }


        if (trimmed.contains(":")) {

            throw new IllegalArgumentException("Legacy colon-separated format is no longer supported. Use curly-brace format instead: action { ... }");
        }


        throw new IllegalArgumentException("Invalid action format. Actions must use curly-brace format: action { ... }");
    }

     private String getEffectiveButtonOnClick(FormButton button, FormPlayer player, ActionSystem.ActionContext context) {
         if (button instanceof ConditionalButton) {
            ConditionalButton conditionalButton = (ConditionalButton) button;


            String matchedCondition = null;
            for (Map.Entry<String, ConditionalButton.ConditionalProperty> entry : conditionalButton.getConditionalProperties().entrySet()) {
                String condition = entry.getKey();
                ConditionalButton.ConditionalProperty property = entry.getValue();

                if ("onClick".equals(property.getProperty()) && ConditionEvaluator.evaluateCondition(player, condition, context, messageData)) {
                    matchedCondition = condition;
                    break;
                }
            }


            return conditionalButton.getEffectiveOnClick(matchedCondition);
        }


        return button.getOnClick();
    }
}

