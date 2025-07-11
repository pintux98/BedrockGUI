# Per-Player Resource Pack Usage Guide

This guide explains how to use the per-player resource pack functionality in BedrockGUI.

## Overview

The BedrockGUI plugin now supports per-player resource packs, allowing you to send different resource packs to individual players without affecting all players on the server.

## API Methods

### 1. Send Resource Pack to Specific Player
```java
// Get the BedrockGUI API instance
BedrockGuiAPI api = BedrockGUI.getApi();

// Send a resource pack to a specific player
UUID playerUuid = player.getUniqueId();
String packIdentifier = "my_custom_pack";
api.sendResourcePackToPlayer(playerUuid, packIdentifier);
```

### 2. Set Multiple Resource Packs for a Player
```java
// Replace all active packs for a player with a new set
Set<String> packIdentifiers = Set.of("pack1", "pack2", "pack3");
api.setPlayerResourcePacks(playerUuid, packIdentifiers);
```

### 3. Remove Resource Pack from Player
```java
// Remove a specific pack from a player
api.removeResourcePackFromPlayer(playerUuid, "pack_to_remove");
```

### 4. Get Player's Active Packs
```java
// Get all active resource packs for a player
Set<String> activePacks = api.getPlayerActivePacks(playerUuid);
System.out.println("Player has " + activePacks.size() + " active packs");
```

## How It Works

1. **Pack Registration**: Resource packs are loaded from the `plugins/BedrockGUI/packs/` directory
2. **Per-Player Tracking**: The system tracks which packs are active for each player
3. **Geyser Integration**: Packs are made available through Geyser's resource pack system
4. **Event-Based Delivery**: Resource packs are delivered when players connect or when packs are assigned

## Important Notes

- **Geyser Required**: This functionality requires Geyser to be installed and running
- **Pack Availability**: Packs must be placed in `plugins/BedrockGUI/packs/` directory
- **Connection Timing**: For immediate pack delivery, players may need to reconnect after pack assignment
- **API Version Limitation**: Due to Geyser API version constraints, `SessionLoadResourcePacksEvent` is not available, so the system uses alternative methods for per-player delivery

## Configuration

Ensure resource packs are enabled in your BedrockGUI configuration:

```yaml
resource_packs:
  enabled: true
  default_pack: "bedrockgui_default.mcpack"
  per_menu_packs:
    main_menu: "main_theme.mcpack"
    shop_menu: "shop_theme.mcpack"
```

## Example Usage in Plugin

```java
public class MyPlugin extends JavaPlugin {
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Send a welcome resource pack to new players
        if (!player.hasPlayedBefore()) {
            BedrockGuiAPI api = BedrockGUI.getApi();
            api.sendResourcePackToPlayer(player.getUniqueId(), "welcome_pack");
        }
    }
    
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (event.getMessage().startsWith("/vip")) {
            // Send VIP resource pack to VIP players
            BedrockGuiAPI api = BedrockGUI.getApi();
            api.sendResourcePackToPlayer(event.getPlayer().getUniqueId(), "vip_pack");
        }
    }
}
```

## Troubleshooting

1. **Packs not loading**: Check that pack files exist in `plugins/BedrockGUI/packs/`
2. **Players not receiving packs**: Ensure Geyser is running and players are connected via Bedrock Edition
3. **Immediate delivery issues**: Players may need to reconnect for immediate pack delivery
4. **Console errors**: Check server console for resource pack loading errors

This per-player resource pack system provides flexibility for creating customized experiences for different players without affecting the entire server.