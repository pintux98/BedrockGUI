# BedrockGUI — Bedwars Addon

**Native Bedrock Edition forms for your BedWars server.**

The Bedwars Addon bridges [BedrockGUI](https://modrinth.com/plugin/bedrockgui) to popular BedWars
plugins and turns their chest GUIs into clean, native Bedrock forms via Floodgate. When a Bedrock
player opens the shop, team upgrades, arena selector, stats, teleporter or party menu, they get a
touch‑friendly form instead of a cramped chest UI. Java players keep the original GUI untouched.

The addon **auto‑detects** the BedWars plugin you have installed — no manual setup of which engine
you use.

> Requires **BedrockGUI** (Paper/Spigot) and **Floodgate**. This is an addon, not a standalone plugin.

---

## ✨ Features

- **Shop** — categories and items as a Bedrock form, with affordability marking and buy feedback.
- **Team Upgrades** — purchase team upgrades and traps; maxed and unaffordable states shown.
- **Arena selector** — browse arenas with live state and player counts, then join with one tap.
- **Stats** — view your wins, losses, kills, final kills, deaths, beds broken, K/D and W/L.
- **Spectator / Teleporter** — pick a player to teleport to from a form.
- **Party** — view members, add players, leave, disband, and kick from a Bedrock form.
- **Native GUI interception** — opening the in‑game shop/teleporter as a Bedrock player swaps the
  chest for a form automatically.
- **Per‑module toggles** — turn any module on or off in the config.

---

## 🔌 Compatible plugins

The addon auto‑detects and adapts to whichever of these is installed:

| BedWars plugin | Supported modules |
|----------------|-------------------|
| **BedWars2023** | Shop · Upgrades · Arena · Stats · Spectator · Party (all) |
| **BedWars1058** | Shop · Upgrades · Arena · Stats · Spectator · Party (all) |
| **ScreamingBedWars (SBW)** | Arena · Stats · Spectator |

> For ScreamingBedWars, the shop, upgrades and party menus continue to use the plugin's own
> native handling; the addon provides Bedrock forms for arena, stats and spectator.

---

## 📦 Installation

1. Install **[BedrockGUI](https://modrinth.com/plugin/bedrockgui)** (v2.0.8 or newer) and
   **Floodgate** on your Paper/Spigot server.
2. Install your BedWars plugin (BedWars2023, BedWars1058, or ScreamingBedWars).
3. Drop **`BedrockGUI-BedwarsAddon.jar`** into your `plugins/` folder.
4. Start the server once to generate `plugins/BedrockGUI-BedwarsAddon/config.yml`.
5. Adjust the config if needed and reload (see below).

No extra wiring is required — Bedrock players automatically get forms in place of the native
BedWars chest GUIs.

---

## ⚙️ Configuration & usage

Every module is enabled by default. Toggle any of them in `config.yml`:

```yaml
modules:
  shop: true
  upgrades: true
  arena: true
  stats: true
  spectator: true
  party: true
```

You can also customize every title, button label and message, and tune the `gui-title-contains`
strings the addon uses to recognize the native stats and teleporter chests.

| Command | Description |
|---------|-------------|
| `/bedwarsaddon reload` | Reload the addon configuration |
| `/bedwarsaddon party` | Open the party form |
| `/bedwarsaddon arena` | Open the arena selector |
| `/bedwarsaddon stats` | Open your stats |
| `/bedwarsaddon spectator` | Open the teleporter |

Alias: `/bwaddon`.

### Opening forms from BedrockGUI menus

Each module is also a BedrockGUI **action**, so you can open it from your own menus or bind it to
a command:

| Action | Opens |
|--------|-------|
| `bw_shop_main` | Shop categories |
| `bw_upgrade_main` | Team upgrades |
| `bw_arena_main` | Arena selector |
| `bw_stats` | Player stats |
| `bw_spec_main` | Spectator / teleporter |
| `bw_party_main` | Party menu |

Example BedrockGUI button action:

```yaml
open { - "bw_arena_main" }
```

---

## ❓ FAQ

**Do I need to redesign my BedWars setup?** No. The addon reads your existing BedWars plugin and
generates the forms automatically.

**Will Java players see the forms?** No — Java players keep the native chest GUIs. Forms are
served to Bedrock (Floodgate) players only.

**Which BedWars plugin should I use?** Any of the three listed. BedWars2023 and BedWars1058 get
the full module set; ScreamingBedWars gets arena, stats and spectator.

---

*Part of the BedrockGUI project — [Docs](https://pintux.gitbook.io/pintux-support/bedrockgui/bedrockgui-v2) · [Discord](https://discord.com/invite/FD2MTETnyQ) · [Web Designer](https://designer.pintux.org/)*
