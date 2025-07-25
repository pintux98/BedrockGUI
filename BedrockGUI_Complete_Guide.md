# BedrockGUI Complete Guide

A comprehensive guide to creating SIMPLE, MODAL, and CUSTOM forms with actions, conditions, and advanced features.

## Table of Contents

1. [Form Types Overview](#form-types-overview)
2. [Available Actions](#available-actions)
3. [Available Conditions](#available-conditions)
4. [Simple Forms](#simple-forms)
5. [Modal Forms](#modal-forms)
6. [Custom Forms](#custom-forms)
7. [Advanced Examples](#advanced-examples)
8. [Best Practices](#best-practices)

---

## Form Types Overview

BedrockGUI supports three main form types:

- **SIMPLE**: Button-based menus with multiple options
- **MODAL**: Yes/No confirmation dialogs (exactly 2 buttons)
- **CUSTOM**: Input forms with various components (text inputs, toggles, dropdowns, etc.)

---

## Available Actions

BedrockGUI provides a comprehensive set of action handlers:

### Core Actions
- **`message`**: Send messages to players
- **`command`**: Execute commands as the player
- **`server`**: Execute commands as the server console
- **`open`**: Open other forms/menus
- **`close`**: Close the current form
- **`broadcast`**: Send messages to all players or specific groups

### Utility Actions
- **`delay`**: Add delays between actions (max 30 seconds)
- **`sound`**: Play sounds to players
- **`economy`**: Add/remove/set player money (requires economy plugin)
- **`random`**: Execute random actions from a list

### Advanced Actions
- **`conditional`**: Execute actions based on conditions (supports if-else pattern)
- **`teleport`**: Teleport players to locations

### Action Syntax
```yaml
# Basic syntax
actionType:actionData

# Multiple actions (separated by colons)
action1:data1:action2:data2:action3:data3

# Examples
message:Hello World!
command:give {player} diamond 1
server:gamemode creative {player}
open:shop_menu
delay:1000
sound:ui.button.click
economy:add:100
random:message:Prize 1|message:Prize 2|message:Prize 3

# Conditional actions (if-else pattern)
conditional:permission:admin.use:message:You are an admin!  # Only success action
conditional:placeholder:{world}:equals:world:message:You're in overworld:message:You're not in overworld  # Success and failure actions
conditional:placeholder:{balance}:>=:1000:economy:remove:100:message:Insufficient funds!  # Success and failure actions
```

### Conditional Actions (If-Else Pattern)

Conditional actions now support an if-else pattern where you can specify both success and failure actions:

```yaml
# Format: conditional:condition_type:condition_value[:operator:expected_value]:success_action_type:success_action_data[:failure_action_type:failure_action_data]

# Examples with both success and failure actions
conditional:permission:vip.access:message:§aWelcome VIP!:message:§cYou need VIP access!
conditional:placeholder:{balance}:>=:500:economy:remove:500:message:§cInsufficient funds!
conditional:placeholder:{world}:equals:survival:teleport:survival_spawn:teleport:creative_spawn
conditional:placeholder:{player_health}:>:10:message:§aYou're healthy!:message:§cYou need healing!

# Examples with only success actions (failure is ignored)
conditional:permission:admin.use:server:gamemode creative {player}
conditional:placeholder:{balance}:>=:1000:economy:add:100
```

---

## Available Conditions

Conditions are used for conditional buttons and actions:

### Condition Types

#### Permission Conditions
```yaml
# Check if player has permission
permission:some.permission.node

# Check if player does NOT have permission
not:permission:some.permission.node
```

#### Placeholder Conditions
```yaml
# Basic syntax
placeholder:{placeholder_name}:operator:expected_value

# With negation
not:placeholder:{placeholder_name}:operator:expected_value
```

### Available Operators

#### String Operators
- **`equals`** or **`==`**: Exact match
- **`not_equals`** or **`!=`**: Not equal
- **`contains`**: Contains substring
- **`starts_with`**: Starts with string
- **`ends_with`**: Ends with string
- **`regex`**: Regular expression match
- **`empty`**: Is empty or null
- **`not_empty`**: Is not empty

#### Numeric Operators
- **`>`** or **`greater_than`**: Greater than
- **`>=`** or **`greater_equal`**: Greater than or equal
- **`<`** or **`less_than`**: Less than
- **`<=`** or **`less_equal`**: Less than or equal

### Condition Examples
```yaml
# Permission examples
show_condition: "permission:admin.use"
show_condition: "not:permission:banned.user"

# Placeholder examples (supports PlaceholderAPI)
show_condition: "placeholder:{balance}:>=:1000"
show_condition: "placeholder:{player_world}:equals:survival"
show_condition: "placeholder:{player_health}:>:10"
show_condition: "placeholder:{player_name}:contains:Admin"
show_condition: "not:placeholder:{player_gamemode}:equals:creative"
show_condition: "placeholder:%vault_eco_balance%:>=:500"  # PlaceholderAPI example
```

---

## Simple Forms

Simple forms are button-based menus that allow players to choose from multiple options.

### Basic Simple Form

```yaml
basic_menu:
  type: "SIMPLE"
  title: "§6§lMain Menu"
  description: "§7Choose an option below:"
  buttons:
    option1:
      text: "§a§lOption 1"
      onClick: "message:You selected option 1!"
```

### Medium Complexity Simple Form

```yaml
server_menu:
  type: "SIMPLE"
  title: "§b§lServer Menu"
  description: "§7Welcome to our server! Choose what you'd like to do:"
  buttons:
    teleport_spawn:
      text: "§a§l🏠 Teleport to Spawn"
      image: "textures/ui/icon_recipe_nature"
      onClick: "teleport:spawn:sound:random.levelup:message:§aWelcome to spawn!"
    
    player_shop:
      text: "§2§l🛒 Player Shop"
      image: "textures/ui/icon_recipe_item"
      onClick: "open:shop_menu:sound:ui.button.click"
      show_condition: "permission:shop.use"
    
    daily_reward:
      text: "§6§l🎁 Daily Reward"
      image: "textures/ui/icon_recipe_equipment"
      onClick: "economy:add:100:message:§a+100 coins daily reward!:sound:random.levelup"
    
    admin_panel:
      text: "§c§l⚙️ Admin Panel"
      image: "textures/ui/icon_recipe_construction"
      onClick: "open:admin_menu"
      show_condition: "permission:admin.use"
```

### Complex Simple Form with Advanced Features

```yaml
advanced_hub:
  type: "SIMPLE"
  title: "§6§l✦ Advanced Server Hub ✦"
  description: "§7Welcome §e{player}§7! Balance: §a${balance} §7| World: §b{player_world}"
  buttons:
    vip_area:
      text: "§d§l👑 VIP Area"
      image: "textures/ui/icon_recipe_equipment"
      onClick: "conditional:permission:vip.access:teleport:vip_lounge:message:§c§lVIP access required!"
      show_condition: "permission:vip.access"
    
    minigames:
      text: "§e§l🎮 Minigames"
      image: "textures/ui/icon_recipe_nature"
      onClick: "conditional:placeholder:{balance}:>=:50:economy:remove:50:message:§c§lInsufficient funds! Need $50 to play."
    
    random_reward:
      text: "§5§l🎲 Random Reward"
      image: "textures/ui/icon_recipe_item"
      onClick: "random:economy:add:10|economy:add:50|economy:add:100|economy:add:500:delay:1000:random:message:§aYou got $10!|message:§aYou got $50!|message:§aYou got $100!|message:§a§lJACKPOT! $500!:sound:random.levelup"
    
    time_based_feature:
      text: "§b§l⏰ Time-Based Feature"
      onClick: "conditional:placeholder:{server_time}:>=:6000:message:§a§lDaytime feature activated!:message:§e§lNighttime feature activated!"
    
    player_stats:
      text: "§3§l📊 Player Statistics"
      onClick: "message:§3§l=== Player Stats ===\n§7Name: §e{player}\n§7Health: §c{player_health}/20\n§7World: §b{player_world}\n§7Balance: §a${balance}\n§7Playtime: §e{player_playtime} hours"
    
    broadcast_system:
      text: "§6§l📢 Announcements"
      onClick: "conditional:permission:broadcast.use:open:broadcast_menu:message:§c§lNo permission to use broadcasts."
      show_condition: "permission:broadcast.use"
```

---

## Modal Forms

Modal forms are confirmation dialogs with exactly two buttons (Yes/No, Accept/Decline, etc.).

### Basic Modal Form

```yaml
confirm_teleport:
  type: "MODAL"
  title: "§e§lConfirm Teleport"
  description: "§7Are you sure you want to teleport to spawn?\n§cThis will cost $10."
  buttons:
    confirm:
      text: "§a§lYes, Teleport"
      onClick: "economy:remove:10:teleport:spawn:message:§a§lTeleported to spawn!"
    
    cancel:
      text: "§c§lNo, Cancel"
      onClick: "message:§e§lTeleport cancelled.:close"
```

### Medium Complexity Modal Form

```yaml
purchase_confirmation:
  type: "MODAL"
  title: "§2§l💰 Purchase Confirmation"
  description: "§7Item: §e{item_name}\n§7Price: §a${item_price}\n§7Your Balance: §a${balance}\n\n§eConfirm this purchase?"
  buttons:
    purchase:
      text: "§a§l✓ Buy Now"
      onClick: "conditional:placeholder:{balance}:>=:{item_price}:economy:remove:{item_price}:message:§c§lInsufficient funds!"
    
    cancel:
      text: "§c§l✗ Cancel"
      onClick: "message:§e§lPurchase cancelled.:sound:ui.button.click:open:shop_menu"
```

### Complex Modal Form with Advanced Logic

```yaml
reset_player_data:
  type: "MODAL"
  title: "§c§l⚠️ DANGER ZONE ⚠️"
  description: "§c§l§nWARNING: IRREVERSIBLE ACTION\n\n§7This will completely reset your player data:\n§c• All inventory items will be lost\n§c• All money will be reset to $0\n§c• All achievements will be cleared\n§c• Your location will reset to spawn\n\n§e§lThis action cannot be undone!\n§7Type your username to confirm: §e{player}"
  permission: "reset.own.data"
  buttons:
    confirm_reset:
      text: "§c§l💀 RESET EVERYTHING"
      onClick: "conditional:permission:reset.confirmed:server:clear {player}:message:§e§lYou need confirmation permission to reset data."
    
    abort:
      text: "§a§l🛡️ Keep My Data"
      onClick: "message:§a§lSmart choice! Your data is safe.:sound:random.levelup:delay:500:message:§7If you change your mind, you can always come back.:open:settings_menu"
```

---

## Custom Forms

Custom forms allow for complex input collection with various component types.

### Basic Custom Form

```yaml
player_feedback:
  type: "CUSTOM"
  title: "§b§lPlayer Feedback"
  components:
    player_name:
      type: "input"
      text: "§7Your Name:"
      placeholder: "Enter your username"
      default: "{player}"
    
    feedback_text:
      type: "input"
      text: "§7Your Feedback:"
      placeholder: "Tell us what you think..."
    
    rating:
      type: "slider"
      text: "§7Rate our server (1-10):"
      min: 1
      max: 10
      step: 1
      default: 5
    
    recommend:
      type: "toggle"
      text: "§7Would you recommend our server?"
      default: true
    
    submit:
      type: "button"
      text: "§a§lSubmit Feedback"
      onClick: "message:§a§lThank you for your feedback!:broadcast:permission:admin.feedback:§e§l[FEEDBACK] §7New feedback from §e{player_name}§7: Rating §a{rating}§7/10"
```

### Medium Complexity Custom Form

```yaml
shop_purchase:
  type: "CUSTOM"
  title: "§2§l🛒 Item Purchase"
  components:
    item_category:
      type: "dropdown"
      text: "§7Select Category:"
      options:
        - "Weapons"
        - "Armor"
        - "Tools"
        - "Food"
        - "Blocks"
      default: 0
    
    item_name:
      type: "input"
      text: "§7Item Name:"
      placeholder: "diamond_sword"
    
    quantity:
      type: "slider"
      text: "§7Quantity:"
      min: 1
      max: 64
      step: 1
      default: 1
    
    express_delivery:
      type: "toggle"
      text: "§7Express Delivery (+$5):"
      default: false
    
    gift_wrap:
      type: "toggle"
      text: "§7Gift Wrap (+$2):"
      default: false
    
    recipient:
      type: "input"
      text: "§7Recipient (leave empty for yourself):"
      placeholder: "player_name"
    
    purchase:
      type: "button"
      text: "§a§lPurchase Items"
      onClick: "conditional:placeholder:{item_name}:not_empty:server:give {recipient} {item_name} {quantity}:message:§c§lPlease enter an item name!"
```

### Complex Custom Form with Advanced Features

```yaml
character_creator:
  type: "CUSTOM"
  title: "§d§l✨ Character Creator ✨"
  permission: "character.create"
  components:
    character_name:
      type: "input"
      text: "§7Character Name:"
      placeholder: "Enter character name"
      show_condition: "not:placeholder:{has_character}:equals:true"
    
    character_class:
      type: "dropdown"
      text: "§7Choose Class:"
      options:
        - "§c⚔️ Warrior"
        - "§9🏹 Archer"
        - "§5🔮 Mage"
        - "§a🗡️ Rogue"
        - "§e🛡️ Paladin"
      default: 0
      show_condition: "not:placeholder:{has_character}:equals:true"
    
    starting_stats:
      type: "label"
      text: "§7§l=== Starting Stats ===\n§cStrength: {class_strength}\n§9Agility: {class_agility}\n§5Intelligence: {class_intelligence}\n§aLuck: {class_luck}"
    
    difficulty:
      type: "dropdown"
      text: "§7Difficulty Mode:"
      options:
        - "§a🟢 Easy (2x XP, 0.5x damage)"
        - "§e🟡 Normal (1x XP, 1x damage)"
        - "§c🔴 Hard (0.8x XP, 1.5x damage)"
        - "§4💀 Nightmare (0.5x XP, 2x damage)"
      default: 1
    
    pvp_enabled:
      type: "toggle"
      text: "§7Enable PvP:"
      default: false
    
    starting_location:
      type: "dropdown"
      text: "§7Starting Location:"
      options:
        - "🏘️ Peaceful Village"
        - "🌲 Dark Forest"
        - "🏔️ Mountain Peak"
        - "🏖️ Coastal Town"
        - "🏜️ Desert Outpost"
      default: 0
    
    bonus_features:
      type: "label"
      text: "§7§l=== Bonus Features ===\n§eVIP Status: {vip_status}\n§aPremium Benefits: {premium_benefits}\n§bSpecial Abilities: {special_abilities}"
      show_condition: "permission:character.premium"
    
    accept_terms:
      type: "toggle"
      text: "§7I accept the character creation terms"
      default: false
    
    create_character:
      type: "button"
      text: "§a§l✨ Create Character"
      onClick: "conditional:{accept_terms}:server:character create {player} {character_name} {character_class}:message:§c§lYou must accept the terms to create a character!"
      show_condition: "not:placeholder:{has_character}:equals:true"
    
    character_info:
      type: "label"
      text: "§7§l=== Current Character ===\n§eName: §b{character_name}\n§eClass: §d{character_class}\n§eLevel: §a{character_level}\n§eLocation: §6{character_location}"
      show_condition: "placeholder:{has_character}:equals:true"
    
    delete_character:
      type: "button"
      text: "§c§l💀 Delete Character"
      onClick: "open:confirm_character_deletion"
      show_condition: "placeholder:{has_character}:equals:true"
```

---

## Advanced Examples

### Complex Conditional Forms

BedrockGUI supports advanced conditional logic that allows forms to dynamically change based on player conditions. Here are three comprehensive examples:

#### 1. Complex Conditional SIMPLE Form

Buttons appear or disappear based on multiple conditions:

```yaml
main_hub:
  type: "SIMPLE"
  title: "§6§l⚡ Server Hub §7(Dynamic)"
  description: "§7Welcome §e{player}§7!\n§7Balance: §a${balance}\n§7Rank: §b{rank}\n§7World: §e{world}\n\n§8Choose an option below:"
  buttons:
    # Always visible
    spawn:
      text: "§a§l🏠 Spawn"
      image: "textures/blocks/grass"
      onClick: "teleport:spawn"
    
    # VIP-only button
    vip_lounge:
      text: "§6§l👑 VIP Lounge"
      image: "textures/blocks/gold_block"
      onClick: "teleport:vip_lounge"
      show_condition: "permission:bedrockgui.vip"
    
    # Rich players only (balance >= 10000)
    premium_shop:
      text: "§e§l💎 Premium Shop"
      image: "textures/items/diamond"
      onClick: "open:premium_shop"
      show_condition: "placeholder:{balance}:>=:10000"
    
    # World-specific buttons
    survival_tools:
      text: "§2§l🔨 Survival Tools"
      image: "textures/items/iron_pickaxe"
      onClick: "open:survival_menu"
      show_condition: "placeholder:{world}:equals:survival"
    
    # Time-based button (only during day)
    daily_rewards:
      text: "§6§l🎁 Daily Rewards"
      image: "textures/items/chest"
      onClick: "conditional:placeholder:{daily_claimed}:equals:false:economy:add:1000:message:§a§l+$1000 Daily Reward!:message:§e§lAlready claimed today!"
      show_condition: "placeholder:{server_time}:>=:6:placeholder:{server_time}:<=:18"
    
    # Multiple conditions (AND logic)
    exclusive_area:
      text: "§5§l🌟 Exclusive Area"
      image: "textures/blocks/beacon"
      onClick: "teleport:exclusive"
      show_condition: "permission:bedrockgui.vip:placeholder:{level}:>=:25:placeholder:{playtime}:>=:100"
```

#### 2. Complex Conditional MODAL Form

Buttons change text and behavior based on conditions:

```yaml
confirmation_system:
  type: "MODAL"
  title: "§c§l⚠️ Dynamic Confirmation"
  description: "§7Action: §e{action_type}\n§7Target: §b{target}\n§7Cost: §a${cost}\n§7Your Balance: §e${balance}\n\n§8Confirmation changes based on your status:"
  buttons:
    confirm:
      # Button text and action change based on balance
      text: "conditional:placeholder:{balance}:>=:{cost}:§a§l✅ Confirm Purchase:§c§l❌ Insufficient Funds"
      onClick: "conditional:placeholder:{balance}:>=:{cost}:economy:remove:{cost}:server:give {player} {target} {amount}:message:§a§lPurchase successful!:message:§c§lInsufficient funds! Need ${cost}"
    
    cancel:
      # Cancel button with different messages for different user types
      text: "conditional:permission:bedrockgui.vip:§6§l👑 VIP Cancel:§7§l❌ Cancel"
      onClick: "conditional:permission:bedrockgui.vip:message:§6§lVIP cancellation - no penalty!:message:§7§lOperation cancelled."
```

#### 3. Complex Conditional CUSTOM Form

Components hide, show, or change based on multiple conditions:

```yaml
dynamic_profile:
  type: "CUSTOM"
  title: "§b§l👤 Dynamic Player Profile"
  components:
    # Basic info - always shown
    player_info:
      type: "label"
      text: "§7Player: §e{player}\n§7Rank: §b{rank}\n§7Balance: §a${balance}\n§7Level: §6{level}"
    
    # VIP-only settings section
    vip_settings_label:
      type: "label"
      text: "\n§6§l👑 VIP Settings:"
      show_condition: "permission:bedrockgui.vip"
    
    # VIP nickname input
    nickname:
      type: "input"
      text: "§6Nickname:"
      placeholder: "Enter your custom nickname..."
      default: "{nickname}"
      show_condition: "permission:bedrockgui.vip"
    
    # Rich player investment options
    investment_label:
      type: "label"
      text: "\n§e§l💰 Investment Options:"
      show_condition: "placeholder:{balance}:>=:50000"
    
    investment_amount:
      type: "input"
      text: "§eInvestment Amount:"
      placeholder: "Minimum $10,000..."
      show_condition: "placeholder:{balance}:>=:50000"
    
    # World-specific preferences
    pvp_enabled:
      type: "toggle"
      text: "§2Enable PvP"
      default: false
      show_condition: "placeholder:{world}:equals:survival"
    
    # Time-based daily goals
    daily_goal:
      type: "dropdown"
      text: "§6Today's Goal:"
      options:
        - "§aMine 100 blocks"
        - "§bKill 50 mobs"
        - "§eEarn $5,000"
      default: 0
      show_condition: "placeholder:{server_time}:>=:6:placeholder:{server_time}:<=:22"
    
    # Level-based advanced options
    particle_effect:
      type: "dropdown"
      text: "§5Particle Effect:"
      options:
        - "§7None"
        - "§eGold Sparkles"
        - "§bWater Drops"
      default: 0
      show_condition: "placeholder:{level}:>=:30"
```

### Advanced Show Conditions

#### Multiple Conditions (AND Logic)
Use multiple conditions separated by colons for AND logic:
```yaml
show_condition: "permission:bedrockgui.vip:placeholder:{level}:>=:25:placeholder:{balance}:>=:10000"
```

#### Complex Conditional Actions in Text
Even button text can be conditional:
```yaml
text: "conditional:placeholder:{balance}:>=:1000:§a§lRich Player Menu:§7§lBasic Menu"
```

#### Time-Based Conditions
```yaml
show_condition: "placeholder:{server_time}:>=:6:placeholder:{server_time}:<=:18"  # Day time only
show_condition: "placeholder:{server_time}:>=:20:placeholder:{server_time}:<=:6"   # Night time only
```

#### Event-Based Conditions
```yaml
show_condition: "placeholder:{event_active}:equals:halloween"
show_condition: "placeholder:{season}:equals:winter"
```

### Benefits of Complex Conditional Forms

#### 1. **Dynamic User Experience**
- Forms adapt to each player's unique situation
- Reduces clutter by hiding irrelevant options
- Provides personalized interfaces based on rank, balance, world, etc.

#### 2. **Progressive Disclosure**
- Advanced features unlock as players progress
- Prevents overwhelming new players with too many options
- Creates a sense of progression and achievement

#### 3. **Context-Aware Interfaces**
- Different options available in different worlds
- Time-sensitive features (daily rewards, flash sales)
- Event-specific content that appears automatically

#### 4. **Efficient Resource Management**
- Only show expensive options to players who can afford them
- Prevent unnecessary server commands for invalid actions
- Reduce support tickets from confused players

### Best Practices for Conditional Forms

#### 1. **Use Clear Condition Logic**
```yaml
# Good: Clear and specific
show_condition: "permission:shop.vip:placeholder:{balance}:>=:1000"

# Avoid: Too many complex conditions
show_condition: "permission:a:permission:b:placeholder:{x}:>=:1:placeholder:{y}:<=:10:placeholder:{z}:equals:value"
```

#### 2. **Provide Fallback Options**
```yaml
# Always provide alternatives for players who don't meet conditions
buttons:
  vip_shop:
    text: "§6§l👑 VIP Shop"
    onClick: "open:vip_shop"
    show_condition: "permission:shop.vip"
  
  regular_shop:
    text: "§7§l🛒 Regular Shop"
    onClick: "open:regular_shop"
    show_condition: "not:permission:shop.vip"
```

#### 3. **Use Conditional Text for Clarity**
```yaml
# Help players understand why options are available/unavailable
text: "conditional:placeholder:{balance}:>=:1000:§a§l💰 Rich Player Menu (${balance}):§c§l💸 Earn More Money (${balance})"
```

#### 4. **Test Edge Cases**
- What happens when a player has no permissions?
- What if placeholder values are null or unexpected?
- How does the form behave during server events?

#### 5. **Performance Considerations**
- Avoid too many complex conditions in a single form
- Cache placeholder values when possible
- Use efficient condition checking order (permissions before expensive placeholders)

### Multi-Step Wizard Form

```yaml
# Step 1: Basic Information
wizard_step1:
  type: "CUSTOM"
  title: "§e§l📋 Setup Wizard (1/3)"
  components:
    welcome:
      type: "label"
      text: "§7§lWelcome to the server setup wizard!\n§eThis will help configure your experience."
    
    username:
      type: "input"
      text: "§7Preferred Display Name:"
      placeholder: "Enter display name"
      default: "{player}"
    
    language:
      type: "dropdown"
      text: "§7Select Language:"
      options:
        - "🇺🇸 English"
        - "🇪🇸 Español"
        - "🇫🇷 Français"
        - "🇩🇪 Deutsch"
        - "🇯🇵 日本語"
      default: 0
    
    next:
      type: "button"
      text: "§a§l➡️ Next Step"
      onClick: "open:wizard_step2"

# Step 2: Preferences
wizard_step2:
  type: "CUSTOM"
  title: "§e§l📋 Setup Wizard (2/3)"
  components:
    progress:
      type: "label"
      text: "§7§lStep 2: Preferences\n§aDisplay Name: §e{username}\n§aLanguage: §e{language}"
    
    notifications:
      type: "toggle"
      text: "§7Enable Notifications:"
      default: true
    
    auto_save:
      type: "toggle"
      text: "§7Auto-save Progress:"
      default: true
    
    theme:
      type: "dropdown"
      text: "§7UI Theme:"
      options:
        - "🌞 Light Theme"
        - "🌙 Dark Theme"
        - "🌈 Colorful Theme"
      default: 0
    
    back:
      type: "button"
      text: "§c§l⬅️ Previous"
      onClick: "open:wizard_step1"
    
    next:
      type: "button"
      text: "§a§l➡️ Final Step"
      onClick: "open:wizard_step3"

# Step 3: Completion
wizard_step3:
  type: "CUSTOM"
  title: "§e§l📋 Setup Wizard (3/3)"
  components:
    summary:
      type: "label"
      text: "§7§l=== Setup Summary ===\n§aDisplay Name: §e{username}\n§aLanguage: §e{language}\n§aNotifications: §e{notifications}\n§aAuto-save: §e{auto_save}\n§aTheme: §e{theme}"
    
    terms:
      type: "toggle"
      text: "§7I agree to the server terms of service"
      default: false
    
    newsletter:
      type: "toggle"
      text: "§7Subscribe to server newsletter"
      default: false
    
    back:
      type: "button"
      text: "§c§l⬅️ Previous"
      onClick: "open:wizard_step2"
    
    complete:
      type: "button"
      text: "§a§l✅ Complete Setup"
      onClick: "conditional:{terms}:server:player setup {player} {username} {language}:message:§c§lYou must agree to the terms of service!"
```

### Dynamic Content Form

```yaml
dynamic_shop:
  type: "SIMPLE"
  title: "§2§l🛒 Dynamic Shop - {shop_category}"
  description: "§7Current deals: §e{active_deals} §7| Your balance: §a${balance}"
  buttons:
    featured_item:
      text: "§6§l⭐ Featured: {featured_item_name}"
      image: "textures/items/{featured_item_id}"
      onClick: "conditional:placeholder:{balance}:>=:{featured_item_price}:open:purchase_confirm_{featured_item_id}:message:§c§lInsufficient funds! Need ${featured_item_price}"
      show_condition: "placeholder:{featured_item_available}:equals:true"
    
    daily_deal:
      text: "§c§l🔥 Daily Deal: {daily_deal_name} (-{daily_deal_discount}%)"
      onClick: "conditional:placeholder:{daily_deal_claimed}:equals:false:economy:remove:{daily_deal_price}:message:§e§lYou already claimed today's deal!"
      show_condition: "placeholder:{daily_deal_active}:equals:true"
    
    category_weapons:
      text: "§c§l⚔️ Weapons ({weapon_count} items)"
      onClick: "open:shop_weapons"
      show_condition: "placeholder:{weapon_count}:>:0"
    
    category_armor:
      text: "§9§l🛡️ Armor ({armor_count} items)"
      onClick: "open:shop_armor"
      show_condition: "placeholder:{armor_count}:>:0"
    
    vip_section:
      text: "§d§l👑 VIP Exclusive ({vip_item_count} items)"
      onClick: "open:shop_vip"
      show_condition: "permission:shop.vip"
    
    limited_time:
      text: "§e§l⏰ Limited Time: {limited_item_name} ({limited_time_left})"
      onClick: "conditional:placeholder:{limited_time_seconds}:>:0:open:purchase_limited_{limited_item_id}:message:§c§lThis offer has expired!"
      show_condition: "placeholder:{limited_offer_active}:equals:true"
```

---

## Best Practices

### 1. Form Design
- Keep titles concise and descriptive
- Use color codes consistently
- Provide clear descriptions
- Use appropriate icons/images

### 2. Action Chaining
- Use delays between actions for better UX
- Provide feedback for every action
- Handle error cases gracefully
- Use sounds to enhance experience

### 3. Conditional Logic
- Always provide fallback actions
- Test edge cases thoroughly
- Use clear error messages
- Validate user permissions

### 4. Performance
- Avoid excessive action chains
- Use appropriate delay times
- Limit form complexity
- Cache frequently used data

### 5. User Experience
- Provide confirmation for destructive actions
- Use consistent navigation patterns
- Offer help/tutorial options
- Support different player types (new/experienced)

### 6. Security
- Validate all user inputs
- Check permissions thoroughly
- Sanitize placeholder values
- Limit resource-intensive operations

---

## Placeholder Support

BedrockGUI supports various placeholder types:

### Built-in Placeholders
- `{player}` - Player name
- `{player_world}` - Current world
- `{player_health}` - Player health
- `{player_x}`, `{player_y}`, `{player_z}` - Player coordinates
- `{server_online}` - Online player count
- `{server_max}` - Max player count
- `{balance}` - Player balance (if economy enabled)

### PlaceholderAPI Support
All PlaceholderAPI placeholders are supported when the plugin is installed:
- `%player_name%` - Player name
- `%player_world%` - Current world
- `%vault_eco_balance%` - Economy balance
- And thousands more from various plugins

**Note**: PlaceholderAPI placeholders work in all contexts including:
- Action data (messages, commands, etc.)
- Conditional action conditions and values
- Form titles, descriptions, and button text
- Show conditions for buttons and forms

### Form Result Placeholders
Results from custom forms can be used as placeholders:
- `{component_name}` - Value from form component
- `{input_field}` - Text input value
- `{toggle_state}` - Toggle true/false
- `{dropdown_selection}` - Selected dropdown option

---

This guide covers all major features of BedrockGUI. For more advanced configurations and custom implementations, refer to the source code and example configurations provided with the plugin.