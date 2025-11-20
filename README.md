# BedrockGUI Plugin for Spigot, Paper, Bungeecord, and their Forks!

[![Spigot](https://img.shields.io/badge/Spigot-Plugin-ff6600)](https://www.spigotmc.org/resources/bedrockgui.119592/)
[![Discord](https://img.shields.io/discord/1033791462313304234?label=Support%20Server&logo=discord&logoColor=white)](https://discord.gg/FD2MTETnyQ)
[![Wiki](https://img.shields.io/badge/Wiki-Documentation-0066ff)](https://pintux.gitbook.io/pintux-support/bedrockgui-v2-beta)

**BedrockGUI** is a simple yet powerful plugin that allows server owners to create custom GUI menus for Bedrock players on Java servers via [Geyser](https://geysermc.org/). Using the Floodgate API, this plugin offers seamless interaction for Bedrock players through custom forms and menus.  

![Example Screenshot](https://i.imgur.com/zdH3D5E.png)

---

## Features
- **Custom GUIs for Bedrock players**: Design interactive menus for players using Simple, Modal, or Custom forms.
- **Support for SimpleForm, ModalForm, and CustomForm**: Build menus for choices, confirmations, or advanced inputs.
- **Seamless Geyser and Floodgate Integration**: Automatically handle Bedrock player interactions.
- **YAML-Based Configuration**: Easily define menus, buttons, and actions without coding.
- **Developer API**: Extend or create menus programmatically for advanced use cases.
- **PlaceholderAPI Support**: Add dynamic placeholders to buttons and forms (optional).

---

## Requirements
- **Minecraft Server**: Paper, Spigot, or compatible forks.
- **Proxy Support**: Bungeecord or its forks (optional).
- [Floodgate](https://github.com/GeyserMC/Floodgate): Required for Bedrock player detection.
- (Optional) [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) for dynamic placeholders.
- **Java 17+**: Required for the plugin to function.

---

## Installation

1. Download the plugin JAR from the [Spigot page](https://www.spigotmc.org/resources/bedrockgui.119592/) or build it from source (instructions below).
2. Place the JAR file in your server's `plugins` folder.
3. Restart your server to generate the default `config.yml` file.
4. Edit the `config.yml` file to define your menus.
5. Run `/bgui reload` (or `/bguiproxy reload` on BungeeCord) to apply changes without restarting the server.

---

## Compiling from Source

If you'd like to build your own version of BedrockGUI, follow these steps:

### Prerequisites
- Java Development Kit (JDK) 17 or later.
- [Gradle](https://gradle.org/) build tool.

### Steps to Build

1. Clone the repository:
    ```bash
    git clone https://github.com/your-username/your-repo.git
    cd your-repo
    ```

2. Build the plugin using Gradle's `shadowJar` task:
    ```bash
    ./gradlew shadowJar
    ```

3. Copy the generated JAR file from the `build/libs/` directory into your server's `plugins` folder.

### Customizing
Modify the source code to add features or change functionality, then re-run the `shadowJar` task to generate an updated JAR.

---

## Commands
| Command                       | Description                               |
|-------------------------------|-------------------------------------------|
| `/bgui reload`                | Reload all menus from the configuration. |
| `/bgui open <menu_name>`      | Open a specific menu for yourself.        |
| `/bgui openfor <player> <menu_name>` | Open a menu for another player.   |

---

## Support

For help or questions, join our [Discord Server](https://discord.gg/FD2MTETnyQ) or check out the [Wiki Documentation](https://pintux.gitbook.io/pintux-support).

---

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).
