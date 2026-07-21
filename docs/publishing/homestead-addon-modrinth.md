# BedrockGUI — Homestead Addon

**Native Bedrock Edition forms for the Homestead land-claiming plugin.**

The Homestead Addon bridges [BedrockGUI](https://modrinth.com/plugin/bedrockgui) to
[Homestead](https://www.spigotmc.org/resources/121873/) and serves every one of its land-claiming
GUIs as clean, native Bedrock forms via Floodgate. Java players keep the normal chest GUIs; Bedrock
players get a touch-friendly UI. No menu design required — the addon builds all **27** screens for
you and drives Homestead through its API.

> Requires **BedrockGUI 2.0.8+** (Paper/Spigot), **Floodgate**, and **Homestead**. This is an addon,
> not a standalone plugin.

---

## ✨ What it covers

Every Homestead GUI, reshaped for Bedrock:

- **Regions** — your regions list, per-region hub, region info, top-regions leaderboard, and
  welcome-sign teleports.
- **Players** — members list, per-member info, pending invites and bans; invite / ban / kick /
  unban / revoke from forms.
- **Flags** — world flags, global player flags, and per-member player & control flags as toggle
  sheets (change several at once, applied on submit).
- **Sub-areas** — list, manage, rename, delete, end-rent; sub-area members and sub-area flags.
- **Chunks** — claimed-chunk list with teleport / unclaim, plus map colour and map icon pickers.
- **Progression** — region levels & XP, rewards, logs (with mark-read / clear), and region rating.
- **Settings** — rename, display name, description, set spawn, transfer ownership, delete, and a
  weather / time cycler.

Bedrock limits are handled for you: item-grid pickers become dropdowns, chunk grids become lists,
left/right-click actions become buttons, and confirmations use modal forms.

---

## 🔌 Compatible plugin

| Feature set | Requires |
|-------------|----------|
| All region / claim GUIs | **Homestead** (SpigotMC 121873) |

---

## 📦 Installation

1. Install **[BedrockGUI](https://modrinth.com/plugin/bedrockgui)** (v2.0.8 or newer) and
   **Floodgate** on your Paper/Spigot server.
2. Install **Homestead**.
3. Drop **`BedrockGUI-HomesteadAddon.jar`** into your `plugins/` folder.
4. Start the server once to generate `plugins/BedrockGUI-HomesteadAddon/config.yml`.
5. Join as a Bedrock player and run `/region` — you get the Bedrock form automatically.

---

## ⚙️ Configuration & usage

Bedrock players who run Homestead's GUI commands are redirected to forms automatically:

- `/region` (and aliases `rg`, `hs`, `homestead`) → your regions
- `/homesteadadmin` (alias `hsadmin`) → all regions (operators)

Admin / testing command:

| Command | Description | Permission |
|---------|-------------|------------|
| `/homesteadaddon reload` | Reload the addon configuration | `homesteadaddon.reload` |
| `/homesteadaddon open <menu> [regionId]` | Open a form for yourself | `homesteadaddon.admin` |
| `/homesteadaddon openfor <player> <menu> [regionId]` | Open a form for another player | `homesteadaddon.admin` |

Alias: `/hsaddon`. Every form title, button label, message and the page size are editable in
`config.yml`.

### Actions-only mode

Prefer to build your own menus? Set `integrated-gui: false` (keep `register-actions: true`) and the
addon stops intercepting commands and serving its built-in forms, but still registers all `hs_*`
actions so you can wire Homestead into your own BedrockGUI menus.

```yaml
integrated-gui: false
register-actions: true
```

### Opening forms from BedrockGUI menus

Every screen is exposed as a BedrockGUI **action**. A few of the available IDs:

| Action | Opens |
|--------|-------|
| `hs_regions` | Your regions list |
| `hs_region_menu:<regionId>` | A region's management hub |
| `hs_players:<regionId>` | Players management |
| `hs_flags:<regionId>` | Flags chooser (world / global) |
| `hs_subareas:<regionId>` | Sub-areas list |
| `hs_chunks:<regionId>` | Claimed chunks |
| `hs_levels:<regionId>` | Region levels |
| `hs_logs:<regionId>` | Region logs |
| `hs_misc:<regionId>` | Miscellaneous settings |
| `hs_top` | Top regions leaderboard |
| `hs_welcome` | Regions with welcome signs |

Example BedrockGUI button action:

```yaml
open { - "hs_regions" }
```

---

## ❓ FAQ

**Do I need Homestead installed?** Yes — the addon drives Homestead's API. Without it, forms report
"Homestead unavailable".

**Will Java players be affected?** No — they keep the native Homestead GUIs. Forms are served to
Bedrock (Floodgate) players only.

**Can I use my own menus instead of the built-in forms?** Yes. Set `integrated-gui: false` and call
the `hs_*` actions from your own BedrockGUI YAML.

**Permissions?** Management respects Homestead's own ownership and control-flag checks, so a member
can only do what Homestead allows.

---

*Part of the BedrockGUI project — [Docs](https://pintux.gitbook.io/pintux-support/bedrockgui/bedrockgui-v2) · [Discord](https://discord.com/invite/FD2MTETnyQ) · [Web Designer](https://designer.pintux.org/)*
