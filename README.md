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
- **Dynamic Forms**: Runtime-conditional components based on permissions or player state.
- **Java Menus**: Early support for traditional chest-based GUIs.

### ⚡ Unified Action System
Execute complex logic using a modern curly-brace syntax. Chain actions together to create guided player flows.
- **Commands**: Run commands as the player or the console.
- **Economy**: Integrate with your economy plugin for balance checks and transactions.
- **UI Feedback**: Send titles, action bars, and colorized messages.
- **Inventory**: Manage player items (give/remove/check).
- **Flow Control**: Use `delay`, `random`, and `conditional` actions to build intelligent menus.
- **BungeeCord**: Send players to different servers via plugin messaging.
- **URLs**: Send clickable links directly to chat.

### 🔌 Integrations
- **Floodgate**: Native support for Bedrock player detection.
- **PlaceholderAPI**: Use any PAPI placeholder in your titles, button text, or action values.
- **ShopGUI+ & EconomyShopGUI**: Dedicated shop bridge actions (see `essentials-addon` module).
- **YAML Configuration**: No need to recompile; edit your menus in real-time.

### 🧩 Multi-Platform Architecture
| Module | Target | Output |
|--------|--------|--------|
| `common` | Shared API & core logic | `BedrockGUI-API.jar` |
| `paper` | Paper / Spigot / Folia | `BedrockGUI-Paper.jar` |
| `velocity` | Velocity proxy | `BedrockGUI-Velocity.jar` |
| `bungeecord` | BungeeCord / Waterfall | `BedrockGUI-Bungee.jar` |
| `essentials-addon` | Paper shop addons | `BedrockGUI-EssentialsAddon.jar` |

---

## ⌨️ Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/bedrockgui` | Administrative control | `bedrockgui.admin` |
| `/bedrockgui reload` | Reloads all configurations | `bedrockgui.admin` |
| `/bedrockgui open <menu> [player]` | Opens a specific form for a player | `bedrockgui.admin` |

---

## 🛠️ Building from Source

### Prerequisites
- **Java 21** or higher
- **Gradle** (or use the included `gradlew` wrapper)

### Build Commands

```bash
# Build everything
./gradlew build

# Build a specific platform
./gradlew buildPaper
./gradlew buildVelocity
./gradlew buildBungeecord
./gradlew buildEssentialsAddon
```

Built JARs are automatically placed in your server directories:
- **BungeeCord** → `~/Desktop/Server/Java-Bedrock-Server/`
- **Paper** → `~/Desktop/Server/Java-Bedrock-Server/lobby/plugins/`
- **Velocity** → `~/Desktop/Server/Java-Bedrock-Server/proxy/plugins/`
- **EssentialsAddon** → `~/Desktop/Server/Java-Bedrock-Server/lobby/plugins/`

### Updating Dependencies
All versions are centralized in [`gradle.properties`](gradle.properties). To update any dependency, change the version in that single file — no need to edit individual module builds.

---

## 🛠️ Tools

- **Web Designer**: Use our [visual editor](https://designer.pintux.org/) to build your forms without touching a text editor.
- **Wiki**: For detailed configuration and action examples, visit the [BedrockGUI Guide](https://pintux.gitbook.io/pintux-support/bedrockgui/bedrockgui-v2).

---

## 📚 Action Quick Reference

| Action | Description | Example |
|--------|-------------|---------|
| `command` | Run as player | `command { - "me hello" }` |
| `server` | Run as console | `server { - "say Hello!" }` |
| `message` | Colorized chat | `message { - "&aWelcome!" }` |
| `title` | Title/subtitle | `title { - "Welcome!:Enjoy:10:60:10" }` |
| `actionbar` | Hotbar message | `actionbar { - "&eLoading..." }` |
| `sound` | Play sound | `sound { - "ui.button.click:0.8:1.0" }` |
| `economy` | Balance ops | `economy { - "check:500" - "remove:500" }` |
| `inventory` | Item ops | `inventory { - "give:diamond:3" }` |
| `open` | Open another menu | `open { - "shop_menu" }` |
| `delay` | Wait + chain | `delay { - "1000" - "message:Done!" }` |
| `random` | Weighted pick | `random { - "message:A:2" - "message:B:1" }` |
| `conditional` | If/else logic | See [docs](docs/BedrockGUI-Guide.md) |
| `broadcast` | Global message | `broadcast { - "Welcome!" }` |
| `url` | Clickable link | `url { - "https://example.com" }` |

---

## 📂 Project Structure

```
BedrockGUI/
├── common/              # Shared API, form builders, action system
├── paper/               # Paper/Spigot/Folia platform
├── velocity/            # Velocity proxy platform
├── bungeecord/          # BungeeCord/Waterfall platform
├── essentials-addon/    # ShopGUI+ & EconomyShopGUI bridges
├── docs/                # API reference and user guide
├── gradle.properties    # Centralized dependency versions
└── build.gradle         # Root build config
```

---

## 📄 License

This project is proprietary. All rights reserved.
