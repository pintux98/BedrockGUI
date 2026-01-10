## What The common Module Supports (Opportunities)

* **Bedrock form metadata**: `command`, `command_intercept`, `permission`, `title`, `description/content`, `type` (SIMPLE/MODAL/CUSTOM). See [FormMenu](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/common/src/main/java/it/pintux/life/common/form/obj/FormMenu.java) and YAML load logic in [FormMenuUtil](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/common/src/main/java/it/pintux/life/common/form/FormMenuUtil.java).

* **Global actions**: `bedrock.global_actions` exists and is executed in runtime (already parsed). This can be a first-class editor feature. See [FormMenu](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/common/src/main/java/it/pintux/life/common/form/obj/FormMenu.java#L15-L18) and [FormMenuUtil.java](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/common/src/main/java/it/pintux/life/common/form/FormMenuUtil.java#L147-L155).

* **Conditional buttons**: `show_condition`, `alternative_*`, plus per-condition overrides (`conditions.*.condition/property/value`), plus conditional actions map. See [ConditionalButton](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/common/src/main/java/it/pintux/life/common/form/obj/ConditionalButton.java) and loader in [FormMenuUtil.java](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/common/src/main/java/it/pintux/life/common/form/FormMenuUtil.java#L179-L210).

* **Java menus beyond basic items**:

  * Menu types: `CHEST`, `ANVIL`, `CRAFTING` ([JavaMenuType](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/common/src/main/java/it/pintux/life/common/form/obj/JavaMenuType.java)).

  * Items: material, amount, name, lore, glow, actions list ([JavaMenuItem](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/common/src/main/java/it/pintux/life/common/form/obj/JavaMenuItem.java)).

  * Fill rules: `fills` (multiple) or `fill` (single) with `type: ROW|COLUMN|EMPTY`, optional row/column, template item, and actions ([JavaMenuFill](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/common/src/main/java/it/pintux/life/common/form/obj/JavaMenuFill.java), [JavaFillType](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/common/src/main/java/it/pintux/life/common/form/obj/JavaFillType.java), and parsing in [FormMenuUtil.java](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/common/src/main/java/it/pintux/life/common/form/FormMenuUtil.java#L283-L352)).

* **Action system richness**:

  * Registered action types include `command`, `server`, `broadcast`, `message`, `open`, `delay`, `sound`, `economy`, `title`, `actionbar`, `random`, `conditional`, `bungee`, `url` (some are commented out at registration but handlers exist). See handler list via [FormMenuUtil.registerDefaultActionHandlers](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/common/src/main/java/it/pintux/life/common/form/FormMenuUtil.java#L79-L113) and handler types in [handlers](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/common/src/main/java/it/pintux/life/common/actions/handlers/).

  * Conditional actions support boolean expressions with `&&`/`||` and placeholder/permission checks (new-format block parsing). See [ConditionalActionHandler](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/common/src/main/java/it/pintux/life/common/actions/handlers/ConditionalActionHandler.java).

  * Runtime also supports “new unified block format” parsing in executor (`type { - "..." }`) and legacy `type: value`. See [ActionExecutor](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/common/src/main/java/it/pintux/life/common/actions/ActionExecutor.java).

* **Editor-side validation**: The backend has strong validation rules and warnings (unknown action types, legacy format detection, modal requires exactly 2 buttons, invalid image, invalid condition syntax, circular references). See [ConfigValidator](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/common/src/main/java/it/pintux/life/common/utils/ConfigValidator.java).

* **Per-form external file support**: `forms.<name>.file` allows splitting config into separate files with `bedrock:` and `java:` roots. See [FormMenuUtil.loadFormMenus](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/common/src/main/java/it/pintux/life/common/form/FormMenuUtil.java#L115-L131).

## What To Add To The Web Editor (Concrete Features)

1. **Bedrock: command intercept + command args schema**

   * Add `command_intercept` field.

   * Add UI for defining required args / placeholders `$1..$n` and preview how placeholders resolve.
2. **Bedrock: Global Actions editor**

   * A separate section that edits `global_actions` (same action builder as button onClick).
3. **Bedrock: Conditional Button editor**

   * Toggle “Conditional button” per button.

   * Fields for `show_condition`, `alternative_text/image/onClick`.

   * “Conditional overrides” table: condition + property (text/image/onClick) + value.

   * Optional “conditional actions” map (condition → action block), aligned with `ConditionalButton` capabilities.
4. **Java: Fill rules editor**

   * UI to add multiple fills (`fills`) and/or fallback single (`fill`).

   * Support type ROW/COLUMN/EMPTY, row/column targeting, and fill-item template + its onClick actions.
5. **Java: Item options completion**

   * Expose `glow` in UI.

   * Allow editing actions per item as a list (already parsed in backend).

   * Add menu type `CRAFTING` editor + correct slot constraints.
6. **Action builder: match backend formats**

   * Add a “format mode” per action: simple (`type: value`) vs block (`type { - "..." }`).

   * First-class builders for `conditional` and `random` actions (so users don’t hand-write complex blocks).
7. **Validation panel matching ConfigValidator**

   * Show Errors + Warnings in the editor (modal button count, unknown action types, legacy format detection, invalid images, invalid condition strings, circular open-form references).
8. **Project export structure**

   * Add export option: single YAML vs “split into per-form files” (generate `forms.<name>.file` and separate YAML bodies with `bedrock:`/`java:` roots).

## Implementation Plan

1. **Align the designer-app schema/types** to include missing fields: `command_intercept`, `global_actions`, conditional button fields, java `fills`/`fill`, `glow`, and `CRAFTING`.
2. **Update import/export mapping** so YAML round-trips exactly as `FormMenuUtil` expects (including list vs string onClick support and slot key parsing).
3. **Build UI editors**:

   * Global actions section

   * Conditional button section

   * Java fills editor

   * Glow + crafting menu support
4. **Add a validation panel** that ports key rules from `ConfigValidator` (and a circular reference graph for open-form actions).
5. **Update previews** to visualize:

   * Conditional button “active state” using a simulated condition toggle

   * Java fill results (preview what a fill would populate)
6. **Tests**

   * Unit tests for YAML import/export of new fields

   * UI tests verifying conditional button editing + java fill editing

## Confirm Scope

If you confirm, I’ll implement items 1–5 first (feature parity), then add the validator + tests.
