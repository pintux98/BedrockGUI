package it.pintux.life.common.form;

import it.pintux.life.common.actions.ActionSystem;

import it.pintux.life.common.actions.*;
import it.pintux.life.common.actions.handlers.*;
import it.pintux.life.common.form.obj.ConditionalButton;
import it.pintux.life.common.form.obj.FormButton;
import it.pintux.life.common.form.obj.FormMenu;
import it.pintux.life.common.platform.*;
import it.pintux.life.common.utils.*;
import it.pintux.life.common.utils.ValidationUtils;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;

import java.util.*;
import java.util.stream.Collectors;

public class FormMenuUtil {

    private static final Logger logger = Logger.getLogger(FormMenuUtil.class.getSimpleName());
    private static final String ONLINE_PLAYERS_TOKEN = "{online_players}";
    private final Map<String, FormMenu> formMenus;
    private final FormConfig config;
    protected final MessageData messageData;
    private final ActionExecutor actionExecutor;
    private final ActionRegistry actionRegistry;
    private final PlatformCommandExecutor commandExecutor;
    private final PlatformSoundManager soundManager;
    private final PlatformEconomyManager economyManager;
    private final FormSender formSender;
    private final PlatformScheduler scheduler;
    private final PlatformTitleManager titleManager;
    private final PlatformPluginManager pluginManager;
    private final PlatformPlayerManager playerManager;
    private final PlatformInventoryManager inventoryManager;
    private AssetServer assetServer;
    private final Map<String, Long> cooldowns = new java.util.concurrent.ConcurrentHashMap<>();


    public FormConfig getConfig() {
        return config;
    }

    public FormMenuUtil(FormConfig config, MessageData messageData,
                        PlatformCommandExecutor commandExecutor,
                        PlatformSoundManager soundManager,
                        PlatformEconomyManager economyManager,
                        FormSender formSender,
                        PlatformTitleManager titleManager,
                        PlatformPluginManager pluginManager,
                        PlatformPlayerManager playerManager,
                        PlatformScheduler scheduler,
                        PlatformInventoryManager inventoryManager) {
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
        this.scheduler = scheduler;
        this.inventoryManager = inventoryManager;
        this.actionRegistry = ActionRegistry.getInstance();
        this.actionExecutor = new ActionExecutor(actionRegistry);
        this.actionExecutor.setScheduler(scheduler);


        if (pluginManager != null) {
            ConditionEvaluator.setPluginManager(pluginManager);
        }

        registerDefaultActionHandlers();
        loadFormMenus();
        validateConfiguration();
    }


    private void registerDefaultActionHandlers() {

        actionRegistry.registerHandler(new CommandActionHandler(commandExecutor));
        actionRegistry.registerHandler(new OpenFormActionHandler(this));
        actionRegistry.registerHandler(new MessageActionHandler(playerManager));
        actionRegistry.registerHandler(new DelayActionHandler(actionExecutor, scheduler));

        if (commandExecutor != null) {
            actionRegistry.registerHandler(new ServerActionHandler(commandExecutor));
        }

        if (playerManager != null) {
            actionRegistry.registerHandler(new BroadcastActionHandler(playerManager));
        }

        if (commandExecutor != null && inventoryManager != null) {
            actionRegistry.registerHandler(new InventoryActionHandler(commandExecutor, inventoryManager));
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
        actionRegistry.registerHandler(new BungeeActionHandler(playerManager));

        logger.info("Registered " + actionRegistry.size() + " action handlers >> "  + actionRegistry.getAllHandlers().stream().map(actionHandler -> actionHandler.getActionType()).collect(Collectors.toList()));
    }

    private void loadFormMenus() {
        for (String key : config.getKeys("forms")) {
            String filePath = config.getString("forms." + key + ".file");
            FormConfig source = config;
            String bedrockBase = "forms." + key;
            if (filePath != null && !filePath.trim().isEmpty()) {
                source = config.loadFormFile(filePath.trim());
                bedrockBase = "bedrock";
            }
            FormMenu menu;
            try {
                menu = buildFormMenuFromPaths(key, bedrockBase, source);
            } catch (Exception e) {
                logger.warn("Skipping form '" + key + "': " + e.getMessage());
                continue;
            }
            if (menu != null) {
                formMenus.put(key.toLowerCase(), menu);
            }
        }
        logger.info("Loaded forms >> " + formMenus.keySet());
    }

    private FormMenu buildFormMenuFromPaths(String key, String bedrockBase, FormConfig cfg) {
        String command = blankToNull(cfg.getString(bedrockBase + ".command"));
        String commandIntercept = blankToNull(cfg.getString(bedrockBase + ".command_intercept"));
        String permission = blankToNull(cfg.getString(bedrockBase + ".permission"));
        String type = cfg.getString(bedrockBase + ".type", "SIMPLE");
        String title = cfg.getString(bedrockBase + ".title", "");
        String contentVal = readMultilineText(cfg, bedrockBase + ".content");
        String description = readMultilineText(cfg, bedrockBase + ".description");
        String resolvedContent = contentVal != null ? contentVal : description;

        List<FormButton> buttons = readBedrockButtons(cfg, bedrockBase, key, type);
        if (buttons == null) {
            return null;
        }

        Map<String, Map<String, Object>> components = readBedrockComponents(cfg, bedrockBase, type);
        List<String> globalActions = cfg.getStringList(bedrockBase + ".global_actions");

        FormMenu menu = new FormMenu(command, commandIntercept, permission, title, resolvedContent, type, buttons, components, globalActions);
        menu.setCloseActions(readActions(cfg, bedrockBase + ".on_close"));
        menu.setOpenCondition(blankToNull(cfg.getString(bedrockBase + ".open_condition")),
                blankToNull(cfg.getString(bedrockBase + ".open_condition_message")));
        menu.setCooldownSeconds(ValidationUtils.parseSeconds(cfg.getString(bedrockBase + ".cooldown")));
        return menu;
    }

    private String readActions(FormConfig cfg, String path) {
        try {
            List<String> list = cfg.getStringList(path);
            if (list != null && !list.isEmpty()) {
                return "[" + String.join(", ", list) + "]";
            }
        } catch (Exception ignored) {
        }
        return blankToNull(cfg.getString(path));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private int remainingCooldown(FormPlayer player, String key, int seconds) {
        if (seconds <= 0 || key == null) {
            return 0;
        }
        String id = player.getUniqueId() + "|" + key;
        long now = System.currentTimeMillis();
        Long until = cooldowns.get(id);
        if (until != null && until > now) {
            return (int) Math.ceil((until - now) / 1000.0);
        }
        cooldowns.put(id, now + seconds * 1000L);
        return 0;
    }

    private boolean onCooldown(FormPlayer player, String key, int seconds) {
        int remaining = remainingCooldown(player, key, seconds);
        if (remaining > 0) {
            player.sendMessage(messageData.getValue(MessageData.MENU_COOLDOWN, Map.of("seconds", remaining), player));
            return true;
        }
        return false;
    }

    private String readMultilineText(FormConfig cfg, String path) {
        String s = cfg.getString(path);
        if (s != null) return s;

        List<String> list = null;
        try {
            list = cfg.getStringList(path);
        } catch (Exception ignored) {
        }
        if (list != null && !list.isEmpty()) {
            return String.join("\n", list);
        }

        Map<String, Object> values = null;
        try {
            values = cfg.getValues(path);
        } catch (Exception ignored) {
        }
        if (values != null && !values.isEmpty()) {
            return String.join("\n", values.keySet());
        }

        return null;
    }

    private List<FormButton> readBedrockButtons(FormConfig cfg, String base, String key, String type) {
        List<FormButton> buttons = new ArrayList<>();
        if (type.equalsIgnoreCase("SIMPLE") || type.equalsIgnoreCase("MODAL")) {
            for (String button : cfg.getKeys(base + ".buttons")) {
                String text = cfg.getString(base + ".buttons." + button + ".text");
                String image = cfg.getString(base + ".buttons." + button + ".image");

                String onClick = readActions(cfg, base + ".buttons." + button + ".onClick");
                int cooldown = ValidationUtils.parseSeconds(cfg.getString(base + ".buttons." + button + ".cooldown"));

                String showCondition = cfg.getString(base + ".buttons." + button + ".show_condition");
                String alternativeText = cfg.getString(base + ".buttons." + button + ".alternative_text");
                String alternativeImage = cfg.getString(base + ".buttons." + button + ".alternative_image");
                String alternativeOnClick = cfg.getString(base + ".buttons." + button + ".alternative_onClick");
                Set<String> conditionKeys = cfg.getKeys(base + ".buttons." + button + ".conditions");
                boolean hasConditions = conditionKeys != null && !conditionKeys.isEmpty();

                if (showCondition != null || alternativeText != null || alternativeImage != null || alternativeOnClick != null || hasConditions) {
                    ConditionalButton conditionalButton = new ConditionalButton(text, image, onClick, showCondition);
                    conditionalButton.setAlternativeText(alternativeText);
                    conditionalButton.setAlternativeImage(alternativeImage);
                    conditionalButton.setAlternativeOnClick(alternativeOnClick);
                    if (onClick != null && !onClick.trim().isEmpty()) {
                        ActionSystem.ActionDefinition actionDef = convertOnClickToActionDefinition(onClick);
                        conditionalButton.setAction(actionDef);
                    }
                    for (String condKey : conditionKeys) {
                        String condition = cfg.getString(base + ".buttons." + button + ".conditions." + condKey + ".condition");
                        String property = cfg.getString(base + ".buttons." + button + ".conditions." + condKey + ".property");
                        String value = cfg.getString(base + ".buttons." + button + ".conditions." + condKey + ".value");
                        if (condition != null && property != null && value != null) {
                            conditionalButton.addConditionalProperty(condition, property, value);
                        }
                    }
                    conditionalButton.setCooldown(key + "." + button, cooldown);
                    buttons.add(conditionalButton);
                } else {
                    FormButton formButton = new FormButton(text, image, onClick);
                    if (onClick != null && !onClick.trim().isEmpty()) {
                        ActionSystem.ActionDefinition actionDef = convertOnClickToActionDefinition(onClick);
                        formButton.setAction(actionDef);
                    }
                    formButton.setCooldown(key + "." + button, cooldown);
                    buttons.add(formButton);
                }
            }
            if (type.equalsIgnoreCase("MODAL")) {
                if (buttons.size() != 2) {
                    logger.warn("Modal's must only have 2 buttons! Please modify menu." + key);
                    return null;
                }
            }
        }
        return buttons;
    }

    private Map<String, Map<String, Object>> readBedrockComponents(FormConfig cfg, String base, String type) {
        Map<String, Map<String, Object>> components = new LinkedHashMap<>();
        if (type.equalsIgnoreCase("CUSTOM")) {
            for (String componentKey : cfg.getKeys(base + ".components")) {
                Map<String, Object> component = cfg.getValues(base + ".components." + componentKey);
                components.put(componentKey, component);
            }
        }
        return components;
    }

    public void reloadFormMenus() {
        logger.info("Reloading form menus from configuration...");
        formMenus.clear();
        loadFormMenus();
        validateConfiguration();
        logger.info("Successfully reloaded " + formMenus.size() + " form menus");
    }


    public void openForm(FormPlayer player, String menuName, String[] args) {
        FormMenu menu = formMenus.get(menuName.toLowerCase());
        if (menu == null) {
            player.sendMessage(messageData.getValue(MessageData.MENU_NOT_FOUND, Map.of("menu", menuName), null));
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

        if (menu.getOpenCondition() != null) {
            ActionSystem.ActionContext context = PlaceholderUtil.createContextWithBuiltinPlaceholders(player, placeholders, messageData);
            if (!ConditionEvaluator.evaluateCondition(player, menu.getOpenCondition(), context, messageData)) {
                player.sendMessage(menu.getOpenConditionMessage() != null
                        ? messageData.applyColor(PlaceholderUtil.processPlaceholders(menu.getOpenConditionMessage(), context.getPlaceholders(), player, messageData))
                        : messageData.getValue(MessageData.MENU_REQUIREMENTS, null, player));
                return;
            }
        }

        if (onCooldown(player, menuName.toLowerCase(), menu.getCooldownSeconds())) {
            return;
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

            boolean cb1ConditionFailed = cb1.hasShowCondition() &&
                    !ConditionEvaluator.evaluateCondition(player, cb1.getShowCondition(), context, messageData);
            if (cb1ConditionFailed && !cb1.hasAlternative()) {
                logger.warn("Modal form button 1 has a show condition that evaluated to false and no alternative. Modal forms always need both buttons, so the primary values are used.");
            }
            button1Text = getEffectiveButtonText(cb1, player, context, placeholders, messageData, cb1ConditionFailed);
            button1OnClick = getEffectiveButtonOnClick(cb1, player, context, cb1ConditionFailed);
        } else {
            button1Text = replacePlaceholders(b1.getText(), placeholders, player, messageData);
            button1OnClick = b1.getOnClick();
        }

        if (b2 instanceof ConditionalButton) {
            ConditionalButton cb2 = (ConditionalButton) b2;

            boolean cb2ConditionFailed = cb2.hasShowCondition() &&
                    !ConditionEvaluator.evaluateCondition(player, cb2.getShowCondition(), context, messageData);
            if (cb2ConditionFailed && !cb2.hasAlternative()) {
                logger.warn("Modal form button 2 has a show condition that evaluated to false and no alternative. Modal forms always need both buttons, so the primary values are used.");
            }
            button2Text = getEffectiveButtonText(cb2, player, context, placeholders, messageData, cb2ConditionFailed);
            button2OnClick = getEffectiveButtonOnClick(cb2, player, context, cb2ConditionFailed);
        } else {
            button2Text = replacePlaceholders(b2.getText(), placeholders, player, messageData);
            button2OnClick = b2.getOnClick();
        }

        formBuilder
                .button1(button1Text)
                .button2(button2Text)
                .validResultHandler((formResponse, modalResponse) -> {
                    boolean first = modalResponse.clickedButtonId() == 0;
                    FormButton clicked = first ? b1 : b2;
                    String action = first ? button1OnClick : button2OnClick;
                    if (action != null && !onCooldown(player, clicked.getCooldownKey(), clicked.getCooldownSeconds())) {
                        handleOnClick(player, action, placeholders, messageData);
                    }
                });
        addCloseActions(formBuilder, player, formMenu, placeholders);

        if (formSender != null) {
            ErrorHandlingUtil.sendFormWithFallback(
                    player,
                    () -> formSender.sendForm(player, formBuilder.build()),
                    messageData.getValue(MessageData.MENU_NOJAVA, null, player)
            );
        } else {
            logger.warn("FormSender is null, cannot send modal form to player: " + player.getName());
            player.sendMessage(messageData.getValue(MessageData.FORMS_SYSTEM_UNAVAILABLE, null, player));
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
        List<FormButton> shownButtons = new ArrayList<>();


        ActionSystem.ActionContext context = PlaceholderUtil.createContextWithBuiltinPlaceholders(player, placeholders, messageData);


        List<FormButton> processedButtons = buttons;

        for (FormButton button : processedButtons) {

            if (button instanceof ConditionalButton) {
                ConditionalButton conditionalButton = (ConditionalButton) button;


                boolean conditionFailed = conditionalButton.hasShowCondition() &&
                        !ConditionEvaluator.evaluateCondition(player, conditionalButton.getShowCondition(), context, messageData);

                if (conditionFailed && !conditionalButton.hasAlternative()) {

                    continue;
                }


                String effectiveText = getEffectiveButtonText(conditionalButton, player, context, placeholders, messageData, conditionFailed);
                String effectiveImage = getEffectiveButtonImage(conditionalButton, player, context, conditionFailed);
                if (effectiveImage != null) {
                    effectiveImage = replacePlaceholders(effectiveImage, placeholders, player, messageData);
                    effectiveImage = mapImageSource(effectiveImage);
                }
                String effectiveOnClick = getEffectiveButtonOnClick(conditionalButton, player, context, conditionFailed);


                if (effectiveImage != null) {
                    FormImage.Type type = it.pintux.life.common.utils.IconResolver.isUrl(effectiveImage) ? FormImage.Type.URL : FormImage.Type.PATH;
                    formBuilder.button(effectiveText, type, effectiveImage);
                } else {
                    formBuilder.button(effectiveText);
                }

                onClickActions.add(effectiveOnClick);
                shownButtons.add(button);
            } else {

                String buttonText = replacePlaceholders(button.getText(), placeholders, player, messageData);
                if (button.getImage() != null) {
                    String srcRaw = button.getImage();
                    String srcProcessed = replacePlaceholders(srcRaw, placeholders, player, messageData);
                    String src = mapImageSource(srcProcessed);
                    if (src != null) {
                        FormImage.Type type = it.pintux.life.common.utils.IconResolver.isUrl(src) ? FormImage.Type.URL : FormImage.Type.PATH;
                        formBuilder.button(buttonText, type, src);
                    } else {
                        formBuilder.button(buttonText);
                    }
                } else {
                    formBuilder.button(buttonText);
                }
                onClickActions.add(button.getOnClick());
                shownButtons.add(button);
            }
        }

        formBuilder.validResultHandler((form, response) -> {
            int clickedButtonId = response.clickedButtonId();
            if (clickedButtonId < 0 || clickedButtonId >= onClickActions.size()) {
                return;
            }
            String action = onClickActions.get(clickedButtonId);
            FormButton clicked = shownButtons.get(clickedButtonId);
            if (action != null && !onCooldown(player, clicked.getCooldownKey(), clicked.getCooldownSeconds())) {
                handleOnClick(player, action, placeholders, messageData);
            }
        });
        addCloseActions(formBuilder, player, formMenu, placeholders);

        SimpleForm form = formBuilder.build();
        if (formSender != null) {
            ErrorHandlingUtil.sendFormWithFallback(
                    player,
                    () -> formSender.sendForm(player, form),
                    messageData.getValue(MessageData.MENU_NOJAVA, null, player)
            );
        } else {
            logger.warn("FormSender is null, cannot send simple form to player: " + player.getName());
            player.sendMessage(messageData.getValue(MessageData.FORMS_SYSTEM_UNAVAILABLE, null, player));
        }
    }

    private void openCustomForm(FormPlayer player, FormMenu formMenu, Map<String, String> placeholders, MessageData messageData) {
        String title = replacePlaceholders(formMenu.getFormTitle(), placeholders, player, messageData);
        CustomForm.Builder formBuilder = CustomForm.builder().title(title);


        ActionSystem.ActionContext context = PlaceholderUtil.createContextWithBuiltinPlaceholders(player, placeholders, messageData);

        Map<Integer, String> componentActions = new HashMap<>();
        Map<String, Object> componentResults = new HashMap<>();
        Map<String, List<String>> resolvedDropdownOptions = new HashMap<>();
        int[] componentIndex = {0};

        for (String componentKey : formMenu.getComponents().keySet()) {
            Map<String, Object> component = formMenu.getComponents().get(componentKey);
            String type = (String) component.get("type");

            switch (type.toLowerCase()) {
                case "input":
                    String inputTextRaw = (String) component.get("text");
                    String inputText = inputTextRaw != null ? replacePlaceholders(inputTextRaw, placeholders, player, messageData) : "";
                    String placeholder = component.get("placeholder") != null ? replacePlaceholders(String.valueOf(component.get("placeholder")), placeholders, player, messageData) : "";
                    String defaultValue = component.get("default") != null ? replacePlaceholders(String.valueOf(component.get("default")), placeholders, player, messageData) : "";
                    formBuilder.input(inputText, placeholder, defaultValue);
                    Object inputAction = component.get("action");
                    if (inputAction instanceof List) {
                        componentActions.put(componentIndex[0], "[" + String.join(", ", (List<String>) inputAction) + "]");
                    } else {
                        componentActions.put(componentIndex[0], (String) inputAction);
                    }
                    componentResults.put(componentKey, "");
                    break;
                case "slider":
                    String sliderTextRaw = (String) component.get("text");
                    String sliderText = sliderTextRaw != null ? replacePlaceholders(sliderTextRaw, placeholders, player, messageData) : "";

                    // Handle numbers safely
                    int min = 0;
                    if (component.get("min") instanceof Number) {
                        min = ((Number) component.get("min")).intValue();
                    }

                    int max = 10;
                    if (component.get("max") instanceof Number) {
                        max = ((Number) component.get("max")).intValue();
                    }

                    int step = 1;
                    if (component.get("step") instanceof Number) {
                        step = ((Number) component.get("step")).intValue();
                    }

                    int defaultSlider = min;
                    if (component.get("default") instanceof Number) {
                        defaultSlider = ((Number) component.get("default")).intValue();
                    }

                    formBuilder.slider(sliderText, min, max, step, defaultSlider);
                    Object sliderAction = component.get("action");
                    if (sliderAction instanceof List) {
                        componentActions.put(componentIndex[0], "[" + String.join(", ", (List<String>) sliderAction) + "]");
                    } else {
                        componentActions.put(componentIndex[0], (String) sliderAction);
                    }
                    componentResults.put(componentKey, 0);
                    break;
                case "dropdown":
                    String dropdownTextRaw = (String) component.get("text");
                    String dropdownText = dropdownTextRaw != null ? replacePlaceholders(dropdownTextRaw, placeholders, player, messageData) : "";
                    List<String> options = resolveDropdownOptions(component.get("options"), placeholders, player, messageData);
                    resolvedDropdownOptions.put(componentKey, options);

                    int defaultDropdown = 0;
                    if (component.get("default") instanceof Number) {
                        defaultDropdown = ((Number) component.get("default")).intValue();
                    }

                    if (defaultDropdown < 0 || defaultDropdown >= options.size()) {
                        defaultDropdown = 0;
                    }
                    formBuilder.dropdown(dropdownText, options, defaultDropdown);
                    Object dropdownAction = component.get("action");
                    if (dropdownAction instanceof List) {
                        componentActions.put(componentIndex[0], "[" + String.join(", ", (List<String>) dropdownAction) + "]");
                    } else {
                        componentActions.put(componentIndex[0], (String) dropdownAction);
                    }
                    componentResults.put(componentKey, "");
                    break;
                case "toggle":
                    String toggleTextRaw = (String) component.get("text");
                    String toggleText = toggleTextRaw != null ? replacePlaceholders(toggleTextRaw, placeholders, player, messageData) : "";

                    boolean defaultToggle = false;
                    Object defToggleObj = component.get("default");
                    if (defToggleObj instanceof Boolean) {
                        defaultToggle = (Boolean) defToggleObj;
                    } else if (defToggleObj instanceof String) {
                        defaultToggle = Boolean.parseBoolean((String) defToggleObj);
                    }

                    formBuilder.toggle(toggleText, defaultToggle);
                    Object toggleAction = component.get("action");
                    if (toggleAction instanceof List) {
                        componentActions.put(componentIndex[0], "[" + String.join(", ", (List<String>) toggleAction) + "]");
                    } else {
                        componentActions.put(componentIndex[0], (String) toggleAction);
                    }
                    componentResults.put(componentKey, false);
                    break;
                case "label":
                    Object labelRaw = component.get("text");
                    formBuilder.label(labelRaw != null ? replacePlaceholders(String.valueOf(labelRaw), placeholders, player, messageData) : "");
                    break;
                case "step_slider":
                    String stepTextRaw = (String) component.get("text");
                    String stepText = stepTextRaw != null ? replacePlaceholders(stepTextRaw, placeholders, player, messageData) : "";
                    List<String> steps = resolveDropdownOptions(component.get("steps"), placeholders, player, messageData);
                    resolvedDropdownOptions.put(componentKey, steps);
                    int defaultStep = component.get("default") instanceof Number number ? number.intValue() : 0;
                    if (defaultStep < 0 || defaultStep >= steps.size()) {
                        defaultStep = 0;
                    }
                    formBuilder.stepSlider(stepText, steps, defaultStep);
                    Object stepAction = component.get("action");
                    if (stepAction instanceof List) {
                        componentActions.put(componentIndex[0], "[" + String.join(", ", (List<String>) stepAction) + "]");
                    } else {
                        componentActions.put(componentIndex[0], (String) stepAction);
                    }
                    componentResults.put(componentKey, "");
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
                        result = PlaceholderUtil.sanitizeInput(customFormResponse.asInput(componentIndex[0]));
                        componentResults.put(componentKey, result);
                        break;
                    case "slider":
                        int sliderResult = (int) customFormResponse.asSlider(componentIndex[0]);
                        result = String.valueOf(sliderResult);
                        componentResults.put(componentKey, sliderResult);
                        break;
                    case "dropdown":
                        int dropdownResult = customFormResponse.asDropdown(componentIndex[0]);
                        List<String> options = resolvedDropdownOptions.get(componentKey);
                        if (options == null) {
                            options = resolveDropdownOptions(component.get("options"), placeholders, player, messageData);
                        }
                        if (dropdownResult >= 0 && dropdownResult < options.size()) {
                            result = PlaceholderUtil.sanitizeInput(options.get(dropdownResult));
                        } else {
                            result = "";
                        }
                        componentResults.put(componentKey, result);
                        break;
                    case "toggle":
                        boolean toggleResult = customFormResponse.asToggle(componentIndex[0]);
                        result = String.valueOf(toggleResult);
                        componentResults.put(componentKey, toggleResult);
                        break;
                    case "step_slider":
                        int stepResult = customFormResponse.asStepSlider(componentIndex[0]);
                        List<String> stepOptions = resolvedDropdownOptions.get(componentKey);
                        result = stepOptions != null && stepResult >= 0 && stepResult < stepOptions.size()
                                ? PlaceholderUtil.sanitizeInput(stepOptions.get(stepResult)) : "";
                        componentResults.put(componentKey, result);
                        break;
                }

                if (action != null) {
                    handleCustomAction(player, action, result);
                }

                componentIndex[0]++;
            }

            List<String> globalActions = formMenu.getGlobalActions();
            if (globalActions != null && !globalActions.isEmpty()) {
                Map<String, String> results = new HashMap<>();
                componentResults.forEach((k, v) -> results.put(k, String.valueOf(v)));
                String sequence = "[" + String.join(", ", globalActions) + "]";
                handleCustomAction(player, PlaceholderUtil.processDynamicPlaceholders(sequence, results), null);
            }
        });

        addCloseActions(formBuilder, player, formMenu, placeholders);

        CustomForm form = formBuilder.build();
        if (formSender != null) {
            ErrorHandlingUtil.sendFormWithFallback(
                    player,
                    () -> formSender.sendForm(player, form),
                    messageData.getValue(MessageData.MENU_NOJAVA, null, player)
            );
        } else {
            logger.warn("FormSender is null, cannot send custom form to player: " + player.getName());
            player.sendMessage(messageData.getValue(MessageData.FORMS_SYSTEM_UNAVAILABLE, null, player));
        }
    }

    private void addCloseActions(org.geysermc.cumulus.form.util.FormBuilder<?, ?, ?> builder, FormPlayer player, FormMenu formMenu, Map<String, String> placeholders) {
        String closeActions = formMenu.getCloseActions();
        if (closeActions != null) {
            builder.closedResultHandler(() -> handleOnClick(player, closeActions, placeholders, messageData));
        }
    }

    protected void handleOnClick(FormPlayer player, String onClickAction, Map<String, String> placeholders, MessageData messageData) {
        if (onClickAction == null || onClickAction.trim().isEmpty()) {
            logger.warn("Empty onClick action for player: " + player.getName());
            return;
        }

        placeholders = PlaceholderUtil.sanitizeInputs(placeholders);
        onClickAction = messageData.applyColor(PlaceholderUtil.processDynamicPlaceholders(onClickAction.trim().replaceAll("\\s+", " "), placeholders));


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
            player.sendMessage(messageData.getValue(MessageData.FORMS_INVALID_ACTION_FORMAT, null, player));
            return;
        }

        ActionSystem.ActionResult result = actionExecutor.executeAction(player, action.getActionDefinition(), context);
        logger.debug(result.toString());
        if (result.isFailure()) {
            logger.warn("Action execution failed for player " + player.getName() + ": " + result.message());
            if (result.message() != null) {
                player.sendMessage(result.message());
            }
        } else {
            logger.debug("Successfully executed action for player " + player.getName() + ": " + onClickAction);
        }
    }


    private void handleMultipleActions(FormPlayer player, String onClickAction, ActionSystem.ActionContext context) {
        try {

            String actionsString = onClickAction.substring(1, onClickAction.length() - 1);

            List<ActionSystem.Action> actions = new ArrayList<>();


            for (String actionString : splitActionSequence(actionsString)) {
                String trimmed = actionString.trim();
                if (!trimmed.isEmpty()) {
                    ActionSystem.Action action = actionExecutor.parseAction(trimmed);
                    if (action != null) {
                        actions.add(action);
                    } else {
                        logger.warn("Failed to parse action in multi-action sequence: " + trimmed);
                        player.sendMessage(messageData.getValue(MessageData.FORMS_INVALID_ACTION_SEQUENCE, Map.of("action", trimmed), player));
                        return;
                    }
                }
            }

            if (actions.isEmpty()) {
                logger.warn("No valid actions found in multi-action sequence for player: " + player.getName());
                player.sendMessage(messageData.getValue(MessageData.FORMS_NO_VALID_ACTIONS, null, player));
                return;
            }


            List<ActionSystem.ActionResult> results = actionExecutor.executeActions(player, actions, context);


            for (int i = 0; i < results.size(); i++) {
                ActionSystem.ActionResult result = results.get(i);
                if (result.isFailure()) {
                    logger.warn("Action " + (i + 1) + " failed for player " + player.getName() + ": " + result.message());
                    player.sendMessage(result.message() != null ? result.message()
                            : messageData.getValue(MessageData.FORMS_SEQUENCE_PARTIAL_FAILURE, null, player));
                    break;
                }
            }

        } catch (Exception e) {
            logger.error("Error executing multi-action sequence for player " + player.getName(), e);
            player.sendMessage(messageData.getValue(MessageData.FORMS_SEQUENCE_ERROR, null, player));
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


        Map<String, String> customPlaceholders = new HashMap<>();
        if (value != null) {
            value = PlaceholderUtil.sanitizeInput(value);
            customPlaceholders.put("value", value);
            customPlaceholders.put("1", value);
            action = PlaceholderUtil.processDynamicPlaceholders(action, Map.of("1", value));
        }

        ActionSystem.ActionContext context = PlaceholderUtil.createContextWithBuiltinPlaceholders(player, customPlaceholders, messageData);

        String trimmedAction = action.trim();
        if (trimmedAction.startsWith("[") && trimmedAction.endsWith("]")) {
            handleMultipleActions(player, trimmedAction, context);
            return;
        }

        ActionSystem.Action parsedAction = actionExecutor.parseAction(action);
        if (parsedAction == null) {
            logger.warn("Failed to parse custom action: " + action);
            player.sendMessage(messageData.getValue(MessageData.FORMS_INVALID_ACTION_FORMAT, null, player));
            return;
        }

        ActionSystem.ActionResult result = actionExecutor.executeAction(player, parsedAction.getActionDefinition(), context);

        if (result.isFailure()) {
            logger.warn("Custom action execution failed for player " + player.getName() + ": " + result.message());
            if (result.message() != null) {
                player.sendMessage(result.message());
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


        result = PlaceholderUtil.processDynamicPlaceholders(result,
                PlaceholderUtil.createContextWithBuiltinPlaceholders(player, placeholders, messageData).getPlaceholders());


        if (messageData != null && result.contains("%")) {
            result = messageData.replaceVariables(result, null, player);
        }

        // Always apply color codes translation at the end so '&' and '&x' hex sequences render
        if (messageData != null) {
            result = messageData.applyColor(result);
        }

        return result;
    }

    private List<String> resolveDropdownOptions(Object rawOptions, Map<String, String> placeholders, FormPlayer player, MessageData messageData) {
        if (rawOptions == null) {
            return List.of("");
        }

        List<String> baseOptions;
        if (rawOptions instanceof List) {
            baseOptions = new ArrayList<>();
            for (Object option : (List<?>) rawOptions) {
                if (option == null) {
                    continue;
                }
                baseOptions.add(String.valueOf(option));
            }
        } else {
            baseOptions = List.of(String.valueOf(rawOptions));
        }

        boolean rawIsString = rawOptions instanceof String;

        List<String> resolved = new ArrayList<>();
        for (String raw : baseOptions) {
            if (raw == null) {
                continue;
            }

            String rawTrimmed = raw.trim();
            if (ONLINE_PLAYERS_TOKEN.equalsIgnoreCase(rawTrimmed)) {
                if (playerManager != null) {
                    playerManager.getOnlinePlayerNames().stream()
                            .filter(name -> !name.equalsIgnoreCase(player.getName()))
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .forEach(resolved::add);
                }
                continue;
            }
            String processed = replacePlaceholders(raw, placeholders, player, messageData);

            boolean splitAsCsv = isSinglePercentPlaceholderToken(rawTrimmed) || (rawIsString && rawTrimmed.contains(","));
            if (splitAsCsv) {
                for (String part : processed.split(",")) {
                    String candidate = part.trim();
                    if (!candidate.isEmpty()) {
                        resolved.add(candidate);
                    }
                }
            } else {
                resolved.add(processed);
            }
        }

        if (resolved.isEmpty()) {
            resolved.add("");
        }

        return resolved;
    }

    private boolean isSinglePercentPlaceholderToken(String raw) {
        if (raw == null) {
            return false;
        }
        String trimmed = raw.trim();
        if (trimmed.length() < 2 || !trimmed.startsWith("%") || !trimmed.endsWith("%")) {
            return false;
        }
        int secondPercent = trimmed.indexOf('%', 1);
        return secondPercent == trimmed.length() - 1;
    }


    public boolean hasMenu(String menuName) {
        if (menuName == null) {
            return false;
        }
        return formMenus.containsKey(menuName.toLowerCase());
    }

    public void registerActionHandler(ActionSystem.ActionHandler handler) {
        actionRegistry.registerHandler(handler);
        logger.info("Registered custom action handler: " + handler.getActionType());
    }


    public ActionRegistry getActionRegistry() {
        return actionRegistry;
    }


    public ActionExecutor getActionExecutor() {
        return actionExecutor;
    }


    public void shutdown() {
        if (assetServer != null) {
            try {
                assetServer.shutdown();
            } catch (Exception ignored) {
            }
        }
        logger.info("FormMenuUtil shutdown completed");
    }


    private static String stripColors(String text) {
        return text == null ? null : text.replaceAll("(?i)[§&]([0-9a-fk-orx]|#[0-9a-f]{6})", "").trim();
    }

    private void validateConfiguration() {
        ConfigValidator validator = new ConfigValidator(messageData, actionRegistry);
        ConfigValidator.ValidationResult result = validator.validateConfiguration(formMenus);

        if (result.hasErrors()) {
            logger.warn("Configuration validation found " + result.getErrors().size() + " errors:");
            for (String error : result.getErrors()) {
                logger.warn("  - " + stripColors(error));
            }
        }

        if (result.hasWarnings()) {
            logger.info("Configuration validation found " + result.getWarnings().size() + " warnings:");
            for (String warning : result.getWarnings()) {
                logger.info("  - " + stripColors(warning));
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


    private String getEffectiveButtonText(FormButton button, FormPlayer player, ActionSystem.ActionContext context, Map<String, String> placeholders, MessageData messageData, boolean conditionFailed) {
        if (button instanceof ConditionalButton) {
            ConditionalButton conditionalButton = (ConditionalButton) button;


            String matchedCondition = null;
            for (Map.Entry<String, ConditionalButton.ConditionalProperty> entry : conditionalButton.getConditionalProperties().entrySet()) {
                ConditionalButton.ConditionalProperty property = entry.getValue();
                String condition = property.getCondition() != null ? property.getCondition() : entry.getKey();

                if ("text".equals(property.getProperty()) && ConditionEvaluator.evaluateCondition(player, condition, context, messageData)) {
                    matchedCondition = condition;
                    break;
                }
            }


            String effectiveText = conditionalButton.getEffectiveText(matchedCondition, conditionFailed);
            return replacePlaceholders(effectiveText, placeholders, player, messageData);
        }


        return replacePlaceholders(button.getText(), placeholders, player, messageData);
    }


    private String getEffectiveButtonImage(FormButton button, FormPlayer player, ActionSystem.ActionContext context, boolean conditionFailed) {
        if (button instanceof ConditionalButton) {
            ConditionalButton conditionalButton = (ConditionalButton) button;


            String matchedCondition = null;
            for (Map.Entry<String, ConditionalButton.ConditionalProperty> entry : conditionalButton.getConditionalProperties().entrySet()) {
                ConditionalButton.ConditionalProperty property = entry.getValue();
                String condition = property.getCondition() != null ? property.getCondition() : entry.getKey();

                if ("image".equals(property.getProperty()) && ConditionEvaluator.evaluateCondition(player, condition, context, messageData)) {
                    matchedCondition = condition;
                    break;
                }
            }


            return conditionalButton.getEffectiveImage(matchedCondition, conditionFailed);
        }


        String src = button.getImage();
        return src;
    }


    private ActionSystem.ActionDefinition convertOnClickToActionDefinition(String onClick) {
        if (onClick == null || onClick.trim().isEmpty()) {
            return null;
        }

        ActionSystem.ActionDefinition actionDef = new ActionSystem.ActionDefinition();


        if (onClick.startsWith("[") && onClick.endsWith("]")) {

            String actionsString = onClick.substring(1, onClick.length() - 1);

            for (String actionString : splitActionSequence(actionsString)) {
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

    private List<String> splitActionSequence(String actionsString) {
        List<String> out = new ArrayList<>();
        if (actionsString == null || actionsString.isEmpty()) return out;

        StringBuilder current = new StringBuilder();
        int braceDepth = 0;
        boolean inQuotes = false;
        boolean escaped = false;

        for (int i = 0; i < actionsString.length(); i++) {
            char c = actionsString.charAt(i);

            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }

            if (inQuotes && c == '\\') {
                current.append(c);
                escaped = true;
                continue;
            }

            if (c == '"') {
                current.append(c);
                inQuotes = !inQuotes;
                continue;
            }

            if (!inQuotes) {
                if (c == '{') {
                    current.append(c);
                    braceDepth++;
                    continue;
                }
                if (c == '}') {
                    current.append(c);
                    if (braceDepth > 0) braceDepth--;
                    continue;
                }
                if (c == ',' && braceDepth == 0) {
                    out.add(current.toString());
                    current.setLength(0);
                    continue;
                }
            }

            current.append(c);
        }

        if (current.length() > 0) out.add(current.toString());
        return out;
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

    private String getEffectiveButtonOnClick(FormButton button, FormPlayer player, ActionSystem.ActionContext context, boolean conditionFailed) {
        if (button instanceof ConditionalButton) {
            ConditionalButton conditionalButton = (ConditionalButton) button;


            String matchedCondition = null;
            for (Map.Entry<String, ConditionalButton.ConditionalProperty> entry : conditionalButton.getConditionalProperties().entrySet()) {
                ConditionalButton.ConditionalProperty property = entry.getValue();
                String condition = property.getCondition() != null ? property.getCondition() : entry.getKey();

                if ("onClick".equals(property.getProperty()) && ConditionEvaluator.evaluateCondition(player, condition, context, messageData)) {
                    matchedCondition = condition;
                    break;
                }
            }


            return conditionalButton.getEffectiveOnClick(matchedCondition, conditionFailed);
        }


        return button.getOnClick();
    }

    public void setAssetServer(AssetServer assetServer) {
        this.assetServer = assetServer;
    }

    private String mapImageSource(String image) {
        if (image == null) return null;
        String trimmed = image.trim();
        if (ValidationUtils.isNullOrEmpty(trimmed)) return null;

        if (trimmed.startsWith("http://textures.minecraft.net/texture/") ||
            trimmed.startsWith("https://textures.minecraft.net/texture/")) {
            String hash = trimmed.substring(trimmed.lastIndexOf('/') + 1);
            return "https://mc-heads.net/head/" + hash + "/64";
        }

        if (trimmed.matches("^[A-Za-z0-9+/=]+$") && trimmed.length() > 40) {
            try {
                byte[] decoded = java.util.Base64.getDecoder().decode(trimmed);
                String json = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"url\"\\s*:\\s*\"https?://textures\\.minecraft\\.net/texture/([^\"]+)\"");
                java.util.regex.Matcher m = p.matcher(json);
                if (m.find()) {
                    String hash = m.group(1);
                    return "https://mc-heads.net/head/" + hash + "/64";
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        String resolved = it.pintux.life.common.utils.IconResolver.resolveImage(trimmed);
        if (resolved != null && it.pintux.life.common.utils.IconResolver.isLocalImageFile(resolved)) {
            return assetServer != null && assetServer.isAvailable() ? assetServer.getAssetUrl(resolved) : resolved;
        }
        return resolved;
    }
}
