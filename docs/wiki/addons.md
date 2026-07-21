---
description: Native Bedrock forms for Essentials-style plugins, BedWars and Homestead.
---

# Addons

BedrockGUI ships three optional **addons** that turn the GUIs of popular server plugins into native
Bedrock Edition forms — automatically. You don't design any menus: each addon detects the plugin
you already run and builds the forms for you. Java players keep the original interface; Bedrock
players (via Floodgate) get a touch-friendly form.

{% hint style="info" %}
All addons are **Paper/Spigot** plugins and require **BedrockGUI 2.0.8+** (the Homestead addon
needs **2.0.9+**) and **Floodgate** installed on the same server. They are downloaded separately
from the main plugin.
{% endhint %}

{% hint style="success" %}
**Actions-only mode.** Every addon has an `integrated-gui` and a `register-actions` toggle. Set
`integrated-gui: false` (keep `register-actions: true`) to stop the built-in forms and command
interception while still registering the addon's actions — so you can build your own BedrockGUI
menus and drive the plugin through them.
{% endhint %}

**Downloads:** [GitHub Releases](https://github.com/pintux98/BedrockGUI/releases/latest)

* `BedrockGUI-EssentialsAddon.jar`
* `BedrockGUI-BedwarsAddon.jar`
* `BedrockGUI-HomesteadAddon.jar`

---

## 🛒 Essentials Addon

Serves native Bedrock forms for everyday server features and auto-detects which plugin provides
each one.

### Compatible plugins

| Module | Compatible plugins |
| ------ | ------------------ |
| Warps  | EssentialsX · CMI |
| Kits   | EssentialsX · CMI |
| Homes  | EssentialsX · CMI · HuskHomes |
| TPA    | EssentialsX · CMI · HuskHomes |
| Shop   | ShopGUI+ · EconomyShopGUI · EconomyShopGUI-Premium |
| Pets   | MyPet |

You only need the plugins for the modules you actually use.

### Install

1. Install **BedrockGUI** (2.0.8+) and **Floodgate**.
2. Install at least one compatible plugin from the table above.
3. Drop `BedrockGUI-EssentialsAddon.jar` into `plugins/`.
4. Start the server once to generate `plugins/BedrockGUI-EssentialsAddon/config.yml`.
5. Enable the modules you want, then reload.

### Enable modules

All modules are **off by default**. Set the ones you want to `true`:

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

Reload without a restart:

```
/essentialsaddon reload
```

| Command | Permission |
| ------- | ---------- |
| `/essentialsaddon reload` | `essentialsaddon.reload` |

{% hint style="success" %}
Prefer your own menus? Turn on `actions-only` for a module and skip the built-in forms — the
addon still registers its actions so you can call them from your BedrockGUI YAML.
{% endhint %}

### Actions

Open any feature from your own menus with these action IDs:

| Action | Opens |
| ------ | ----- |
| `essentials_hub` | Unified Essentials hub |
| `essentials_warp_main` | Warps list |
| `essentials_kit_main` | Kits list |
| `home_main` | Homes menu |
| `tpa_main` | Teleport requests |
| `shopgui_main` | ShopGUI+ categories |
| `economyshop_main` | EconomyShopGUI categories |
| `essentials_pet_main` | Your pets (MyPet) |
| `essentials_pet_shop` | Pet shop |

```yaml
open { - "essentials_hub" }
```

---

## ⚔️ Bedwars Addon

Turns BedWars chest GUIs into native Bedrock forms. It auto-detects your BedWars plugin and
intercepts the native menus for Bedrock players.

### Compatible plugins

| BedWars plugin | Supported modules |
| -------------- | ----------------- |
| **BedWars2023** | Shop · Upgrades · Arena · Stats · Spectator · Party |
| **BedWars1058** | Shop · Upgrades · Arena · Stats · Spectator · Party |
| **ScreamingBedWars (SBW)** | Arena · Stats · Spectator |

{% hint style="info" %}
On ScreamingBedWars, the shop, upgrades and party menus keep using the plugin's own native
handling; the addon provides Bedrock forms for arena, stats and spectator.
{% endhint %}

### Install

1. Install **BedrockGUI** (2.0.8+) and **Floodgate**.
2. Install your BedWars plugin (BedWars2023, BedWars1058, or ScreamingBedWars).
3. Drop `BedrockGUI-BedwarsAddon.jar` into `plugins/`.
4. Start the server once to generate `plugins/BedrockGUI-BedwarsAddon/config.yml`.

Bedrock players automatically get forms in place of the native chest GUIs — no extra wiring.

### Enable modules

All modules are enabled by default. Toggle them in `config.yml`:

```yaml
modules:
  shop: true
  upgrades: true
  arena: true
  stats: true
  spectator: true
  party: true
```

### Commands

| Command | Description |
| ------- | ----------- |
| `/bedwarsaddon reload` | Reload the configuration |
| `/bedwarsaddon party` | Open the party form |
| `/bedwarsaddon arena` | Open the arena selector |
| `/bedwarsaddon stats` | Open your stats |
| `/bedwarsaddon spectator` | Open the teleporter |

Alias: `/bwaddon`.

### Actions

| Action | Opens |
| ------ | ----- |
| `bw_shop_main` | Shop categories |
| `bw_upgrade_main` | Team upgrades |
| `bw_arena_main` | Arena selector |
| `bw_stats` | Player stats |
| `bw_spec_main` | Spectator / teleporter |
| `bw_party_main` | Party menu |

```yaml
open { - "bw_arena_main" }
```

---

## 🏡 Homestead Addon

Serves native Bedrock forms for every GUI of the **Homestead** land-claiming plugin (all 27
screens). Bedrock players who run `/region` get the form; Java players keep the native GUIs.

### Compatible plugin

| Feature set | Requires |
| ----------- | -------- |
| All region / claim GUIs | **Homestead** (SpigotMC 121873) |

### Install

1. Install **BedrockGUI** (2.0.9+) and **Floodgate**.
2. Install **Homestead**.
3. Drop `BedrockGUI-HomesteadAddon.jar` into `plugins/`.
4. Start the server once to generate `plugins/BedrockGUI-HomesteadAddon/config.yml`.

Bedrock players who run `/region` (or `/homesteadadmin`) automatically get forms in place of the
native chest GUIs — no extra wiring.

### What's covered

Regions (list, hub, info, top regions, welcome signs) · Players (members, invites, bans, kick) ·
Flags (world, global, member, control) · Sub-areas (manage, members, flags) · Claimed chunks ·
Map colour & icon · Levels · Rewards · Logs · Rating · Region settings · Weather / time.

Management respects Homestead's own ownership and control-flag checks, so members can only do what
Homestead allows.

### Commands

Bedrock players are redirected automatically; the addon also has an admin command:

| Command | Description | Permission |
| ------- | ----------- | ---------- |
| `/homesteadaddon reload` | Reload the configuration | `homesteadaddon.reload` |
| `/homesteadaddon open <menu> [regionId]` | Open a form for yourself | `homesteadaddon.admin` |
| `/homesteadaddon openfor <player> <menu> [regionId]` | Open a form for another player | `homesteadaddon.admin` |

Alias: `/hsaddon`.

### Actions

Open any screen from your own menus with these action IDs (region-scoped ones take the region id):

| Action | Opens |
| ------ | ----- |
| `hs_regions` | Your regions list |
| `hs_region_menu:<id>` | A region's management hub |
| `hs_players:<id>` | Players management |
| `hs_flags:<id>` | Flags chooser (world / global) |
| `hs_subareas:<id>` | Sub-areas list |
| `hs_chunks:<id>` | Claimed chunks |
| `hs_levels:<id>` | Region levels |
| `hs_logs:<id>` | Region logs |
| `hs_misc:<id>` | Miscellaneous settings |
| `hs_top` | Top regions leaderboard |
| `hs_welcome` | Regions with welcome signs |

```yaml
open { - "hs_regions" }
```

{% hint style="info" %}
Homestead must be installed — without it, the forms report "Homestead unavailable".
{% endhint %}

---

## Troubleshooting

* **Forms don't appear for Bedrock players** — confirm Floodgate is installed and the player
  joined through it.
* **"Provider unavailable" messages** — the matching plugin isn't installed, or the module is
  disabled in the addon config.
* **Java players still see the chest GUI** — that's expected; addons only replace GUIs for Bedrock
  players.

Need help? Join the [Discord](https://discord.com/invite/FD2MTETnyQ).
