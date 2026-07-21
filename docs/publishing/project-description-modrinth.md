![BedrockGUI Banner](https://cdn.modrinth.com/data/cached_images/255ef9cc052bffa506f3e9c4d65692b25bc5ad58.jpeg)

# BedrockGUI v2
**Bring native Bedrock Edition forms to your Java server!**
BedrockGUI is a powerful utility for **Paper**, **Velocity** and **Bungeecord** that allows server owners to create native Bedrock-style interactive forms. By integrating with Floodgate, it provides a seamless UI experience for Bedrock players while remaining manageable via simple YAML configurations.

[🚀 Web Designer](https://designer.pintux.org/) | [📖 Documentation](https://pintux.gitbook.io/pintux-support/bedrockgui/bedrockgui-v2) | [💬 Discord Support](https://discord.com/invite/FD2MTETnyQ)
---
## ✨ Key Features
### 🖼️ Versatile Form Types
- **Simple Forms**: Button-based menus. Support for custom images via URLs, textures, or player names.
- **Modal Forms**: Simple Yes/No confirmation dialogs.
- **Custom Forms**: Complex data entry with Text Inputs, Sliders, Dropdowns, and Toggles.
- **Java Menus**: Early support for traditional chest-based GUIs.
### ⚡ Unified Action System
Execute complex logic using a modern curly-brace syntax. Chain actions together to create guided player flows.
- **Commands**: Run commands as the player or the console.
- **Economy**: Integrate with your economy plugin for balance checks and transactions.
- **UI Feedback**: Send titles, action bars, and colorized messages.
- **Inventory**: Manage player items (give/remove/check).
- **Flow Control**: Use `delay`, `random`, and `conditional` actions to build intelligent menus.
- **BungeeCord**: Send players to different servers via plugin messaging.
### 🔌 Integrations
- **Floodgate**: Native support for Bedrock player detection.
- **PlaceholderAPI**: Use any PAPI placeholder in your titles, button text, or action values.
- **YAML Configuration**: No need to recompile; edit your menus in real-time.
---
## 🧩 Official Addons
Optional companion plugins that turn the GUIs of popular server plugins into native Bedrock forms — no menu design required. Drop them in alongside BedrockGUI and Floodgate.

### 🛒 Essentials Addon
Native Bedrock forms for **Warps, Kits, Homes, TPA, Shops and Pets**, auto-detecting the plugin you run:
- Warps / Kits / Homes / TPA — **EssentialsX**, **CMI**, or **HuskHomes**
- Shops — **ShopGUI+** and **EconomyShopGUI** (incl. Premium)
- Pets — **MyPet** (buy, list, stats, call, skilltrees)

📥 **[Download on GitHub](https://github.com/pintux98/BedrockGUI/releases/latest)** — `BedrockGUI-EssentialsAddon.jar`

### ⚔️ Bedwars Addon
Native Bedrock forms for **Shop, Team Upgrades, Arena selector, Stats, Spectator and Party**, auto-detecting your BedWars plugin:
- **BedWars2023** and **BedWars1058** — all modules
- **ScreamingBedWars** — arena, stats, spectator

📥 **[Download on GitHub](https://github.com/pintux98/BedrockGUI/releases/latest)** — `BedrockGUI-BedwarsAddon.jar`

> Both addons require BedrockGUI (Paper/Spigot) + Floodgate. See the [Wiki](https://pintux.gitbook.io/pintux-support/bedrockgui/bedrockgui-v2) for setup.
---
## ⌨️ Commands
- `/bedrockgui` - Administrative control (Permission: `bedrockgui.admin`)
- `/bedrockgui reload` - Reloads all configurations.
- `/bedrockgui open <menu> [player]` - Opens a specific form for a player.
---
## 🛠️ Tools
- **Web Designer**: Use our [visual editor](https://designer.pintux.org/) to build your forms without touching a text editor.
- **Wiki**: For detailed configuration and action examples, visit the [BedrockGUI Guide](https://pintux.gitbook.io/pintux-support/bedrockgui/bedrockgui-v2).
