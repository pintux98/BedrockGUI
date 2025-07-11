# BedrockGUI

**A powerful and flexible GUI plugin for Minecraft Bedrock Edition servers with advanced form management, dynamic resource packs, and extensible action systems.**

## 🌟 Features

### Core Features
- **Multi-Platform Support**: Works on Paper, Velocity, and BungeeCord
- **Three Form Types**: Modal, Simple, and Custom forms with rich components
- **Dynamic Resource Packs**: Per-player resource packs with menu-specific themes
- **Extensible Action System**: 15+ built-in action handlers with custom action support
- **Unified Placeholder System**: Support for both dynamic placeholders and PlaceholderAPI
- **Conditional Buttons**: Show/hide buttons based on permissions and placeholder values
- **Platform Abstraction**: Clean separation between common logic and platform-specific implementations

### Advanced Features
- **Economy Integration**: Built-in support for Vault and other economy plugins
- **Sound Effects**: Customizable sound feedback with volume and pitch control
- **Action Sequences**: Chain multiple actions with delays and conditions
- **Random Actions**: Lottery systems and random rewards
- **Broadcasting**: Send messages to all players or specific groups
- **Admin Tools**: Server management through intuitive forms

## 📋 Requirements

### Minimum Requirements
- **Minecraft Bedrock Edition** server
- **Java 17** or higher
- One of the supported platforms:
  - **Paper/Spigot/Bukkit** 1.19+
  - **Velocity** 3.0+
  - **BungeeCord/Waterfall** (latest)

### Optional Dependencies
- **Vault** (for economy features)
- **PlaceholderAPI** (for advanced placeholders)
- **Floodgate** (for Bedrock player detection)
- **Geyser** (for cross-platform compatibility)

## 🚀 Installation

### Basic Installation
1. Download the appropriate JAR file for your platform:
   - `BedrockGUI-Paper-X.X.X.jar` for Paper/Spigot/Bukkit
   - `BedrockGUI-Velocity-X.X.X.jar` for Velocity
   - `BedrockGUI-BungeeCord-X.X.X.jar` for BungeeCord/Waterfall

2. Place the JAR file in your server's `plugins` folder

3. Restart your server

4. Configure the plugin by editing `plugins/BedrockGUI/config.yml`

### Resource Pack Setup (Optional)
1. Create a `resource_packs` folder in your plugin directory
2. Add your resource pack folders with the following structure:
   ```
   plugins/BedrockGUI/resource_packs/
   ├── default/
   │   ├── pack.mcpack
   │   └── manifest.json
   ├── dark_theme/
   │   ├── pack.mcpack
   │   └── manifest.json
   └── neon_theme/
       ├── pack.mcpack
       └── manifest.json
   ```

3. Enable resource packs in your config:
   ```yaml
   resource_packs:
     enabled: true
     auto_apply: true
     per_player_packs: true
   ```

## 🎮 Usage

### Commands
- `/bedrock` - Open the main menu
- `/bedrock menu <menu_name>` - Open a specific menu
- `/bedrock menu <menu_name> [args...]` - Open menu with arguments
- `/bedrock reload` - Reload configuration
- `/bedrock theme <player> <theme>` - Change player's resource pack theme
- `/bedrock pack send <player> <pack>` - Send resource pack to player

### Permissions
- `bedrockgui.use` - Basic plugin usage
- `bedrockgui.menu.*` - Access to all menus
- `bedrockgui.menu.<menu_name>` - Access to specific menu
- `bedrockgui.admin` - Administrative commands
- `bedrockgui.reload` - Reload configuration
- `bedrockgui.theme` - Change themes

## 📝 Configuration Guide

### Form Types

#### Modal Forms
Simple yes/no or confirmation dialogs:
```yaml
confirm_teleport:
  title: "§bTeleport Confirmation"
  type: "modal"
  text: "Do you want to teleport to spawn?"
  button1: "§aYes"
  button2: "§cNo"
  actions:
    button1:
      - type: "teleport"
        value: "spawn"
    button2:
      - type: "message"
        value: "§cTeleport cancelled"
```

#### Simple Forms
Button-based menus with images:
```yaml
main_menu:
  title: "§6§lMain Menu"
  type: "simple"
  content: "§7Welcome to the server!"
  buttons:
    - text: "§a§lShop"
      image:
        type: "path"
        data: "textures/ui/icon_shop"
      onClick: "open:shop"
    - text: "§b§lTeleports"
      image:
        type: "url"
        data: "https://example.com/teleport_icon.png"
      onClick: "open:teleports"
```

#### Custom Forms
Advanced forms with multiple input components:
```yaml
shop_form:
  title: "§6§lServer Shop"
  type: "custom"
  components:
    - type: "label"
      text: "§7Welcome to the shop!"
    - type: "dropdown"
      text: "§bSelect Item:"
      options: ["Diamond", "Emerald", "Gold"]
      default: 0
    - type: "slider"
      text: "§aQuantity:"
      min: 1
      max: 64
      default: 1
    - type: "input"
      text: "§ePlayer Name (optional):"
      placeholder: "Leave empty for yourself"
    - type: "toggle"
      text: "§dGift Wrap (+10 coins)"
      default: false
  actions:
    - type: "economy"
      value: "remove:$slider"
    - type: "command"
      value: "give $player $dropdown $slider"
```

### Component Types

| Component | Description | Properties |
|-----------|-------------|------------|
| `label` | Display text | `text` |
| `input` | Text input field | `text`, `placeholder`, `default` |
| `dropdown` | Selection dropdown | `text`, `options`, `default` |
| `slider` | Numeric slider | `text`, `min`, `max`, `step`, `default` |
| `toggle` | Boolean switch | `text`, `default` |

### Action System

#### Built-in Action Handlers

| Action Type | Description | Example |
|-------------|-------------|----------|
| `command` | Execute command as player | `command:spawn` |
| `server` | Execute command as console | `server:give $player diamond 64` |
| `message` | Send message to player | `message:§aWelcome to the server!` |
| `open` | Open another menu | `open:shop` |
| `close` | Close current menu | `close` |
| `teleport` | Teleport player | `teleport:spawn` or `teleport:100,64,200` |
| `sound` | Play sound effect | `sound:ui.button.click:1.0:1.2` |
| `economy` | Economy operations | `economy:add:100` |
| `delay` | Add delay between actions | `delay:1000` |
| `conditional` | Execute based on conditions | `conditional:permission:vip.access:message:VIP only!` |
| `random` | Random action selection | `random:command:give $player diamond|command:give $player emerald` |
| `broadcast` | Send message to all players | `broadcast:Server announcement!` |

#### Action Sequences
Chain multiple actions together:
```yaml
actions:
  - type: "sound"
    value: "entity.experience_orb.pickup"
  - type: "delay"
    value: "500"
  - type: "economy"
    value: "add:100"
  - type: "message"
    value: "§a+100 coins!"
  - type: "broadcast"
    value: "permission:admin.notify:$player earned 100 coins"
```

### Placeholder System

#### Dynamic Placeholders (`$` prefix)
Replaced with dynamic values from player input or context:
- `$player` - Player's name
- `$uuid` - Player's UUID
- `$value` - Selected value from form component
- `$input`, `$dropdown`, `$slider`, `$toggle` - Form component values
- `$1`, `$2`, `$3` - Menu arguments
- `$amount`, `$balance` - Economy context
- `$x`, `$y`, `$z`, `$world` - Location context

#### PlaceholderAPI Placeholders (`%` prefix)
Processed by PlaceholderAPI if available:
- `%player_name%` - Player's display name
- `%vault_eco_balance%` - Economy balance
- `%player_world%` - Current world
- `%player_health%` - Player's health
- `%player_level%` - Player's level

### Conditional Buttons

Show or hide buttons based on conditions:

#### Permission-based Conditions
```yaml
buttons:
  - text: "§6§lVIP Area"
    onClick: "teleport:vip_spawn"
    conditions:
      - type: "permission"
        value: "vip.access"
```

#### Placeholder-based Conditions
```yaml
buttons:
  - text: "§a§lExpensive Item ($1000)"
    onClick: "economy:remove:1000:command:give $player diamond_block"
    conditions:
      - type: "placeholder"
        placeholder: "%vault_eco_balance%"
        operator: ">="
        value: "1000"
```

#### Supported Operators
- `equals`, `==` - String equality
- `not_equals`, `!=` - String inequality
- `contains` - String contains
- `starts_with` - String starts with
- `ends_with` - String ends with
- `>`, `greater_than` - Numeric greater than
- `>=`, `greater_equal` - Numeric greater than or equal
- `<`, `less_than` - Numeric less than
- `<=`, `less_equal` - Numeric less than or equal

### Resource Pack Integration

#### Per-Player Resource Packs
```yaml
resource_packs:
  enabled: true
  auto_apply: true
  per_player_packs: true
  default_pack: "default"
```

#### Menu-Specific Themes
```yaml
themed_menu:
  title: "§b§lThemed Interface"
  type: "simple"
  resource_pack_themes:
    default:
      title: "§7§lStandard Interface"
      button_style: "default"
    dark:
      title: "§8§lDark Interface"
      button_style: "dark"
      background_color: "#1a1a1a"
    neon:
      title: "§d§lNeon Interface"
      button_style: "neon"
      background_color: "#ff00ff"
```

## 🔧 Developer API

### Basic API Usage

#### Opening Menus Programmatically
```java
// Get the API instance
BedrockGUIAPI api = BedrockGUI.getApi();

// Open a menu for a player
api.openMenu(player, "shop");

// Open a menu with arguments
api.openMenu(player, "teleport", "spawn", "free");
```

#### Creating Custom Action Handlers
```java
public class CustomActionHandler implements ActionHandler {
    @Override
    public String getType() {
        return "custom";
    }
    
    @Override
    public ActionResult execute(ActionContext context) {
        String value = context.getValue();
        Player player = context.getPlayer();
        
        // Your custom logic here
        player.sendMessage("Custom action executed: " + value);
        
        return ActionResult.success();
    }
    
    @Override
    public boolean validateValue(String value) {
        return value != null && !value.isEmpty();
    }
}

// Register the handler
api.getActionRegistry().registerHandler(new CustomActionHandler());
```

#### Resource Pack Management
```java
// Get resource pack manager
ResourcePackManager packManager = api.getResourcePackManager();

// Send resource pack to player
packManager.sendResourcePack(player, "dark_theme");

// Get player's current pack
String currentPack = packManager.getPlayerPack(player);

// Check if pack is loaded
boolean isLoaded = packManager.isPackLoaded("neon_theme");
```

#### Event Handling
```java
@EventHandler
public void onFormSubmit(FormSubmitEvent event) {
    Player player = event.getPlayer();
    String menuName = event.getMenuName();
    Map<String, Object> responses = event.getResponses();
    
    // Handle form submission
    if (menuName.equals("custom_shop")) {
        String selectedItem = (String) responses.get("dropdown");
        Integer quantity = (Integer) responses.get("slider");
        // Process purchase...
    }
}

@EventHandler
public void onResourcePackApply(ResourcePackApplyEvent event) {
    Player player = event.getPlayer();
    String packName = event.getPackName();
    
    player.sendMessage("§aResource pack '" + packName + "' applied!");
}
```

### Platform Abstraction

#### Implementing Platform Interfaces
```java
// Economy manager implementation
public class VaultEconomyManager implements PlatformEconomyManager {
    private Economy economy;
    
    @Override
    public boolean hasAccount(String playerName) {
        return economy.hasAccount(playerName);
    }
    
    @Override
    public double getBalance(String playerName) {
        return economy.getBalance(playerName);
    }
    
    @Override
    public boolean withdraw(String playerName, double amount) {
        EconomyResponse response = economy.withdrawPlayer(playerName, amount);
        return response.transactionSuccess();
    }
    
    @Override
    public boolean deposit(String playerName, double amount) {
        EconomyResponse response = economy.depositPlayer(playerName, amount);
        return response.transactionSuccess();
    }
}

// Sound manager implementation
public class PaperSoundManager implements PlatformSoundManager {
    @Override
    public void playSound(Player player, String soundName, float volume, float pitch) {
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            // Handle invalid sound
        }
    }
    
    @Override
    public boolean isValidSound(String soundName) {
        try {
            Sound.valueOf(soundName.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
```

#### Custom Platform Implementation
```java
public class MyPlatformPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        // Initialize platform managers
        PlatformCommandExecutor commandExecutor = new MyCommandExecutor();
        PlatformSoundManager soundManager = new MySoundManager();
        PlatformEconomyManager economyManager = new MyEconomyManager();
        
        // Initialize BedrockGUI with platform managers
        BedrockGUI.initialize(
            this,
            commandExecutor,
            soundManager,
            economyManager
        );
    }
}
```

## 🎯 Examples

### Server Owner Examples

#### Simple Shop Menu
```yaml
shop:
  title: "§6§lServer Shop"
  type: "simple"
  content: "§7Buy items with your coins!"
  buttons:
    - text: "§a§lDiamond ($100)"
      onClick: |
        conditional:economy:check:100:
        economy:remove:100:
        command:give $player diamond 1:
        sound:entity.experience_orb.pickup:
        message:§aPurchased 1 diamond!
    - text: "§b§lEmerald ($50)"
      onClick: |
        conditional:economy:check:50:
        economy:remove:50:
        command:give $player emerald 1:
        sound:entity.experience_orb.pickup:
        message:§aPurchased 1 emerald!
```

#### Teleport Hub
```yaml
teleports:
  title: "§b§lTeleport Hub"
  type: "simple"
  content: "§7Choose your destination"
  buttons:
    - text: "§a§lSpawn (Free)"
      onClick: "teleport:spawn:message:§aTeleported to spawn!"
    - text: "§c§lNether ($100)"
      onClick: |
        conditional:economy:check:100:
        economy:remove:100:
        teleport:0,64,0,world_nether:
        message:§cWelcome to the Nether!
      conditions:
        - type: "permission"
          value: "teleport.nether"
```

#### Admin Panel
```yaml
admin_panel:
  title: "§c§lAdmin Panel"
  type: "custom"
  components:
    - type: "input"
      text: "§ePlayer Name:"
      placeholder: "Enter player name"
    - type: "dropdown"
      text: "§bAction:"
      options: ["Kick", "Ban", "Give Diamond", "Heal"]
  actions:
    - type: "conditional"
      value: "placeholder:$dropdown:equals:Kick:server:kick $input Kicked by admin"
    - type: "conditional"
      value: "placeholder:$dropdown:equals:Give Diamond:server:give $input diamond 64"
```

### Developer Examples

#### Custom Action Handler
```java
@Component
public class TeleportRandomActionHandler implements ActionHandler {
    private final List<Location> randomLocations;
    
    public TeleportRandomActionHandler() {
        this.randomLocations = loadRandomLocations();
    }
    
    @Override
    public String getType() {
        return "teleport_random";
    }
    
    @Override
    public ActionResult execute(ActionContext context) {
        Player player = context.getPlayer();
        
        if (randomLocations.isEmpty()) {
            return ActionResult.failure("No random locations configured");
        }
        
        Location randomLoc = randomLocations.get(
            ThreadLocalRandom.current().nextInt(randomLocations.size())
        );
        
        player.teleport(randomLoc);
        player.sendMessage("§aTeleported to a random location!");
        
        return ActionResult.success();
    }
}
```

#### Form Builder Utility
```java
public class FormBuilder {
    public static void createDynamicShop(Player player, String category) {
        BedrockGUIAPI api = BedrockGUI.getApi();
        
        // Build form dynamically based on category
        FormMenu.Builder builder = FormMenu.builder()
            .title("§6§l" + category + " Shop")
            .type(FormType.CUSTOM);
            
        // Add category-specific items
        List<ShopItem> items = getItemsForCategory(category);
        
        builder.addComponent(Component.label(
            "§7Welcome to the " + category + " shop!"
        ));
        
        String[] itemNames = items.stream()
            .map(ShopItem::getName)
            .toArray(String[]::new);
            
        builder.addComponent(Component.dropdown(
            "§bSelect Item:", 
            itemNames, 
            0
        ));
        
        builder.addComponent(Component.slider(
            "§aQuantity:", 
            1, 64, 1, 1
        ));
        
        // Add purchase action
        builder.addAction(Action.conditional(
            "economy:check:$price",
            Action.sequence(
                Action.economy("remove:$price"),
                Action.command("give $player $item $slider"),
                Action.sound("entity.experience_orb.pickup"),
                Action.message("§aPurchased $slider $item!")
            )
        ));
        
        FormMenu menu = builder.build();
        api.openMenu(player, menu);
    }
}
```

## 🔍 Troubleshooting

### Common Issues

#### Forms Not Opening
- **Check permissions**: Ensure players have `bedrockgui.use` and menu-specific permissions
- **Verify Floodgate**: Make sure Floodgate is installed for Bedrock player detection
- **Check console**: Look for error messages in server console
- **Test with Java players**: Some features may not work with Java Edition players

#### Resource Packs Not Loading
- **File structure**: Ensure resource packs follow the correct directory structure
- **Manifest validation**: Check that `manifest.json` files are valid
- **File permissions**: Verify the server can read resource pack files
- **Client compatibility**: Ensure resource packs are compatible with client version

#### Actions Not Executing
- **Syntax validation**: Check action syntax in configuration
- **Permission checks**: Verify players have required permissions for actions
- **Economy integration**: Ensure Vault and economy plugin are properly installed
- **Placeholder resolution**: Check that placeholders are resolving correctly

#### Performance Issues
- **Reduce form complexity**: Limit the number of components in custom forms
- **Optimize resource packs**: Use smaller file sizes and efficient textures
- **Cache management**: Monitor memory usage and adjust cache settings
- **Database optimization**: Optimize player data storage if using database

### Debug Mode
Enable debug mode for detailed logging:
```yaml
debug: true
```

This will log:
- Form open/close events
- Action execution details
- Placeholder resolution
- Resource pack operations
- Error stack traces

### Getting Help
- **Documentation**: Check this README and configuration examples
- **Console logs**: Always check server console for error messages
- **Test environment**: Test configurations in a development environment first
- **Community support**: Join our Discord server for community help
- **Issue reporting**: Report bugs on our GitHub repository

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details on:
- Code style and standards
- Pull request process
- Issue reporting
- Development setup

## 📞 Support

- **Discord**: [Join our Discord server](https://discord.gg/bedrockgui)
- **GitHub Issues**: [Report bugs and request features](https://github.com/BedrockGUI/BedrockGUI/issues)
- **Documentation**: [Wiki and guides](https://github.com/BedrockGUI/BedrockGUI/wiki)
- **Email**: support@bedrockgui.com

---

**BedrockGUI** - Making Minecraft Bedrock server management beautiful and intuitive. ✨
