# BedrockGUI Plugin for spigot / paper forks and Bungeecord and his forks!

[Spigot page](https://www.spigotmc.org/resources/bedrockgui.119592/)
[Discord](https://discord.gg/FD2MTETnyQ)

This is a simple Minecraft plugin that allows servers to create custom GUI menus for Bedrock players who play on Java servers through [Geyser](https://geysermc.org/). This plugin leverages the Floodgate API to build GUIs that are accessible to Bedrock players, giving server admins the flexibility to create forms and menus for Bedrock players.

![Example](https://i.imgur.com/zdH3D5E.png)

## Features
- Create custom GUIs for Bedrock players.
- Support for both `SimpleForm`, `ModalForm` and `CustomForm`@
- Easy integration with Geyser and Floodgate to manage Bedrock-specific forms.
- YAML-based configuration for defining menus and actions.

## Requirements
- Minecraft Server (Paper, Spigot, etc.)
- Minecraft Proxy (Bungeecord and forks)
- [Floodgate](https://github.com/GeyserMC/Floodgate)
- (Optional) [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) for placeholders
- Java 17+ (for server-side plugin)

## Installation

1. Download the plugin JAR from the releases page or compile it yourself (see below).
2. Place the plugin JAR in your server's `plugins` folder.
3. Restart your server.
4. Configure the menus in the `config.yml` file.
5. Run `/bgui (or /bguiproxy on bungee) reload` to reload menus after making configuration changes.

## Compiling from Source

If you want to clone and compile a custom version of this plugin, follow the steps below.

### Prerequisites
- Java Development Kit (JDK) 8 or later
- Gradle build tool

### Steps to Build

1. Clone the repository:

    ```bash
    git clone https://github.com/your-username/your-repo.git
    cd your-repo
    ```

2. Compile the plugin with Gradle using the `shadowJar` task:

    ```bash
    ./gradlew shadowJar
    ```

3. Copy the generated JAR file into your server's `plugins` folder.

### Customizing

You can modify the plugin to suit your needs, and once changes are made, simply re-run the `shadowJar` task to create a new JAR with your modifications.

---

## License

This project is licensed under the GNU General Public License v3.0. See the [LICENSE](LICENSE) file for more details.
