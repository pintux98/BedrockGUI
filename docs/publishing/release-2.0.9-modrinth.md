# BedrockGUI v2.0.9

This release adds a third official addon — **Homestead** — and a new *actions-only* mode across
all addons.

## 🆕 New: Homestead Addon

Native Bedrock forms for the **Homestead** land-claiming plugin — all **27** of its GUIs, built for
you and served automatically to Bedrock players:

- **Regions** — list, per-region hub, info, top-regions leaderboard, welcome-sign teleports.
- **Players** — members, invites, bans; invite / ban / kick / unban / revoke from forms.
- **Flags** — world, global, and per-member player & control flags as toggle sheets.
- **Sub-areas** — manage, rename, delete, members and flags.
- **Chunks** — claimed-chunk list (teleport / unclaim) plus map colour & icon pickers.
- **Progression & settings** — levels, rewards, logs, rating, region settings and a weather/time
  cycler.

Bedrock players who run `/region` (or `/homesteadadmin`) get the form automatically; Java players
keep the native GUIs. Requires **Homestead** installed.

## 🔀 New: actions-only mode for every addon

All three addons now have **`integrated-gui`** and **`register-actions`** toggles. Set
`integrated-gui: false` to stop the built-in forms and command interception while still registering
the addon's actions — so you can build your own BedrockGUI menus and drive the plugin through them.

## 📦 Distribution

Addons are distributed with the main plugin — grab the JARs from the release:

- `BedrockGUI-HomesteadAddon.jar` *(new)*
- `BedrockGUI-EssentialsAddon.jar`
- `BedrockGUI-BedwarsAddon.jar`

---

**Installation:** drop the addon JAR(s) into `plugins/` alongside BedrockGUI v2.0.9+ and Floodgate.
The Homestead addon also needs the **Homestead** plugin. See each addon's page for setup details.

*Requires Paper/Spigot 1.20.1+ and Java 21.*
