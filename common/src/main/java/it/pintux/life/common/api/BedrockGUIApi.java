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


public class BedrockGUIApi {

    private static final Logger logger = Logger.getLogger(BedrockGUIApi.class);
    private static BedrockGUIApi instance;

    private final FormMenuUtil formMenuUtil;
    private final ActionExecutor actionExecutor;
    private final ActionRegistry actionRegistry;
    private final PlatformFormSender formSender;
    private final MessageData messageData;
    private final PlatformTitleManager platformTitleManager;


    private final Map<String, DynamicForm> dynamicForms = new HashMap<>();


    private final Map<String, FormTemplate> formTemplates = new HashMap<>();


    private final Map<String, List<FormValidator>> formValidators = new HashMap<>();

    public BedrockGUIApi(FormConfig config, MessageData messageData,
                         PlatformCommandExecutor commandExecutor,
                         PlatformSoundManager soundManager,
                         PlatformEconomyManager economyManager,
                         PlatformFormSender formSender,
                         PlatformTitleManager platformTitleManager,
                         PlatformPluginManager pluginManager,
                         PlatformPlayerManager playerManager) {
        this.messageData = messageData;
        this.formSender = formSender;
        this.platformTitleManager = platformTitleManager;
        this.formMenuUtil = new FormMenuUtil(config, messageData, commandExecutor, soundManager, economyManager, formSender, platformTitleManager, pluginManager, playerManager);
        this.actionExecutor = formMenuUtil.getActionExecutor();
        this.actionRegistry = formMenuUtil.getActionRegistry();

        instance = this;
        logger.info("BedrockGUIApi initialized with enhanced features");
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
                return new InputComponentBuilder(text, defaultValue, "");
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
            default:
                throw new IllegalArgumentException("Unknown component type: " + type);
        }
    }


    public FormBuilder createFromTemplate(String templateName, Map<String, Object> parameters) {
        FormTemplate template = formTemplates.get(templateName);
        if (template == null) {
            throw new IllegalArgumentException("Template not found: " + templateName);
        }
        return template.createForm(parameters);
    }


    public void registerDynamicForm(String formId, DynamicForm form) {
        dynamicForms.put(formId, form);
        logger.info("Registered dynamic form: " + formId);
    }


    public DynamicForm getDynamicForm(String formId) {
        return dynamicForms.get(formId);
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
                    "Â§eForm could not be displayed. Please try using commands or contact an administrator."
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


    public void addFormValidator(String formType, FormValidator validator) {
        formValidators.computeIfAbsent(formType, k -> new ArrayList<>()).add(validator);
    }


    public ValidationResult validateForm(String formType, Map<String, Object> formData, FormPlayer player) {
        List<FormValidator> validators = formValidators.get(formType);
        if (validators == null || validators.isEmpty()) {
            return ValidationResult.success();
        }

        for (FormValidator validator : validators) {
            ValidationResult result = validator.validate(formData, player);
            if (!result.isValid()) {
                return result;
            }
        }

        return ValidationResult.success();
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


    public void shutdown() {
        formMenuUtil.shutdown();
        dynamicForms.clear();
        formTemplates.clear();
        formValidators.clear();
        logger.info("BedrockGUIApi shutdown completed");
    }


    public abstract class FormBuilder {
        protected String title;
        protected String content;
        protected Map<String, String> placeholders = new HashMap<>();
        protected List<String> permissions = new ArrayList<>();
        protected List<FormValidator> validators = new ArrayList<>();
        protected Consumer<FormPlayer> onOpen;
        protected Consumer<FormPlayer> onClose;

        public FormBuilder(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        public FormBuilder content(String content) {
            this.content = content;
            return this;
        }

        public FormBuilder placeholder(String key, String value) {
            this.placeholders.put(key, value);
            return this;
        }

        public FormBuilder placeholders(Map<String, String> placeholders) {
            this.placeholders.putAll(placeholders);
            return this;
        }

        public FormBuilder requirePermission(String permission) {
            this.permissions.add(permission);
            return this;
        }

        public FormBuilder validator(FormValidator validator) {
            this.validators.add(validator);
            return this;
        }

        public FormBuilder onOpen(Consumer<FormPlayer> callback) {
            this.onOpen = callback;
            return this;
        }

        public FormBuilder onClose(Consumer<FormPlayer> callback) {
            this.onClose = callback;
            return this;
        }

        protected boolean hasPermission(FormPlayer player) {
            return permissions.isEmpty() || permissions.stream().allMatch(player::hasPermission);
        }

        protected String processPlaceholders(String text, FormPlayer player) {
            return PlaceholderUtil.processPlaceholders(text, placeholders, player, messageData);
        }

        public abstract Form build();

        public CompletableFuture<FormResult> send(FormPlayer player) {
            if (!hasPermission(player)) {
                return CompletableFuture.completedFuture(FormResult.failure("Insufficient permissions"));
            }

            if (onOpen != null) {
                onOpen.accept(player);
            }

            return openForm(player, build());
        }
    }


    public class SimpleFormBuilder extends FormBuilder {
        private List<FormButtonBuilder> buttons = new ArrayList<>();
        private FormPlayer currentPlayer;

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
            SimpleForm.Builder builder = SimpleForm.builder()
                    .title(title);

            if (content != null) {
                builder.content(content);
            }

            List<Consumer<FormPlayer>> clickHandlers = new ArrayList<>();

            for (FormButtonBuilder buttonBuilder : buttons) {
                FormButtonData buttonData = buttonBuilder.build();

                if (buttonData.image != null) {
                    builder.button(buttonData.text, FormImage.Type.URL, buttonData.image);
                } else {
                    builder.button(buttonData.text);
                }

                clickHandlers.add(buttonData.onClick);
            }

            builder.validResultHandler((form, response) -> {
                int buttonId = response.clickedButtonId();
                if (buttonId >= 0 && buttonId < clickHandlers.size()) {
                    Consumer<FormPlayer> handler = clickHandlers.get(buttonId);
                    if (handler != null && currentPlayer != null) {
                        handler.accept(currentPlayer);
                    } else if (handler != null) {
                        logger.warn("Button clicked but FormPlayer context not available");
                    }
                }
            });

            return builder.build();
        }

        @Override
        public CompletableFuture<FormResult> send(FormPlayer player) {
            this.currentPlayer = player;


            List<Consumer<FormPlayer>> clickHandlers = new ArrayList<>();
            for (FormButtonBuilder buttonBuilder : buttons) {
                FormButtonData buttonData = buttonBuilder.build();
                clickHandlers.add(buttonData.onClick);
            }


            SimpleForm.Builder builder = SimpleForm.builder();
            builder.title(title);
            if (content != null) {
                builder.content(content);
            }

            for (FormButtonBuilder buttonBuilder : buttons) {
                FormButtonData buttonData = buttonBuilder.build();
                if (buttonData.image != null) {
                    builder.button(buttonData.text, FormImage.Type.URL, buttonData.image);
                } else {
                    builder.button(buttonData.text);
                }
            }

            builder.validResultHandler((form, response) -> {
                int buttonId = response.clickedButtonId();
                if (buttonId >= 0 && buttonId < clickHandlers.size()) {
                    Consumer<FormPlayer> handler = clickHandlers.get(buttonId);
                    if (handler != null) {
                        handler.accept(player);
                    }
                }
            });

            return openForm(player, builder.build());
        }
    }


    public class ModalFormBuilder extends FormBuilder {
        private String button1Text = "Yes";
        private String button2Text = "No";
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
            ModalForm.Builder builder = ModalForm.builder()
                    .title(title)
                    .button1(button1Text)
                    .button2(button2Text);

            if (content != null) {
                builder.content(content);
            }

            builder.validResultHandler((form, response) -> {
                if (response.clickedButtonId() == 0 && onButton1 != null) {

                } else if (response.clickedButtonId() == 1 && onButton2 != null) {

                }
            });

            return builder.build();
        }
    }


    public class CustomFormBuilder extends FormBuilder {
        private List<FormComponentBuilder> components = new ArrayList<>();
        private Consumer<Map<String, Object>> onSubmit;
        private BiConsumer<FormPlayer, Map<String, Object>> onSubmitWithPlayer;

        public CustomFormBuilder(String title) {
            super(title);
        }

        public CustomFormBuilder input(String text, String placeholder, String defaultValue) {
            components.add(new InputComponentBuilder(text, placeholder, defaultValue));
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
            CustomForm.Builder builder = CustomForm.builder()
                    .title(title);

            for (FormComponentBuilder component : components) {
                component.addToForm(builder);
            }

            builder.validResultHandler((form, response) -> {
                if (onSubmit != null) {
                    Map<String, Object> results = new HashMap<>();
                    for (int i = 0; i < components.size(); i++) {
                        FormComponentBuilder component = components.get(i);
                        Object value = component.extractValue(response, i);
                        results.put(component.getName(), value);
                    }
                    onSubmit.accept(results);
                }
            });

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
            this.name = text.toLowerCase().replaceAll("\\s+", "_");
        }

        public FormComponentBuilder name(String name) {
            this.name = name;
            return this;
        }

        public String getName() {
            return name;
        }

        public abstract void addToForm(CustomForm.Builder builder);

        public abstract Object extractValue(org.geysermc.cumulus.response.CustomFormResponse response, int index);
    }


    public class InputComponentBuilder extends FormComponentBuilder {
        private String placeholder;
        private String defaultValue;

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
        public Object extractValue(org.geysermc.cumulus.response.CustomFormResponse response, int index) {
            return response.asInput(index);
        }
    }


    public class SliderComponentBuilder extends FormComponentBuilder {
        private int min, max, step, defaultValue;

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
        public Object extractValue(org.geysermc.cumulus.response.CustomFormResponse response, int index) {
            return (int) response.asSlider(index);
        }
    }


    public class DropdownComponentBuilder extends FormComponentBuilder {
        private List<String> options;
        private int defaultIndex;

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
        public Object extractValue(org.geysermc.cumulus.response.CustomFormResponse response, int index) {
            int selectedIndex = response.asDropdown(index);
            return selectedIndex >= 0 && selectedIndex < options.size() ? options.get(selectedIndex) : null;
        }
    }


    public class ToggleComponentBuilder extends FormComponentBuilder {
        private boolean defaultValue;

        public ToggleComponentBuilder(String text, boolean defaultValue) {
            super(text);
            this.defaultValue = defaultValue;
        }

        @Override
        public void addToForm(CustomForm.Builder builder) {
            builder.toggle(text, defaultValue);
        }

        @Override
        public Object extractValue(org.geysermc.cumulus.response.CustomFormResponse response, int index) {
            return response.asToggle(index);
        }
    }


    public class DynamicFormBuilder extends FormBuilder {
        private Map<String, Predicate<Map<String, Object>>> conditions = new HashMap<>();
        private Map<String, List<FormComponentBuilder>> conditionalComponents = new HashMap<>();
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
        public Form build() {

            CustomFormBuilder builder = createCustomForm(title);


            for (Map.Entry<String, List<FormComponentBuilder>> entry : conditionalComponents.entrySet()) {
                String conditionName = entry.getKey();
                List<FormComponentBuilder> components = entry.getValue();


                if ("always".equals(conditionName) ||
                    (conditions.containsKey(conditionName) && conditions.get(conditionName).test(new HashMap<>()))) {
                    for (FormComponentBuilder component : components) {
                        if (component instanceof InputComponentBuilder) {
                            InputComponentBuilder inputComponent = (InputComponentBuilder) component;
                            builder.input(inputComponent.text, inputComponent.placeholder, inputComponent.defaultValue);
                        } else if (component instanceof ToggleComponentBuilder) {
                            ToggleComponentBuilder toggleComponent = (ToggleComponentBuilder) component;
                            builder.toggle(toggleComponent.text, toggleComponent.defaultValue);
                        } else if (component instanceof DropdownComponentBuilder) {
                            DropdownComponentBuilder dropdownComponent = (DropdownComponentBuilder) component;
                            builder.dropdown(dropdownComponent.text, dropdownComponent.options, dropdownComponent.defaultIndex);
                        } else if (component instanceof SliderComponentBuilder) {
                            SliderComponentBuilder sliderComponent = (SliderComponentBuilder) component;
                            builder.slider(sliderComponent.text, sliderComponent.min, sliderComponent.max,
                                    sliderComponent.step, sliderComponent.defaultValue);
                        }
                    }
                }
            }


            if (onSubmitCallback != null) {
                builder.onSubmit((player, results) -> {
                    onSubmitCallback.accept(player, results);
                });
            }

            return builder.build();
        }
    }


    public interface DynamicForm {
        Form generateForm(FormPlayer player, Map<String, Object> context);

        void updateForm(String property, Object value);

        boolean isValid();
    }


    public interface FormTemplate {
        FormBuilder createForm(Map<String, Object> parameters);

        String getTemplateName();

        List<String> getRequiredParameters();
    }


    public interface FormValidator {
        ValidationResult validate(Map<String, Object> formData, FormPlayer player);

        String getValidatorName();
    }


    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        private final List<String> errors;

        private ValidationResult(boolean valid, String message, List<String> errors) {
            this.valid = valid;
            this.message = message;
            this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message, null);
        }

        public static ValidationResult failure(String message, List<String> errors) {
            return new ValidationResult(false, message, errors);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }
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

