# BedrockGUI Shop Addon (ShopGUI+ + EconomyShopGUI)

This addon provides Bedrock Edition compatible shop UIs by translating shop plugin GUIs into BedrockGUI forms.

## Install

- Build the addon jar and put it in your Paper server `plugins/` folder.
- Also install:
  - `BedrockGUI-Paper`
  - `floodgate` (required for Bedrock detection)
  - One or both:
    - `ShopGUIPlus`
    - `EconomyShopGUI` or `EconomyShopGUI-Premium`

## Exported Example Forms (New Per-File Format)

On first run, the addon exports example forms into its data folder:

- `plugins/BedrockGUI-ShopAddons/forms/shopguiplus_bedrock_hub.yml`
- `plugins/BedrockGUI-ShopAddons/forms/shopguiplus_bedrock_shortcuts.yml`
- `plugins/BedrockGUI-ShopAddons/forms/economyshopgui_bedrock_hub.yml`
- `plugins/BedrockGUI-ShopAddons/forms/economyshopgui_bedrock_shortcuts.yml`

Edit these files and replace the sample shop/section ids (like `blocks`, `farming`, `tools`) with your real ids.

## Action Snippets

BedrockGUI supports both action-string format (`action:type:value`) and the modern block format:

### ShopGUI+ Actions

- Open main menu:

```yaml
onClick:
  - |
    shopgui_main {
      - "main"
    }
```

- Open a category page:

```yaml
onClick:
  - |
    shopgui_shop {
      - "blocks|1"
    }
```

- Open an item menu:

```yaml
onClick:
  - |
    shopgui_item {
      - "blocks|stone|1"
    }
```

- Execute transaction (BUY/SELL/TRADE):

```yaml
onClick:
  - |
    shopgui_transaction {
      - "BUY|blocks|stone|16|1"
    }
```

### EconomyShopGUI Actions

- Open main menu:

```yaml
onClick:
  - |
    economyshop_main {
      - "main"
    }
```

- Open a section page:

```yaml
onClick:
  - |
    economyshop_shop {
      - "blocks|1"
    }
```

- Open an item menu:

```yaml
onClick:
  - |
    economyshop_item {
      - "blocks|pages.page1.items.item1|1"
    }
```

- Execute transaction (BUY/SELL):

```yaml
onClick:
  - |
    economyshop_transaction {
      - "BUY|blocks|pages.page1.items.item1|16|1"
    }
```

## Pagination

Both backends support paging:

- When you open a shop/section, the addon shows items for that page.
- If other accessible pages exist, it automatically adds `Previous` / `Next` buttons.

## Extending To More Shop Plugins

To add another shop plugin backend, implement `ShopBackend` and register it in the router:

- Interface: `it.pintux.life.shopguiaddon.backend.ShopBackend`
- Router: `it.pintux.life.shopguiaddon.backend.ShopBackendRouter`
- Registration: `BedrockShopGuiAddonPlugin` creates the router with a list of backends.

Recommended pattern:

1. Create `YourShopBackend` that implements `ShopBackend`:
   - handle `/shop` interception (or plugin-specific commands)
   - detect plugin GUI opens in `handleInventoryOpen`
2. Create a `BedrockYourShopService` that does the rendering and transaction handling.
3. Register custom BedrockGUI actions (`yourshop_main`, `yourshop_shop`, `yourshop_item`, `yourshop_transaction`) in `onEnable`.

