# BedrockGUI — Essentials Addon

**Native Bedrock Edition forms for your everyday server plugins.**

The Essentials Addon bridges [BedrockGUI](https://modrinth.com/plugin/bedrockgui) to the
plugins your players already use — warps, kits, homes, teleport requests, shops and pets — and
serves them as clean, native Bedrock forms via Floodgate. Java players keep their normal
experience; Bedrock players get a touch‑friendly UI. No menu design required: the addon builds
every form for you and auto‑detects which plugin you have installed.

> Requires **BedrockGUI** (Paper/Spigot) and **Floodgate**. This is an addon, not a standalone plugin.

---

## ✨ Features

- **Unified Hub** — one `Essentials Menu` form that shows a button for every module you enable.
- **Warps** — browse and teleport to warps as a Bedrock form, with per‑warp permission checks.
- **Kits** — claim kits from a form, with cooldown display.
- **Homes** — list, teleport to, set and delete homes; respects home limits.
- **TPA** — send `/tpa` and `/tpahere`, accept/deny/cancel pending requests, all from forms.
- **Shops** — adapt your existing GUI shop into Bedrock forms (categories → items → buy/sell),
  with amount presets and optional purchase confirmation.
- **Pets** — a full MyPet bridge: buy from a pet shop, list your pets, view stats, call / put away,
  and switch skilltrees.
- **Per‑module toggles** — everything is **off by default**; enable only what you need.
- **Actions‑only mode** — skip the built‑in forms and just register the actions so you can wire
  them into your own BedrockGUI menus.

---

## 🔌 Compatible plugins

The addon picks a provider automatically based on what's installed. Each module works with any
one of the listed plugins:

| Module | Compatible plugins |
|--------|--------------------|
| Warps | EssentialsX · CMI |
| Kits | EssentialsX · CMI |
| Homes | EssentialsX · CMI · HuskHomes |
| TPA | EssentialsX · CMI · HuskHomes |
| Shop | ShopGUI+ · EconomyShopGUI · EconomyShopGUI‑Premium |
| Pets | MyPet |

You do **not** need all of these — install the addon alongside whichever of these plugins you
already run, enable the matching module, and it just works.

---

## 📦 Installation

1. Install **[BedrockGUI](https://modrinth.com/plugin/bedrockgui)** (v2.0.8 or newer) and
   **Floodgate** on your Paper/Spigot server.
2. Install at least one compatible plugin from the table above (e.g. EssentialsX + ShopGUI+).
3. Drop **`BedrockGUI-EssentialsAddon.jar`** into your `plugins/` folder.
4. Start the server once to generate `plugins/BedrockGUI-EssentialsAddon/config.yml`.
5. Open the config, set the modules you want to `true`, and reload (see below).

---

## ⚙️ Configuration & usage

All modules start disabled. Enable the ones you want in `config.yml`:

```yaml
modules:
  warps: true
  kits: true
  homes: true
  tpa: true
  shopgui-plus: true       # ShopGUI+
  economyshop-gui: false   # EconomyShopGUI / Premium
  mypet: true
```

Then reload without restarting:

```
/essentialsaddon reload
```

| Command | Description | Permission |
|---------|-------------|------------|
| `/essentialsaddon reload` | Reload the addon configuration | `essentialsaddon.reload` |

The same file lets you customize every form title, button label, message and sound. Hub button
labels, shop amount presets (`1, 8, 16, 32, 64`), purchase confirmation, and pet form text are
all editable.

### Opening forms from BedrockGUI menus

Every feature is also exposed as a BedrockGUI **action**, so you can open it from any of your own
menus or bind it to a command. A few of the available action IDs:

| Action | Opens |
|--------|-------|
| `essentials_hub` | The unified Essentials hub |
| `essentials_warp_main` | Warps list |
| `essentials_kit_main` | Kits list |
| `home_main` | Homes menu |
| `tpa_main` | Teleport requests |
| `shopgui_main` | ShopGUI+ categories |
| `economyshop_main` | EconomyShopGUI categories |
| `essentials_pet_main` | Your pets (MyPet) |
| `essentials_pet_shop` | Pet shop |

Example BedrockGUI button action:

```yaml
open { - "essentials_hub" }
```

---

## ❓ FAQ

**Do I need every plugin listed?** No. Enable only the modules that match the plugins you run.

**Will Java players be affected?** No — they keep the native plugin GUIs. Forms are served to
Bedrock (Floodgate) players.

**Can I use my own menus instead of the built‑in forms?** Yes. Turn on `actions-only` for a
module and call the actions from your own BedrockGUI YAML.

---

*Part of the BedrockGUI project — [Docs](https://pintux.gitbook.io/pintux-support/bedrockgui/bedrockgui-v2) · [Discord](https://discord.com/invite/FD2MTETnyQ) · [Web Designer](https://designer.pintux.org/)*
