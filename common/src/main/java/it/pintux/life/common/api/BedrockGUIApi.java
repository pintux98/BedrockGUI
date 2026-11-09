package it.pintux.life.common.api;
import it.pintux.life.common.actions.ActionSystem;

import it.pintux.life.common.actions.*;
import it.pintux.life.common.form.FormMenuUtil;
import it.pintux.life.common.platform.*;
import it.pintux.life.common.utils.*;
import it.pintux.life.common.utils.ErrorHandlingUtil;
import it.pintux.life.common.utils.Logger;
import org.geysermc.cumulus.form.*;
import org.geysermc.cumulus.util.FormImage;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;


public class BedrockGUIApi {

    private static final Logger logger = Logger.getLogger(BedrockGUIApi.class.getSimpleName());
    private static BedrockGUIApi instance;

    private final FormMenuUtil formMenuUtil;
    private final ActionExecutor actionExecutor;
    private final ActionRegistry actionRegistry;
    private final FormSender formSender;
    private final MessageData messageData;
    private final PlatformTitleManager platformTitleManager;
    private AssetServer assetServer;


    private final Map<String, FormTemplate> formTemplates = new HashMap<>();

    public BedrockGUIApi(FormConfig config, MessageData messageData,
                         PlatformCommandExecutor commandExecutor,
                         PlatformSoundManager soundManager,
                         PlatformEconomyManager economyManager,
                         FormSender formSender,
                         PlatformTitleManager platformTitleManager,
                         PlatformPluginManager pluginManager,
                         PlatformPlayerManager playerManager,
                         it.pintux.life.common.platform.PlatformScheduler scheduler,
                         it.pintux.life.common.platform.PlatformInventoryManager inventoryManager) {
        this.messageData = messageData;
        this.formSender = formSender;
        this.platformTitleManager = platformTitleManager;
        this.formMenuUtil = new FormMenuUtil(config, messageData, commandExecutor, soundManager, economyManager, formSender, platformTitleManager, pluginManager, playerManager, scheduler, inventoryManager);
        this.actionExecutor = formMenuUtil.getActionExecutor();
        this.actionRegistry = formMenuUtil.getActionRegistry();

        instance = this;
        logger.info("BedrockGUIApi initialized");
    }


    public static BedrockGUIApi getInstance() {
        if (instance == null) {
            throw new IllegalStateException("BedrockGUIApi has not been initialized yet");
        }
        return instance;
    }


    public void reloadConfiguration() {
        logger.info("Reloading BedrockGUI configuration...");

        if (formMenuUtil != null) {
            formMenuUtil.reloadFormMenus();
            logger.info("Configuration reloaded successfully");
        } else {
            logger.warn("FormMenuUtil is null, cannot reload configuration");
        }
    }

    public ActionExecutor getActionExecutor() {
        return actionExecutor;
    }

    public ActionRegistry getActionRegistry() {
        return actionRegistry;
    }

    public FormSender getFormSender() {
        return formSender;
    }

    public PlatformTitleManager getPlatformTitleManager() {
        return platformTitleManager;
    }

    public void setAssetServer(AssetServer assetServer) {
        this.assetServer = assetServer;
        if (formMenuUtil != null) {
            formMenuUtil.setAssetServer(assetServer);
        }
    }

    public AssetServer getAssetServer() {
        return assetServer;
    }

    /**
     * Resolve a button image the same way a config-defined menu does, so an API caller can name a
     * material, a {@code head:}/{@code POTION:} value, a texture path, a URL or a local file.
     *
     * @param image the raw image value
     * @return the value to hand to the client, or null when it resolves to no image
     */
    private String resolveButtonImage(String image) {
        String resolved = IconResolver.resolveImage(image);
        if (resolved != null && assetServer != null && assetServer.isAvailable()
                && IconResolver.isLocalImageFile(resolved)) {
            return assetServer.getAssetUrl(resolved);
        }
        return resolved;
    }


    public SimpleFormBuilder createSimpleForm(String title) {
        return new SimpleFormBuilder(title);
    }


    public ModalFormBuilder createModalForm(String title) {
        return new ModalFormBuilder(title);
    }


    public ModalFormBuilder createModalForm(String title, String content) {
        ModalFormBuilder builder = new ModalFormBuilder(title);
        builder.content(content);
        return builder;
    }


    public CustomFormBuilder createCustomForm(String title) {
        return new CustomFormBuilder(title);
    }


    public DynamicFormBuilder createDynamicForm(String title) {
        return new DynamicFormBuilder(title);
    }


    public FormComponentBuilder createComponent(String type, String text, String defaultValue) {
        switch (type.toLowerCase()) {
            case "input":
                return new InputComponentBuilder(text, "", defaultValue);
            default:
                throw new IllegalArgumentException("Unknown component type: " + type);
        }
    }


    public FormComponentBuilder createComponent(String type, String text, boolean defaultValue) {
        switch (type.toLowerCase()) {
            case "toggle":
                return new ToggleComponentBuilder(text, defaultValue);
            default:
                throw new IllegalArgumentException("Unknown component type: " + type);
        }
    }


    public FormComponentBuilder createComponent(String type, String text, List<String> options) {
        switch (type.toLowerCase()) {
            case "dropdown":
                return new DropdownComponentBuilder(text, options, 0);
            case "step_slider":
                return new StepSliderComponentBuilder(text, options, 0);
            default:
                throw new IllegalArgumentException("Unknown component type: " + type);
        }
    }


    public FormBuilder<?> createFromTemplate(String templateName, Map<String, Object> parameters) {
        FormTemplate template = formTemplates.get(templateName);
        if (template == null) {
            throw new IllegalArgumentException("Template not found: " + templateName);
        }
        return template.createForm(parameters);
    }

    public void registerTemplate(String templateName, FormTemplate template) {
        formTemplates.put(templateName, template);
        logger.info("Registered form template: " + templateName);
    }


    public CompletableFuture<FormResult> openForm(FormPlayer player, Form form) {
        return openForm(player, form, new HashMap<>());
    }


    public CompletableFuture<FormResult> openForm(FormPlayer player, Form form, Map<String, Object> context) {
        CompletableFuture<FormResult> future = new CompletableFuture<>();

        try {

            if (!canReceiveForms(player)) {
                future.complete(FormResult.failure("Player cannot receive forms"));
                return future;
            }


            boolean sent = ErrorHandlingUtil.sendFormWithFallback(
                    player,
                    () -> formSender.sendForm(player, form),
                    messageData.getValue(MessageData.MENU_NOJAVA, null, player)
            );

            if (sent) {
                future.complete(FormResult.success("Form sent successfully"));
            } else {
                future.complete(FormResult.failure("Failed to send form after retries"));
            }

        } catch (Exception e) {
            logger.error("Error opening form for player: " + player.getName(), e);
            future.complete(FormResult.failure("Unexpected error: " + e.getMessage()));
        }

        return future;
    }


    public void openMenu(FormPlayer player, String menuName, String... args) {
        formMenuUtil.openForm(player, menuName, args);
    }

    public ActionSystem.ActionResult executeActionString(FormPlayer player, String actionString, ActionSystem.ActionContext context) {
        try {
            ActionSystem.Action action = actionExecutor.parseAction(actionString);
            if (action == null) {
                return ActionSystem.ActionResult.failure("Invalid action format");
            }
            return actionExecutor.executeAction(player, action.getActionDefinition(), context);
        } catch (Exception e) {
            return ActionSystem.ActionResult.failure("Action execution failed: " + e.getMessage());
        }
    }

    public ActionSystem.ActionContext createActionContext(Map<String, String> placeholders,
                                                          Map<String, Object> formResults,
                                                          Map<String, Object> metadata,
                                                          String menuName,
                                                          String formType) {
        ActionSystem.ActionContext.Builder b = ActionSystem.ActionContext.builder();
        if (placeholders != null) b.placeholders(placeholders);
        if (formResults != null) b.formResults(formResults);
        if (metadata != null) b.metadata(metadata);
        if (menuName != null) b.menuName(menuName);
        if (formType != null) b.formType(formType);
        return b.build();
    }


    public void registerActionHandler(ActionSystem.ActionHandler handler) {
        formMenuUtil.registerActionHandler(handler);
    }


    public ActionSystem.ActionResult executeAction(FormPlayer player, ActionSystem.ActionDefinition action, ActionSystem.ActionContext context) {
        return actionExecutor.executeAction(player, action, context);
    }


    public List<ActionSystem.ActionResult> executeActions(FormPlayer player, List<ActionSystem.Action> actions, ActionSystem.ActionContext context) {
        return actionExecutor.executeActions(player, actions, context);
    }


    public boolean canReceiveForms(FormPlayer player) {
        return formSender.isBedrockPlayer(player.getUniqueId());
    }


    public boolean hasMenu(String menuName) {
        return formMenuUtil.hasMenu(menuName);
    }


    public Set<String> getMenuNames() {
        return formMenuUtil.getFormMenus().keySet();
    }


    public FormMenuUtil getFormMenuUtil() {
        return formMenuUtil;
    }


    public MessageData getMessageData() {
        return messageData;
    }

    public PlaceholderRegistry getPlaceholderRegistry() {
        return messageData != null ? messageData.getPlaceholderRegistry() : PlaceholderRegistry.shared();
    }

    public String formatText(FormPlayer player, String text) {
        return formatText(player, text, null);
    }

    public String formatText(FormPlayer player, String text, Map<String, String> placeholders) {
        if (text == null) {
            return null;
        }
        String result = PlaceholderUtil.processPlaceholders(text, placeholders, player, messageData);
        return messageData != null ? messageData.applyColor(result) : result;
    }


    public void shutdown() {
        formMenuUtil.shutdown();
        formTemplates.clear();
        if (instance == this) {
            instance = null;
        }
        logger.info("BedrockGUIApi shutdown completed");
    }


    public abstract class FormBuilder<B extends FormBuilder<B>> {
        protected String title;
        protected String content;
        protected Map<String, String> placeholders = new HashMap<>();
        protected List<String> permissions = new ArrayList<>();
        protected Consumer<FormPlayer> onOpen;
        protected Consumer<FormPlayer> onClose;
        protected FormPlayer currentPlayer;

        public FormBuilder(String title) {
            this.title = title;
        }

        @SuppressWarnings("unchecked")
        protected B self() {
            return (B) this;
        }

        public String getTitle() {
            return title;
        }

        public B content(String content) {
            this.content = content;
            return self();
        }

        public B placeholder(String key, String value) {
            this.placeholders.put(key, value);
            return self();
        }

        public B placeholders(Map<String, String> placeholders) {
            this.placeholders.putAll(placeholders);
            return self();
        }

        public B requirePermission(String permission) {
            this.permissions.add(permission);
            return self();
        }

        public B onOpen(Consumer<FormPlayer> callback) {
            this.onOpen = callback;
            return self();
        }

        public B onClose(Consumer<FormPlayer> callback) {
            this.onClose = callback;
            return self();
        }

        protected boolean hasPermission(FormPlayer player) {
            if (permissions.isEmpty()) {
                return true;
            }
            return player != null && permissions.stream().allMatch(player::hasPermission);
        }

        protected String processPlaceholders(String text, FormPlayer player) {
            return PlaceholderUtil.processPlaceholders(text, placeholders, player, messageData);
        }

        protected String resolveText(String text) {
            if (text == null) {
                return null;
            }
            String result = processPlaceholders(text, currentPlayer);
            return messageData != null ? messageData.applyColor(result) : result;
        }

        protected UnaryOperator<String> textResolver() {
            return this::resolveText;
        }

        protected FormResult beforeSend(FormPlayer player) {
            this.currentPlayer = player;

            if (!hasPermission(player)) {
                return FormResult.failure(messageData.getValue(MessageData.MENU_NOPEX, null, player));
            }

            if (onOpen != null) {
                onOpen.accept(player);
            }

            return null;
        }

        protected void registerCloseHandler(org.geysermc.cumulus.form.util.FormBuilder<?, ?, ?> builder, FormPlayer player) {
            if (onClose == null || player == null) {
                return;
            }
            Consumer<FormPlayer> handler = onClose;
            builder.closedResultHandler(() -> handler.accept(player));
        }

        public Form build() {
            return build(currentPlayer);
        }

        public Form build(FormPlayer player) {
            this.currentPlayer = player;
            return buildFor(player);
        }

        protected abstract Form buildFor(FormPlayer player);

        public CompletableFuture<FormResult> send(FormPlayer player) {
            FormResult rejection = beforeSend(player);
            if (rejection != null) {
                return CompletableFuture.completedFuture(rejection);
            }
            return openForm(player, buildFor(player));
        }
    }


    public class SimpleFormBuilder extends FormBuilder<SimpleFormBuilder> {
        private final List<FormButtonBuilder> buttons = new ArrayList<>();

        public SimpleFormBuilder(String title) {
            super(title);
        }

        public SimpleFormBuilder button(String text, Consumer<FormPlayer> onClick) {
            buttons.add(new FormButtonBuilder(text).onClick(onClick));
            return this;
        }

        public SimpleFormBuilder button(String text, String image, Consumer<FormPlayer> onClick) {
            buttons.add(new FormButtonBuilder(text).image(image).onClick(onClick));
            return this;
        }

        public FormButtonBuilder addButton(String text) {
            FormButtonBuilder button = new FormButtonBuilder(text);
            buttons.add(button);
            return button;
        }

        @Override
        public SimpleForm build() {
            return (SimpleForm) super.build();
        }

        @Override
        public SimpleForm build(FormPlayer player) {
            return (SimpleForm) super.build(player);
        }

        @Override
        protected SimpleForm buildFor(FormPlayer player) {
            SimpleForm.Builder builder = SimpleForm.builder().title(resolveText(title));
            if (content != null) {
                builder.content(resolveText(content));
            }

            List<Consumer<FormPlayer>> clickHandlers = new ArrayList<>();
            for (FormButtonBuilder buttonBuilder : buttons) {
                FormButtonData buttonData = buttonBuilder.build();
                if (buttonData.condition != null && (player == null || !buttonData.condition.test(player))) {
                    continue;
                }
                String resolved = buttonData.image != null ? resolveButtonImage(buttonData.image) : null;
                if (resolved != null) {
                    FormImage.Type type = IconResolver.isUrl(resolved) ? FormImage.Type.URL : FormImage.Type.PATH;
                    builder.button(resolveText(buttonData.text), type, resolved);
                } else {
                    builder.button(resolveText(buttonData.text));
                }
                clickHandlers.add(buttonData.onClick);
            }

            builder.validResultHandler((form, response) -> {
                int buttonId = response.clickedButtonId();
                if (buttonId < 0 || buttonId >= clickHandlers.size()) {
                    return;
                }
                Consumer<FormPlayer> handler = clickHandlers.get(buttonId);
                if (handler == null) {
                    return;
                }
                if (player == null) {
                    logger.warn("Button clicked on a form built without a player; use build(player) or send(player)");
                    return;
                }
                handler.accept(player);
            });

            registerCloseHandler(builder, player);
            return builder.build();
        }
    }


    public class ModalFormBuilder extends FormBuilder<ModalFormBuilder> {
        private String button1Text = getMessageData().getValueNoPrefix("forms.modal_yes", null, null);
        private String button2Text = getMessageData().getValueNoPrefix("forms.modal_no", null, null);
        private Consumer<FormPlayer> onButton1;
        private Consumer<FormPlayer> onButton2;
        private BiConsumer<FormPlayer, Boolean> onSubmitHandler;

        public ModalFormBuilder(String title) {
            super(title);
        }

        public ModalFormBuilder button1(String text, Consumer<FormPlayer> onClick) {
            this.button1Text = text;
            this.onButton1 = onClick;
            return this;
        }

        public ModalFormBuilder button2(String text, Consumer<FormPlayer> onClick) {
            this.button2Text = text;
            this.onButton2 = onClick;
            return this;
        }

        public ModalFormBuilder buttons(String button1Text, String button2Text) {
            this.button1Text = button1Text;
            this.button2Text = button2Text;
            return this;
        }

        public ModalFormBuilder onSubmit(BiConsumer<FormPlayer, Boolean> handler) {
            this.onSubmitHandler = handler;
            return this;
        }

        @Override
        public ModalForm build() {
            return (ModalForm) super.build();
        }

        @Override
        public ModalForm build(FormPlayer player) {
            return (ModalForm) super.build(player);
        }

        @Override
        protected ModalForm buildFor(FormPlayer player) {
            ModalForm.Builder builder = ModalForm.builder()
                    .title(resolveText(title))
                    .button1(resolveText(button1Text))
                    .button2(resolveText(button2Text));
            if (content != null) {
                builder.content(resolveText(content));
            }

            Consumer<FormPlayer> first = onButton1;
            Consumer<FormPlayer> second = onButton2;
            BiConsumer<FormPlayer, Boolean> submit = onSubmitHandler;
            builder.validResultHandler((form, response) -> {
                if (player == null) {
                    logger.warn("Modal answered on a form built without a player; use build(player) or send(player)");
                    return;
                }
                boolean firstClicked = response.clickedButtonId() == 0;
                Consumer<FormPlayer> handler = firstClicked ? first : second;
                if (handler != null) {
                    handler.accept(player);
                }
                if (submit != null) {
                    submit.accept(player, firstClicked);
                }
            });

            registerCloseHandler(builder, player);
            return builder.build();
        }
    }


    public class CustomFormBuilder extends FormBuilder<CustomFormBuilder> {
        private final List<FormComponentBuilder> components = new ArrayList<>();
        private Consumer<Map<String, Object>> onSubmit;
        private BiConsumer<FormPlayer, Map<String, Object>> onSubmitWithPlayer;

        public CustomFormBuilder(String title) {
            super(title);
        }

        public CustomFormBuilder input(String text, String placeholder, String defaultValue) {
            components.add(new InputComponentBuilder(text, placeholder, defaultValue));
            return this;
        }

        public CustomFormBuilder namedInput(String name, String text, String placeholder, String defaultValue) {
            InputComponentBuilder component = new InputComponentBuilder(text, placeholder, defaultValue);
            component.name(name);
            components.add(component);
            return this;
        }

        public CustomFormBuilder slider(String text, int min, int max, int step, int defaultValue) {
            components.add(new SliderComponentBuilder(text, min, max, step, defaultValue));
            return this;
        }

        public CustomFormBuilder dropdown(String text, List<String> options, int defaultIndex) {
            components.add(new DropdownComponentBuilder(text, options, defaultIndex));
            return this;
        }

        public CustomFormBuilder toggle(String text, boolean defaultValue) {
            components.add(new ToggleComponentBuilder(text, defaultValue));
            return this;
        }

        public CustomFormBuilder label(String text) {
            components.add(new LabelComponentBuilder(text));
            return this;
        }

        public CustomFormBuilder stepSlider(String text, List<String> steps, int defaultIndex) {
            components.add(new StepSliderComponentBuilder(text, steps, defaultIndex));
            return this;
        }

        public CustomFormBuilder component(FormComponentBuilder component) {
            components.add(component);
            return this;
        }

        public CustomFormBuilder onSubmit(Consumer<Map<String, Object>> callback) {
            this.onSubmit = callback;
            return this;
        }

        public CustomFormBuilder onSubmit(BiConsumer<FormPlayer, Map<String, Object>> callback) {
            this.onSubmitWithPlayer = callback;
            return this;
        }

        @Override
        public CustomForm build() {
            return (CustomForm) super.build();
        }

        @Override
        public CustomForm build(FormPlayer player) {
            return (CustomForm) super.build(player);
        }

        @Override
        protected CustomForm buildFor(FormPlayer player) {
            CustomForm.Builder builder = CustomForm.builder().title(resolveText(title));

            List<FormComponentBuilder> snapshot = new ArrayList<>(components);
            UnaryOperator<String> resolver = textResolver();
            for (FormComponentBuilder component : snapshot) {
                component.addToForm(builder, resolver);
            }

            Consumer<Map<String, Object>> plain = onSubmit;
            BiConsumer<FormPlayer, Map<String, Object>> withPlayer = onSubmitWithPlayer;
            builder.validResultHandler((form, response) -> {
                if (plain == null && withPlayer == null) {
                    return;
                }
                Map<String, Object> results = new LinkedHashMap<>();
                for (int i = 0; i < snapshot.size(); i++) {
                    FormComponentBuilder component = snapshot.get(i);
                    if (component instanceof LabelComponentBuilder) {
                        continue;
                    }
                    Object value = component.extractValue(response, i);
                    String key = component.getName();
                    if (results.containsKey(key)) {
                        key = key + "_" + i;
                    }
                    results.put(key, value);
                }
                if (plain != null) {
                    plain.accept(results);
                }
                if (withPlayer != null) {
                    if (player != null) {
                        withPlayer.accept(player, results);
                    } else {
                        logger.warn("Custom form submitted on a form built without a player; use build(player) or send(player)");
                    }
                }
            });

            registerCloseHandler(builder, player);
            return builder.build();
        }
    }


    public class FormButtonBuilder {
        private String text;
        private String image;
        private Consumer<FormPlayer> onClick;
        private Predicate<FormPlayer> condition;

        public FormButtonBuilder(String text) {
            this.text = text;
        }

        public FormButtonBuilder image(String image) {
            this.image = image;
            return this;
        }

        public FormButtonBuilder onClick(Consumer<FormPlayer> onClick) {
            this.onClick = onClick;
            return this;
        }

        public FormButtonBuilder condition(Predicate<FormPlayer> condition) {
            this.condition = condition;
            return this;
        }

        public FormButtonData build() {
            return new FormButtonData(text, image, onClick, condition);
        }
    }


    public static class FormButtonData {
        public final String text;
        public final String image;
        public final Consumer<FormPlayer> onClick;
        public final Predicate<FormPlayer> condition;

        public FormButtonData(String text, String image, Consumer<FormPlayer> onClick, Predicate<FormPlayer> condition) {
            this.text = text;
            this.image = image;
            this.onClick = onClick;
            this.condition = condition;
        }
    }


    public abstract class FormComponentBuilder {
        protected String name;
        protected String text;

        public FormComponentBuilder(String text) {
            this.text = text;
            this.name = FormComponentNames.derive(text);
        }

        public FormComponentBuilder name(String name) {
            this.name = name;
            return this;
        }

        public String getName() {
            return name;
        }

        public abstract void addToForm(CustomForm.Builder builder);

        public void addToForm(CustomForm.Builder builder, UnaryOperator<String> resolver) {
            addToForm(builder);
        }

        protected String resolve(UnaryOperator<String> resolver, String value) {
            return resolver == null || value == null ? value : resolver.apply(value);
        }

        public abstract Object extractValue(org.geysermc.cumulus.response.CustomFormResponse response, int index);
    }


    public class InputComponentBuilder extends FormComponentBuilder {
        private final String placeholder;
        private final String defaultValue;

        public InputComponentBuilder(String text, String placeholder, String defaultValue) {
            super(text);
            this.placeholder = placeholder;
            this.defaultValue = defaultValue;
        }

        @Override
        public void addToForm(CustomForm.Builder builder) {
            builder.input(text, placeholder, defaultValue);
        }

        @Override
        public void addToForm(CustomForm.Builder builder, UnaryOperator<String> resolver) {
            builder.input(resolve(resolver, text), resolve(resolver, placeholder), resolve(resolver, defaultValue));
        }

        @Override
        public Object extractValue(org.geysermc.cumulus.response.CustomFormResponse response, int index) {
            return response.asInput(index);
        }
    }


    public class SliderComponentBuilder extends FormComponentBuilder {
        private final int min;
        private final int max;
        private final int step;
        private final int defaultValue;

        public SliderComponentBuilder(String text, int min, int max, int step, int defaultValue) {
            super(text);
            this.min = min;
            this.max = max;
            this.step = step;
            this.defaultValue = defaultValue;
        }

        @Override
        public void addToForm(CustomForm.Builder builder) {
            builder.slider(text, min, max, step, defaultValue);
        }

        @Override
        public void addToForm(CustomForm.Builder builder, UnaryOperator<String> resolver) {
            builder.slider(resolve(resolver, text), min, max, step, defaultValue);
        }

        @Override
        public Object extractValue(org.geysermc.cumulus.response.CustomFormResponse response, int index) {
            return (int) response.asSlider(index);
        }
    }


    public class DropdownComponentBuilder extends FormComponentBuilder {
        private final List<String> options;
        private final int defaultIndex;

        public DropdownComponentBuilder(String text, List<String> options, int defaultIndex) {
            super(text);
            this.options = options;
            this.defaultIndex = defaultIndex;
        }

        @Override
        public void addToForm(CustomForm.Builder builder) {
            builder.dropdown(text, options, defaultIndex);
        }

        @Override
        public void addToForm(CustomForm.Builder builder, UnaryOperator<String> resolver) {
            List<String> labels = new ArrayList<>(options.size());
            for (String option : options) {
                labels.add(resolve(resolver, option));
            }
            builder.dropdown(resolve(resolver, text), labels, defaultIndex);
        }

        @Override
        public Object extractValue(org.geysermc.cumulus.response.CustomFormResponse response, int index) {
            int selectedIndex = response.asDropdown(index);
            return selectedIndex >= 0 && selectedIndex < options.size() ? options.get(selectedIndex) : null;
        }
    }


    public class ToggleComponentBuilder extends FormComponentBuilder {
        private final boolean defaultValue;

        public ToggleComponentBuilder(String text, boolean defaultValue) {
            super(text);
            this.defaultValue = defaultValue;
        }

        @Override
        public void addToForm(CustomForm.Builder builder) {
            builder.toggle(text, defaultValue);
        }

        @Override
        public void addToForm(CustomForm.Builder builder, UnaryOperator<String> resolver) {
            builder.toggle(resolve(resolver, text), defaultValue);
        }

        @Override
        public Object extractValue(org.geysermc.cumulus.response.CustomFormResponse response, int index) {
            return response.asToggle(index);
        }
    }


    public class LabelComponentBuilder extends FormComponentBuilder {

        public LabelComponentBuilder(String text) {
            super(text);
        }

        @Override
        public void addToForm(CustomForm.Builder builder) {
            builder.label(text);
        }

        @Override
        public void addToForm(CustomForm.Builder builder, UnaryOperator<String> resolver) {
            builder.label(resolve(resolver, text));
        }

        @Override
        public Object extractValue(org.geysermc.cumulus.response.CustomFormResponse response, int index) {
            return null;
        }
    }


    public class StepSliderComponentBuilder extends FormComponentBuilder {
        private final List<String> steps;
        private final int defaultIndex;

        public StepSliderComponentBuilder(String text, List<String> steps, int defaultIndex) {
            super(text);
            this.steps = steps;
            this.defaultIndex = defaultIndex >= 0 && defaultIndex < steps.size() ? defaultIndex : 0;
        }

        @Override
        public void addToForm(CustomForm.Builder builder) {
            builder.stepSlider(text, steps, defaultIndex);
        }

        @Override
        public void addToForm(CustomForm.Builder builder, UnaryOperator<String> resolver) {
            List<String> labels = new ArrayList<>(steps.size());
            for (String step : steps) {
                labels.add(resolve(resolver, step));
            }
            builder.stepSlider(resolve(resolver, text), labels, defaultIndex);
        }

        @Override
        public Object extractValue(org.geysermc.cumulus.response.CustomFormResponse response, int index) {
            int selected = response.asStepSlider(index);
            return selected >= 0 && selected < steps.size() ? steps.get(selected) : null;
        }
    }


    public class DynamicFormBuilder extends FormBuilder<DynamicFormBuilder> {
        private final Map<String, Predicate<Map<String, Object>>> conditions = new LinkedHashMap<>();
        private final Map<String, List<FormComponentBuilder>> conditionalComponents = new LinkedHashMap<>();
        private BiConsumer<FormPlayer, Map<String, Object>> onSubmitCallback;

        public DynamicFormBuilder(String title) {
            super(title);
        }

        public DynamicFormBuilder addCondition(String conditionName, Predicate<Map<String, Object>> condition) {
            conditions.put(conditionName, condition);
            return this;
        }

        public DynamicFormBuilder addComponent(String conditionName, FormComponentBuilder component) {
            conditionalComponents.computeIfAbsent(conditionName, k -> new ArrayList<>()).add(component);
            return this;
        }

        public DynamicFormBuilder onSubmit(BiConsumer<FormPlayer, Map<String, Object>> callback) {
            this.onSubmitCallback = callback;
            return this;
        }

        @Override
        protected Form buildFor(FormPlayer player) {
            CustomFormBuilder builder = createCustomForm(title);
            builder.placeholders(this.placeholders);
            if (onClose != null) {
                builder.onClose(onClose);
            }

            for (Map.Entry<String, List<FormComponentBuilder>> entry : conditionalComponents.entrySet()) {
                String conditionName = entry.getKey();
                Predicate<Map<String, Object>> condition = conditions.get(conditionName);
                if ("always".equals(conditionName) || (condition != null && condition.test(new HashMap<>()))) {
                    entry.getValue().forEach(builder::component);
                }
            }

            if (onSubmitCallback != null) {
                builder.onSubmit(onSubmitCallback);
            }

            return builder.build(player);
        }
    }


    public interface FormTemplate {
        FormBuilder<?> createForm(Map<String, Object> parameters);

        String getTemplateName();

        List<String> getRequiredParameters();
    }


    public static class FormResult {
        private final boolean success;
        private final String message;
        private final Map<String, Object> data;

        private FormResult(boolean success, String message, Map<String, Object> data) {
            this.success = success;
            this.message = message;
            this.data = data != null ? new HashMap<>(data) : new HashMap<>();
        }

        public static FormResult success(String message) {
            return new FormResult(true, message, null);
        }

        public static FormResult success(String message, Map<String, Object> data) {
            return new FormResult(true, message, data);
        }

        public static FormResult failure(String message) {
            return new FormResult(false, message, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Map<String, Object> getData() {
            return new HashMap<>(data);
        }
    }
}

