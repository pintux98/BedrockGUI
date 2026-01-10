## Repository Findings
- Java-based Minecraft plugin with Paper and Velocity modules; no web frontend present.
- Bedrock forms via GeyserMC Cumulus; Java menus via Bukkit inventories.
- YAML configuration schema used for forms and menus with validator and builder logic: see [BedrockGUI-Guide.md](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/docs/BedrockGUI-Guide.md), [FormMenuUtil.java](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/common/src/main/java/it/pintux/life/common/form/FormMenuUtil.java#L115-L604), [ConfigValidator.java](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/common/src/main/java/it/pintux/life/common/utils/ConfigValidator.java#L58-L94).

## Goals
- Build a self-contained, interactive web app to visually design Bedrock forms and Java menus.
- Provide accurate real-time preview, drag-and-drop composition, and YAML export compatible with the plugin.
- Ensure extensibility for new components, actions, and features, with strong validation and testing.

## Tech Stack
- Frontend: React + TypeScript + Vite for fast dev/build, ES modules.
- State management: Zustand (lightweight) or Redux Toolkit (predictable updates). Prefer Zustand for simplicity.
- Drag-and-drop: @dnd-kit for accessible, flexible DnD.
- Forms & validation: Zod for schema-driven validation; react-hook-form for ergonomics in property panels.
- Styling: Tailwind CSS for rapid UI, plus CSS variables for theme; fallback to CSS Modules if Tailwind not preferred.
- YAML: js-yaml for export/import.
- Testing: Vitest (unit) + Playwright (e2e).
- Docs in-app: MDX/Markdown rendering with a help panel referencing existing guide.

## Folder Structure
- c:/Users/pintu/Desktop/Server/BedrockGUI/designer-app
  - src/
    - app/ (App shell, routing, layout, error boundary)
    - core/ (types, schemas, registries, validators)
    - components/ (UI primitives and editors)
    - canvas/ (drag-drop canvas and preview renderers)
    - panels/ (palette, properties, actions, features)
    - exporters/ (YAML exporters and migrators)
    - importers/ (schema-based import/parsing)
    - tests/ (unit specs)
  - e2e/ (Playwright tests)
  - public/ (icons, mock assets)
  - package.json, vite.config.ts, tailwind.config.cjs

## Data Models & Schemas
- Define shared config model aligned with plugin:
  - Root: forms.<menu_name> supports inline or file reference: see [FormMenuUtil.java:L115-L132](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/common/src/main/java/it/pintux/life/common/form/FormMenuUtil.java#L115-L132).
  - Bedrock form types: SIMPLE|MODAL|CUSTOM with title, content, permission, buttons/components, global_actions.
  - Java menu: type CHEST|ANVIL|CRAFTING with title, size, items by slot, item props (material, amount, name, lore, glow), onClick.
- Zod schemas mirror validator rules: [ConfigValidator.java:L58-L94](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/common/src/main/java/it/pintux/life/common/utils/ConfigValidator.java#L58-L94).
- Action interface: id, label, parameters schema (Zod), serialization to YAML; support curly-brace open syntax used in plugin: [OpenFormActionHandler.java:L33-L66](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/common/src/main/java/it/pintux/life/common/actions/handlers/OpenFormActionHandler.java#L33-L66).
- Versioning: add configVersion field; maintain migrators for future changes.

## Component System
- Modular registry for components (palette):
  - Bedrock primitives: Button, ImageButton, Toggle, Input, Dropdown, Label, Slider, Stepper, ToggleGroup, CustomLayout container.
  - Java primitives: InventoryGrid, SlotItem, AnvilText, CraftingGrid.
- Each component: metadata, props schema (Zod), defaultProps, renderer for preview, serializer for export.
- Add/remove/edit via property panel bound to selected component; undo/redo stack.

## Drag-and-Drop & Preview
- Canvas supports drag from palette, drop into form/layout; reorder via DnD.
- Real-time preview:
  - Bedrock: simulate Cumulus style (fonts, button spacing, image icons via data URL), layout approximations.
  - Java: inventory grid (size rows), slot items with material icons, glow effect visualization.
- Zoom, snap, alignment guides; multi-select; keyboard shortcuts.

## Action Framework
- ActionRegistry with built-in actions (open form, run command, close, message, server, permission-sensitive actions).
- Parameter editors generated from schema, with validation and helpful defaults.
- Pre-export validation ensures all required params; show inline errors.
- Extensible interface: addAction({ id, schema, serialize, UI }); auto-registered in actions panel.

## Java & Bedrock Feature Integration
- FeatureRegistry with toggles for platform-specific options:
  - Bedrock: image button support, modal confirmations, permission gate, global_actions.
  - Java: chest size, anvil text entry, crafting grid templates, item glow.
- Config panel for each feature; warnings when mixing unsupported features.
- Clear docs for adding new Java features.

## Export & Import
- Export to plugin-compatible YAML:
  - Inline export under forms.<menu_name> for quick use.
  - External file export with bedrock/ and java sections for split configs.
- Ensure keys match guide examples: [common/resources/config.yml](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/common/src/main/resources/config.yml), [paper/resources/config.yml](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/paper/src/main/resources/config.yml).
- Validate against Zod before writing; include configVersion.
- Import existing YAML to resume editing; migration applies if versions differ.

## Error Handling & UX
- ErrorBoundary wrapping app shell; fallback UI with retry.
- Form-level validation messages; parameter tooltips; toasts for save/export/import.
- Autosave to localStorage; confirm on unsaved exit.
- Accessibility: keyboard nav, focus outlines, ARIA labels.

## Testing
- Unit tests (Vitest):
  - Schema validators for Bedrock/Java.
  - Component serializers; action serialization.
  - Exporter roundtrip (model → YAML → model).
- E2E (Playwright):
  - Create SIMPLE Bedrock form with buttons and actions; preview and export.
  - Create CUSTOM Bedrock form with components; export and re-import.
  - Create CHEST Java menu with items; export and re-import.
  - Validation blocks invalid actions; user feedback.

## Documentation
- In-app Help panel with quick-start, platform differences, examples, and links to [BedrockGUI-Guide.md](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/docs/BedrockGUI-Guide.md).
- Tooltips and inline hints in property editors.
- Sample templates gallery (SIMPLE, MODAL, CUSTOM, CHEST/ANVIL).

## Backward Compatibility & Migration
- Export adheres to current validator rules; tests compare with sample configs.
- configVersion embedded; migrators handle future changes, maintaining older versions.
- Action compatibility notes (e.g., curly-brace open syntax) documented and preserved.

## Build & Delivery
- Dedicated folder `designer-app` with Node-based build; no changes to plugin modules.
- Dev server for local editing; production build outputs to `designer-app/dist`.
- Optionally provide a script to copy exported YAML into plugin data directories, but no coupling required.

## Milestones
- M1: Project scaffold, core types/schemas, palette/canvas skeleton, SIMPLE form preview.
- M2: Action framework, property editors, YAML export/import.
- M3: CUSTOM form components and Java inventory designer.
- M4: Robust validation, features toggles, docs, unit/e2e tests, responsive polish.

## Success Criteria
- Drag-and-drop designer produces YAML that loads without errors in the plugin.
- Accurate real-time previews for Bedrock and Java menus.
- Extensible registries for components, actions, and features with clear interfaces.
- Comprehensive tests and in-app documentation; versioned exports with migration paths.