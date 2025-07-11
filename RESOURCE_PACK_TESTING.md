# Resource Pack Testing Guide

This guide explains how to test the resource pack functionality in BedrockGUI.

## Overview

The resource pack feature allows you to send custom resource packs to Bedrock players through menu actions. This enhances the user experience with custom textures, UI elements, and themes.

## Configuration

### Resource Pack Settings

Resource packs are configured in `config.yml` under the `resource_packs` section:

```yaml
resource_packs:
  enabled: true
  default_pack: "ui_pack"
  
  # Available resource packs
  packs:
    ui_pack:
      name: "Enhanced UI Pack"
      url: "https://example.com/packs/ui_pack.mcpack"
      hash: "abc123def456"
      force: false
      description: "Enhanced UI elements for better user experience"
```

### Pack Properties

- **name**: Display name of the resource pack
- **url**: Download URL for the resource pack file (.mcpack)
- **hash**: SHA-256 hash of the pack file for verification
- **force**: Whether to force the pack installation
- **description**: Description of what the pack contains

### Menu-Specific Packs

You can assign specific resource packs to menus:

```yaml
menu_packs:
  main: "ui_pack"
  resourcepack_test: "custom_pack"
  pack_sequence: "base_pack"
```

## Action Usage

### ResourcePack Action

The `resourcepack` action type allows you to send resource packs to players:

```yaml
onClick: "resourcepack:pack_name"
```

### Examples

1. **Send UI Pack**: `resourcepack:ui_pack`
2. **Send Custom Pack**: `resourcepack:custom_pack`
3. **Send with Placeholder**: `resourcepack:%selected_pack%`

## Testing Menus

### Resource Pack Test Menu

Access via command: `/resourcepack` or `/rp`

This menu demonstrates:
- Sending different types of resource packs
- Displaying pack information
- Sequential pack loading

### Pack Sequence Test Menu

Access via command: `/packseq`

This menu demonstrates:
- Step-by-step pack loading
- Sequential pack application
- Combined pack loading with delays

## Testing Steps

### 1. Basic Pack Sending

1. Join as a Bedrock player
2. Run `/resourcepack` to open the test menu
3. Click "Send UI Pack" to test basic pack sending
4. Verify the pack is received and applied

### 2. Sequential Pack Loading

1. Open the pack sequence menu with `/packseq`
2. Test individual steps (Step 1, 2, 3)
3. Test the "Load All Packs" button for sequential loading

### 3. Menu-Specific Packs

1. Open different menus and observe automatic pack loading
2. Check that the correct pack is sent for each menu
3. Verify fallback behavior when packs are unavailable

## Troubleshooting

### Common Issues

1. **Pack Not Found**: Ensure the pack name exists in the configuration
2. **Download Failed**: Check the URL and hash values
3. **Not Bedrock Player**: Resource packs only work for Bedrock Edition players
4. **Permissions**: Ensure players have the required permissions

### Debug Information

Enable debug mode in settings:

```yaml
settings:
  debug: true
```

This will provide detailed logging for resource pack operations.

### Log Messages

Look for these log messages:
- `Successfully sent resource pack 'pack_name' to player PlayerName`
- `Resource pack 'pack_name' not found in available packs`
- `Failed to send resource pack 'pack_name' to player PlayerName`

## Advanced Features

### Conditional Pack Loading

You can use conditional actions to load different packs based on player conditions:

```yaml
onClick: "conditional:has_permission(vip)|resourcepack:vip_pack|resourcepack:default_pack"
```

### Pack Sequences with Delays

Use the delay action to create smooth pack transitions:

```yaml
onClick: "resourcepack:base_pack|delay:2000|resourcepack:ui_pack"
```

### Placeholder Support

Resource pack names support placeholders:

```yaml
onClick: "resourcepack:%player_world%_pack"
```

## Best Practices

1. **Test with Real Packs**: Use actual .mcpack files for realistic testing
2. **Verify Hashes**: Always include correct SHA-256 hashes
3. **Gradual Loading**: Use delays between multiple pack loads
4. **Fallback Options**: Provide alternatives when packs fail to load
5. **Permission Checks**: Ensure proper permission handling

## Platform Compatibility

- **Geyser**: Full support with GeyserResourcePackManager
- **Paper**: Requires Bedrock player detection
- **Velocity/BungeeCord**: Proxy-level pack management

## Security Considerations

1. **Trusted Sources**: Only use resource packs from trusted sources
2. **Hash Verification**: Always verify pack integrity with hashes
3. **Size Limits**: Be mindful of pack sizes for download performance
4. **Player Consent**: Consider making pack installation optional

For more information, see the main documentation and example configurations.