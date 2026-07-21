# BedrockGUI v2.0.8

This release introduces **addon support** — two official addons that bring native Bedrock Edition
forms to popular server plugins, with zero menu design required.

## 🆕 New: Essentials Addon

Bridges BedrockGUI to your everyday plugins and serves native Bedrock forms for:

- **Warps, Kits, Homes, TPA** — works with EssentialsX, CMI, or HuskHomes (auto‑detected).
- **Shops** — adapts ShopGUI+ and EconomyShopGUI (incl. Premium) into Bedrock forms.
- **Pets** — full MyPet bridge: pet shop, pet list, stats, call / put away, and skilltrees.
- **Unified Hub** — one menu with a button per enabled module.

Every module is off by default — enable only what you need, then `/essentialsaddon reload`.

## 🆕 New: Bedwars Addon

Turns BedWars chest GUIs into native Bedrock forms, auto‑detecting your BedWars plugin:

- **Shop, Team Upgrades, Arena selector, Stats, Spectator, Party**
- Supports **BedWars2023** and **BedWars1058** (all modules), and **ScreamingBedWars**
  (arena, stats, spectator).
- Bedrock players get forms automatically; Java players keep the native GUI.

## 📦 Distribution

Release notes and addon pages are now published in both **Spigot BBCode** and **Modrinth
Markdown** formats.

---

**Installation:** drop the addon JAR(s) into `plugins/` alongside BedrockGUI v2.0.8+ and
Floodgate. See each addon's page for setup details.

- `BedrockGUI-EssentialsAddon.jar`
- `BedrockGUI-BedwarsAddon.jar`

*Requires Paper/Spigot 1.20.1+ and Java 21.*
