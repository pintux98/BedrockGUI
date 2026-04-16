# BedrockGUI API Reference and Implementation Guide

## Table of Contents
- [1. API Overview](#1-api-overview)
- [2. Getting Started](#2-getting-started)
- [3. Form Creation Guide](#3-form-creation-guide)
  - [3.1 Simple Forms](#31-simple-forms)
  - [3.2 Modal Dialogs](#32-modal-dialogs)
  - [3.3 Custom Forms](#33-custom-forms)
  - [3.4 Dynamic Form Layouts](#34-dynamic-form-layouts)
- [4. Validation Patterns](#4-validation-patterns)
- [5. Action Registration and Callback Patterns](#5-action-registration-and-callback-patterns)
  - [5.1 Custom Action Handlers](#51-custom-action-handlers)
  - [5.2 Event Listener and Callback Patterns](#52-event-listener-and-callback-patterns)
- [6. Built-in Actions and Extension Patterns](#6-built-in-actions-and-extension-patterns)
- [7. Configuration, Styling, and Localization](#7-configuration-styling-and-localization)
- [8. Error Handling and Troubleshooting](#8-error-handling-and-troubleshooting)
- [9. Integration Examples](#9-integration-examples)
- [10. Performance Optimization](#10-performance-optimization)
- [11. Testing Guide](#11-testing-guide)
- [12. Comprehensive API Reference](#12-comprehensive-api-reference)
- [13. Real-World Scenarios](#13-real-world-scenarios)

## 1. API Overview
- `BedrockGUIApi` is the high-level integration point for:
  - Creating and opening Bedrock forms.
  - Executing configured or programmatic actions.
  - Registering custom action handlers and validators.
  - Managing reload and shutdown lifecycle.
- It wraps core subsystems:
  - `FormMenuUtil` for menu loading, parsing, and opening.
  - `ActionExecutor` and `ActionRegistry` for action processing.
  - `FormSender` for Bedrock-specific form delivery.
  - `MessageData` for localized, placeholder-aware user messages.
- Primary source file:
  - `common/src/main/java/it/pintux/life/common/api/BedrockGUIApi.java`

## 2. Getting Started
- `BedrockGUIApi` is initialized by platform entry points (Paper, Velocity, BungeeCord) with platform abstractions.
- Typical flow:
  1. Acquire instance with `BedrockGUIApi.getInstance()`.
  2. Verify target player can receive forms with `canReceiveForms`.
  3. Build a form (simple/modal/custom/dynamic).
  4. Send it with `builder.send(player)` or `api.openForm(player, form)`.
  5. Execute actions through callbacks or action strings.

### Minimal usage example
```java
BedrockGUIApi api = BedrockGUIApi.getInstance();

if (!api.canReceiveForms(player)) {
    player.sendMessage(api.getMessageData().getValue(
        MessageData.MENU_NOJAVA, null, player
    ));
    return;
}

api.createSimpleForm("Main Hub")
    .content("Welcome, {player}")
    .button("Open Shop", p -> api.openMenu(p, "shop_menu"))
    .button("Info", p -> p.sendMessage("Hello " + p.getName()))
    .send(player);
```

## 3. Form Creation Guide

### 3.1 Simple Forms
- Use when you need a list of clickable options.
- API entrypoint: `createSimpleForm(String title)`.
- Add text and buttons; each button can run callback logic.

#### Step-by-step
1. Create builder with title.
2. Set content.
3. Add buttons (optional images).
4. Send to player.

```java
BedrockGUIApi api = BedrockGUIApi.getInstance();

api.createSimpleForm("Server Panel")
    .content("Choose an action")
    .button("Teleport Spawn", p -> api.executeActionString(
        p,
        "server { - \"tp {player} world 0 100 0\" }",
        api.createActionContext(null, null, null, "server_panel", "SIMPLE")
    ))
    .button("Open Store", "textures/items/emerald", p -> api.openMenu(p, "store"))
    .send(player);
```

### 3.2 Modal Dialogs
- Use for confirmation/choice prompts with exactly two outcomes.
- API entrypoints:
  - `createModalForm(String title)`
  - `createModalForm(String title, String content)`
- Set button handlers and optional submit callback.

#### Step-by-step
1. Create modal builder.
2. Configure buttons and labels.
3. Attach callbacks for each path.
4. Send form.

```java
BedrockGUIApi api = BedrockGUIApi.getInstance();

api.createModalForm("Confirm Purchase", "Buy this item for 250?")
    .buttons("Confirm", "Cancel")
    .button1("Confirm", p -> api.executeActionString(
        p,
        "economy { - \"check:250\" - \"remove:250\" }",
        api.createActionContext(null, null, null, "confirm_purchase", "MODAL")
    ))
    .button2("Cancel", p -> p.sendMessage("Purchase cancelled"))
    .onSubmit((p, accepted) -> {
        if (!accepted) {
            p.sendMessage("No changes applied");
        }
    })
    .send(player);
```

### 3.3 Custom Forms
- Use for structured inputs (`input`, `slider`, `dropdown`, `toggle`).
- API entrypoint: `createCustomForm(String title)`.
- Components are extracted into a result map by component name.

#### Step-by-step with validation
1. Create custom builder.
2. Add components with clear names.
3. Handle submission data.
4. Validate via `addFormValidator` + `validateForm`.
5. Execute actions only on valid data.

```java
BedrockGUIApi api = BedrockGUIApi.getInstance();

api.addFormValidator("feedback", (formData, p) -> {
    String name = String.valueOf(formData.getOrDefault("player_name", ""));
    if (name.isBlank()) {
        return BedrockGUIApi.ValidationResult.failure("Name is required");
    }
    Object ratingObj = formData.get("rating");
    int rating = ratingObj instanceof Integer ? (Integer) ratingObj : 0;
    if (rating < 1 || rating > 10) {
        return BedrockGUIApi.ValidationResult.failure("Rating must be between 1 and 10");
    }
    return BedrockGUIApi.ValidationResult.success();
});

api.createCustomForm("Feedback Form")
    .input("Player Name", "Steve", "")
    .slider("Rating", 1, 10, 1, 5)
    .dropdown("Category", List.of("Bug", "Suggestion", "Other"), 0)
    .toggle("Allow Contact", true)
    .onSubmit(results -> {
        Map<String, Object> normalized = new HashMap<>();
        normalized.put("player_name", results.get("player_name"));
        normalized.put("rating", results.get("rating"));
        normalized.put("category", results.get("category"));
        normalized.put("allow_contact", results.get("allow_contact"));

        BedrockGUIApi.ValidationResult validation =
            api.validateForm("feedback", normalized, player);
        if (!validation.isValid()) {
            player.sendMessage(validation.getMessage());
            return;
        }

        ActionSystem.ActionContext context = api.createActionContext(
            Map.of("player", player.getName()),
            normalized,
            Map.of("source", "feedback_form"),
            "feedback_form",
            "CUSTOM"
        );
        api.executeActionString(
            player,
            "message { - \"Thanks {player}, rating: $rating\" }",
            context
        );
    })
    .send(player);
```

### 3.4 Dynamic Form Layouts
- Use when form components depend on runtime conditions.
- API entrypoint: `createDynamicForm(String title)`.
- Supports named conditions with conditional component groups.

```java
BedrockGUIApi api = BedrockGUIApi.getInstance();

api.createDynamicForm("Profile Setup")
    .addCondition("is_vip", data -> player.hasPermission("group.vip"))
    .addComponent("always", api.new InputComponentBuilder("Nickname", "Type nickname", ""))
    .addComponent("always", api.new ToggleComponentBuilder("Public Profile", true))
    .addComponent("is_vip", api.new DropdownComponentBuilder(
        "VIP Color", List.of("Gold", "Aqua", "Purple"), 0
    ))
    .onSubmit((p, results) -> {
        p.sendMessage("Saved profile for " + p.getName());
    })
    .send(player);
```

## 4. Validation Patterns
- Use API-level validators for reusable cross-form logic:
  - `addFormValidator(String formType, FormValidator validator)`
  - `validateForm(String formType, Map<String, Object> formData, FormPlayer player)`
- Return:
  - `ValidationResult.success()`
  - `ValidationResult.failure(message)`
  - `ValidationResult.failure(message, errors)`

### Recommended validation strategy
1. Normalize form field names to stable keys.
2. Run local null/range checks in callback.
3. Run reusable API validators by form type.
4. Return user-facing localized messages via `MessageData`.

## 5. Action Registration and Callback Patterns

### 5.1 Custom Action Handlers
- Implement `ActionSystem.ActionHandler`.
- Register with `api.registerActionHandler(handler)`.
- Execute through menu config, action strings, or `executeAction`.

```java
public final class LogActionHandler implements ActionSystem.ActionHandler {
    @Override
    public String getActionType() {
        return "log";
    }

    @Override
    public ActionSystem.ActionResult execute(
            FormPlayer player,
            String actionValue,
            ActionSystem.ActionContext context
    ) {
        String message = actionValue.replace("{player}", player.getName());
        System.out.println("[BedrockGUI-LOG] " + message);
        return ActionSystem.ActionResult.success("Logged custom message");
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return actionValue != null && !actionValue.isBlank();
    }

    @Override
    public String getDescription() {
        return "Logs a formatted message to console";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[] {
            "log { - \"Player {player} accepted terms\" }"
        };
    }
}
```

```java
BedrockGUIApi api = BedrockGUIApi.getInstance();
api.registerActionHandler(new LogActionHandler());

ActionSystem.ActionContext context = api.createActionContext(
    Map.of("player", player.getName()),
    null,
    Map.of("trace", UUID.randomUUID().toString()),
    "terms_menu",
    "MODAL"
);

api.executeActionString(player, "log { - \"Accepted by {player}\" }", context);
```

### 5.2 Event Listener and Callback Patterns
- Form lifecycle callbacks:
  - `FormBuilder.onOpen(Consumer<FormPlayer>)`
  - `FormBuilder.onClose(Consumer<FormPlayer>)`
- Button callbacks:
  - `SimpleFormBuilder.button(..., Consumer<FormPlayer> onClick)`
  - `ModalFormBuilder.button1/button2`
- Submission callbacks:
  - `ModalFormBuilder.onSubmit(BiConsumer<FormPlayer, Boolean>)`
  - `CustomFormBuilder.onSubmit(Consumer<Map<String, Object>>)`
  - `DynamicFormBuilder.onSubmit(BiConsumer<FormPlayer, Map<String,Object>>)`

### Callback best practices
- Keep callbacks non-blocking; offload expensive work.
- Build `ActionContext` once and pass it through chained actions.
- Use metadata entries for tracing and debugging.
- Always guard against null/empty form values.

## 6. Built-in Actions and Extension Patterns
- Default registered actions include:
  - `command`, `open`, `message`, `delay`, `conditional`, `random`, `bungee`
  - Optional (based on platform services): `server`, `broadcast`, `inventory`, `sound`, `economy`, `title`, `actionbar`
- Built-in action registration happens in `FormMenuUtil.registerDefaultActionHandlers`.

### Built-in action usage examples
```yaml
onClick:
  - |
    message {
      - "&aWelcome {player}!"
    }
  - |
    sound {
      - "ui.button.click:0.8:1.0"
    }
  - |
    conditional {
      check: "permission:vip.access"
      true:
        - "title:VIP Access:Enabled:10:40:10"
      false:
        - "message:&cVIP required"
    }
```

### Extending built-in behavior
- Wrap built-in calls with custom logic:
  1. Pre-check values (permissions, balances, external service state).
  2. Execute built-in action.
  3. Add fallback message or follow-up action.

```java
ActionSystem.ActionContext context = api.createActionContext(
    Map.of("player", player.getName()),
    Map.of("target_menu", "vip_store"),
    Map.of("flow", "upgrade_path"),
    "upgrade_menu",
    "SIMPLE"
);

if (player.hasPermission("vip.access")) {
    api.executeActionString(player, "open { - \"vip_store\" }", context);
} else {
    api.executeActionString(player, "message { - \"&cYou need VIP\" }", context);
}
```

## 7. Configuration, Styling, and Localization

### Form configuration options
- Bedrock menu keys under `forms.<menu_name>`:
  - `type`: `SIMPLE | MODAL | CUSTOM`
  - `title`, `content`, `description`, `permission`
  - `buttons.<id>.text`, `buttons.<id>.image`, `buttons.<id>.onClick`
  - `buttons.<id>.show_condition`
  - `buttons.<id>.alternative_text`, `alternative_image`, `alternative_onClick`
  - `buttons.<id>.conditions.<id>.condition/property/value`
  - `components.<componentKey>` for custom forms
  - `global_actions` for post-submit actions

### Styling options
- Text styling:
  - Legacy color format with `&` supported in messages.
  - Hex color support in platform message config implementations.
- Button imagery:
  - Internal Bedrock texture path when prefix is `textures/`.
  - URL image otherwise.
- Java menu bridge options:
  - `forms.<menu>.java.type`, `size`, `items`, `fills` for hybrid Bedrock/Java UX.

### Localization support
- Localized values are fetched through `MessageData`.
- Constants in `MessageData` map to `messages.yml` keys.
- Placeholder expansion supports:
  - `$key` and `{key}` replacements.
  - platform placeholders via `setPlaceholders`.
  - player fallback tokens `%player_name%`, `%player%`, `%player_uuid%`.

```yaml
prefix: "&8[&6BedrockGUI&8]"
menu:
  noJava: "&cThis menu is available only for Bedrock players."
action:
  success: "&aAction completed."
validation:
  unknown_action_type: "&cUnknown action type: $action_type"
```

## 8. Error Handling and Troubleshooting

### Error-handling patterns in the API
- `openForm` checks platform eligibility using `canReceiveForms`.
- Form sending uses `ErrorHandlingUtil.sendFormWithFallback` with retry semantics.
- Action execution path validates format, handler existence, and action value.
- Failure results are returned as:
  - `FormResult.failure(message)`
  - `ActionResult.failure(message)`

### Common troubleshooting scenarios
- Form not shown to user:
  - Ensure player is Bedrock (`canReceiveForms`).
  - Ensure Floodgate/session integration is active.
  - Verify fallback `menu.noJava` message is configured.
- Menu not found:
  - Check `hasMenu(menuName)` and `getMenuNames()`.
  - Verify YAML path and form file loading.
- Action ignored:
  - Validate syntax and registered handler type.
  - Inspect `ActionRegistry` content and `ConfigValidator` warnings.
- Modal fails to load:
  - Modal menus must define exactly two buttons in config.
- Validation not applied:
  - API-level validators run only when `validateForm` is called explicitly.

### Known implementation caveats
- `CustomFormBuilder.onSubmit(BiConsumer<FormPlayer, Map<String,Object>>)` stores callback state but is not invoked by `build()` in current implementation.
- `FormBuilder.validator(...)` stores validators, but builder send path does not automatically execute them.
- `FormButtonBuilder.condition(...)` exists as a model field; simple form rendering path does not evaluate this predicate automatically.

## 9. Integration Examples

### Integrating with command execution
```java
api.createSimpleForm("Admin Tools")
    .button("Reload Config", p -> api.reloadConfiguration())
    .button("Run Maintenance", p -> api.executeActionString(
        p,
        "server { - \"say Starting maintenance\" - \"save-all\" }",
        api.createActionContext(null, null, Map.of("source", "admin_tools"), "admin_tools", "SIMPLE")
    ))
    .send(player);
```

### Integrating with economy and titles
```java
ActionSystem.ActionContext purchaseContext = api.createActionContext(
    Map.of("player", player.getName()),
    Map.of("price", 500),
    null,
    "shop",
    "SIMPLE"
);

api.executeActionString(player, "economy { - \"check:500\" - \"remove:500\" }", purchaseContext);
api.executeActionString(player, "title { - \"Purchase Complete:Thanks!:10:50:10\" }", purchaseContext);
```

### Integrating with scheduler-friendly async logic
```java
CompletableFuture
    .supplyAsync(() -> "Fetched remote profile")
    .thenAccept(result -> api.executeActionString(
        player,
        "message { - \"&a" + result + "\" }",
        api.createActionContext(null, null, Map.of("source", "remote_profile"), "profile", "CUSTOM")
    ));
```

## 10. Performance Optimization
- Reuse templates for repeated form structures:
  - `registerTemplate(name, template)`
  - `createFromTemplate(name, params)`
- Keep action chains short and explicit for high-frequency interactions.
- Prefer one form open operation per user interaction step.
- Avoid heavy synchronous work inside button/submit callbacks.
- Cache frequently reused lists/options for large dropdown forms.
- For frequent updates:
  - Update config/message instances using `updateFormConfig` and `updateMessageData`.
  - Call `reloadConfiguration` only when full form reload is required.
- Use concise placeholders and context maps to reduce object churn.

## 11. Testing Guide
- The repository currently has no dedicated `src/test` coverage for `BedrockGUIApi`.
- Recommended strategy:
  - Unit tests for action handlers and validator logic.
  - Integration tests for menu loading and action execution paths.
  - Configuration validation tests using representative YAML files.

### Unit test example (validator)
```java
class FeedbackValidatorTest {
    @org.junit.jupiter.api.Test
    void rejectsBlankName() {
        BedrockGUIApi.FormValidator validator = new BedrockGUIApi.FormValidator() {
            @Override
            public BedrockGUIApi.ValidationResult validate(Map<String, Object> formData, FormPlayer player) {
                String value = String.valueOf(formData.getOrDefault("name", ""));
                return value.isBlank()
                    ? BedrockGUIApi.ValidationResult.failure("Name required")
                    : BedrockGUIApi.ValidationResult.success();
            }

            @Override
            public String getValidatorName() {
                return "feedback_name";
            }
        };

        BedrockGUIApi.ValidationResult result = validator.validate(Map.of("name", ""), null);
        org.junit.jupiter.api.Assertions.assertFalse(result.isValid());
    }
}
```

### Unit test example (custom action handler)
```java
class LogActionHandlerTest {
    @org.junit.jupiter.api.Test
    void returnsSuccessForNonEmptyValue() {
        ActionSystem.ActionHandler handler = new LogActionHandler();
        ActionSystem.ActionResult result = handler.execute(
            new TestFormPlayer("Steve"),
            "hello",
            ActionSystem.ActionContext.builder().build()
        );
        org.junit.jupiter.api.Assertions.assertTrue(result.isSuccess());
    }
}
```

### Integration test pattern
1. Build a test config with forms and actions.
2. Initialize `BedrockGUIApi` with mocked platform services.
3. Register test action handlers.
4. Open forms using test players.
5. Assert action results, fallback messages, and callbacks.

## 12. Comprehensive API Reference

### 12.1 BedrockGUIApi Core Methods
| Method | Parameters | Returns | Description |
|---|---|---|---|
| `getInstance()` | none | `BedrockGUIApi` | Returns singleton instance; throws if not initialized. |
| `reloadConfiguration()` | none | `void` | Reloads menus through `FormMenuUtil`. |
| `shutdown()` | none | `void` | Shuts down API internals and clears templates/validators. |
| `getActionExecutor()` | none | `ActionExecutor` | Accesses action execution engine. |
| `getActionRegistry()` | none | `ActionRegistry` | Accesses handler registry. |
| `getFormSender()` | none | `FormSender` | Accesses Bedrock form sender abstraction. |
| `getPlatformTitleManager()` | none | `PlatformTitleManager` | Accesses title/actionbar platform manager. |
| `setAssetServer(AssetServer)` | `assetServer` | `void` | Injects asset server and forwards to menu util. |
| `getAssetServer()` | none | `AssetServer` | Returns configured asset server. |
| `setJavaMenuManager(PlatformJavaMenuManager)` | `javaMenuManager` | `void` | Injects Java inventory menu manager. |
| `getJavaMenuManager()` | none | `PlatformJavaMenuManager` | Returns Java menu manager. |
| `updateMessageData(MessageData)` | `newMessageData` | `void` | Replaces message provider reference during reload. |
| `updateFormConfig(FormConfig)` | `newConfig` | `void` | Updates config source in underlying form util. |
| `createSimpleForm(String)` | `title` | `SimpleFormBuilder` | Creates simple form builder. |
| `createModalForm(String)` | `title` | `ModalFormBuilder` | Creates modal builder with default button labels. |
| `createModalForm(String,String)` | `title`, `content` | `ModalFormBuilder` | Creates modal with immediate content setup. |
| `createCustomForm(String)` | `title` | `CustomFormBuilder` | Creates custom builder. |
| `createDynamicForm(String)` | `title` | `DynamicFormBuilder` | Creates dynamic form builder. |
| `createComponent(String,String,String)` | `type`, `text`, `defaultValue` | `FormComponentBuilder` | Creates input component by string type. |
| `createComponent(String,String,boolean)` | `type`, `text`, `defaultValue` | `FormComponentBuilder` | Creates toggle component by string type. |
| `createComponent(String,String,List<String>)` | `type`, `text`, `options` | `FormComponentBuilder` | Creates dropdown component by string type. |
| `registerTemplate(String,FormTemplate)` | `templateName`, `template` | `void` | Registers reusable form template factory. |
| `createFromTemplate(String,Map<String,Object>)` | `templateName`, `parameters` | `FormBuilder` | Creates form builder from registered template. |
| `openForm(FormPlayer,Form)` | `player`, `form` | `CompletableFuture<FormResult>` | Opens built form with default empty context. |
| `openForm(FormPlayer,Form,Map<String,Object>)` | `player`, `form`, `context` | `CompletableFuture<FormResult>` | Opens form with explicit context map. |
| `openMenu(FormPlayer,String,String...)` | `player`, `menuName`, `args` | `void` | Opens configured menu by name. |
| `executeActionString(FormPlayer,String,ActionContext)` | `player`, `actionString`, `context` | `ActionResult` | Parses and executes string action. |
| `createActionContext(...)` | placeholders, formResults, metadata, menuName, formType | `ActionContext` | Builds action context from optional map inputs. |
| `registerActionHandler(ActionHandler)` | `handler` | `void` | Registers custom action handler. |
| `executeAction(FormPlayer,ActionDefinition,ActionContext)` | `player`, `action`, `context` | `ActionResult` | Executes single action definition. |
| `executeActions(FormPlayer,List<Action>,ActionContext)` | `player`, `actions`, `context` | `List<ActionResult>` | Executes action sequence. |
| `addFormValidator(String,FormValidator)` | `formType`, `validator` | `void` | Adds reusable validator by form type. |
| `validateForm(String,Map<String,Object>,FormPlayer)` | `formType`, `formData`, `player` | `ValidationResult` | Runs all validators for type until first failure. |
| `canReceiveForms(FormPlayer)` | `player` | `boolean` | Checks if user is Bedrock-capable via sender. |
| `hasMenu(String)` | `menuName` | `boolean` | Tests configured menu existence. |
| `getMenuNames()` | none | `Set<String>` | Returns loaded menu names. |
| `getFormMenuUtil()` | none | `FormMenuUtil` | Returns low-level menu utility. |
| `getMessageData()` | none | `MessageData` | Returns message provider. |

### 12.2 Builder and Nested Types
| Class/Interface | Key Methods |
|---|---|
| `FormBuilder` | `content`, `placeholder`, `placeholders`, `requirePermission`, `validator`, `onOpen`, `onClose`, `build`, `send` |
| `SimpleFormBuilder` | `button(text,onClick)`, `button(text,image,onClick)`, `addButton`, `build`, `send` |
| `ModalFormBuilder` | `button1`, `button2`, `buttons`, `onSubmit`, `build`, `send` |
| `CustomFormBuilder` | `input`, `slider`, `dropdown`, `toggle`, `onSubmit(Consumer)`, `onSubmit(BiConsumer)`, `build` |
| `DynamicFormBuilder` | `addCondition`, `addComponent`, `onSubmit`, `build` |
| `FormButtonBuilder` | `image`, `onClick`, `condition`, `build` |
| `FormComponentBuilder` | `name`, `getName`, `addToForm`, `extractValue` |
| `InputComponentBuilder` | constructor + extraction with `asInput` |
| `SliderComponentBuilder` | constructor + extraction with `asSlider` |
| `DropdownComponentBuilder` | constructor + extraction with `asDropdown` |
| `ToggleComponentBuilder` | constructor + extraction with `asToggle` |
| `FormTemplate` | `createForm`, `getTemplateName`, `getRequiredParameters` |
| `FormValidator` | `validate`, `getValidatorName` |
| `ValidationResult` | `success`, `failure`, `isValid`, `getMessage`, `getErrors` |
| `FormResult` | `success`, `failure`, `isSuccess`, `getMessage`, `getData` |

### 12.3 Related Constants and Enums
- `MessageData` constants define localization keys used by API and handlers:
  - `MENU_NOJAVA`, `MENU_NOT_FOUND`, `ACTION_SUCCESS`, `EXECUTION_ERROR`,
    `SYSTEM_SERVICE_UNAVAILABLE`, `VALIDATION_UNKNOWN_ACTION_TYPE`, and others.
- `ActionSystem.ActionResult.Status` enum:
  - `SUCCESS`, `FAILURE`, `PARTIAL_SUCCESS`, `SKIPPED`.

## 13. Real-World Scenarios
- Player onboarding:
  - Show simple form with server rules and quick navigation.
  - Use modal confirmation before granting starter kits.
- Marketplace flow:
  - Open custom form to pick quantity/options.
  - Validate values, run economy actions, then show title feedback.
- Staff operations:
  - Build admin panel with conditional buttons by permission.
  - Chain `message`, `server`, and `broadcast` actions for operational workflows.
- Multi-platform bridge:
  - Keep Bedrock UX via forms and Java UX via Java menu configuration in the same menu definition.
