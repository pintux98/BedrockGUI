# BedrockGUI Plugin Guide

## Overview
- Provides Bedrock-style forms on Paper and Velocity; integrates with Floodgate for Bedrock detection
- Unified action system with modern curly-brace syntax and placeholder support
- Supports multi-action sequences, delays, random selection, and conditional logic
- YAML-driven menus plus programmatic builders for dynamic forms
- Admin command to reload and open forms

## Action System
- Syntax
  - Use: `action_type { - "value1" - "value2" }`
- Placeholders: `{player}`, `{player_level}`, `${vault_eco_balance}` and values derived from form results

### Actions

#### command
- Executes commands as the player
- Examples
  - ```
    command {
      - "me hello world"
      - "time query daytime"
    }
    ```

#### server
- Executes commands as the server console
- Examples
  - ```
    server {
      - "say {player} ran tests"
      - "time set day"
    }
    ```

#### broadcast
- Broadcasts messages globally or to targeted audiences (permission/world/radius)
- Examples
  - ```
    broadcast {
      - "Welcome everyone!"
      - "permission:vip.access:VIP notice"
      - "world:world_nether:Nether alert"
      - "radius:20:Nearby message"
    }
    ```

#### message
- Sends colorized chat messages; supports `&` and hex colors
- Examples
  - ```
    message {
      - "&#00ff99Welcome {player}!"
      - "&aBalance: ${vault_eco_balance}"
    }
    ```

#### sound
- Plays sounds with optional volume and pitch
- Examples
  - ```
    sound {
      - "ui.button.click"
      - "entity.experience_orb.pickup:0.8:1.2"
    }
    ```

#### economy
- Balance operations: `add`, `remove`, `set`, `check`, `pay`
- Examples
  - ```
    economy {
      - "check:500"
      - "add:250"
      - "pay:Friend:100"
    }
    ```

#### title
- Shows title/subtitle with timing `fadeIn:stay:fadeOut`
- Examples
  - ```
    title {
      - "Welcome!:Enjoy your stay:10:60:10"
    }
    ```

#### actionbar
- Shows short messages above the hotbar
- Examples
  - ```
    actionbar {
      - "&eLoading..."
      - "&aDone!"
    }
    ```

#### inventory
- Item operations: `give:item:amount`, `remove:item:amount`, `clear:all|item`, `check:item`
- Examples
  - ```
    inventory {
      - "give:diamond:3"
      - "check:diamond"
      - "clear:all"
    }
    ```

#### open
- Opens another form/menu
- Examples
  - ```
    open {
      - "main_hub"
      - "advanced_test_menu"
    }
    ```

#### delay
- Waits for milliseconds, then optionally runs a chained action
- Examples
  - ```
    delay {
      - "1000"
      - "message:After 1 second"
    }
    ```

#### random
- Selects and executes one action from a list; last colon may be weight
- Examples
  - ```
    random {
      - "message:Prize A:2.0"
      - "message:Prize B:1.0"
    }
    ```

#### url
- Sends HTTP(S) links to the player’s chat (clients render clickable)
- Examples
  - ```
    url {
      - "https://store.server.com/{player}"
    }
    ```

#### conditional
- Executes actions based on permission/placeholder checks
- Examples
  - ```
    conditional {
      check: "placeholder:${vault_eco_balance} >= 500"
      true:
        - "message:&aYou can afford it!"
        - "economy:add:100"
      false:
        - "message:&cNeed $500"
    }
    ```

## Form Types

### Simple Form
- Title, optional content, multiple buttons
- Configuration
  - ```yaml
    forms:
      main_hub:
        type: "SIMPLE"
        title: "§a§lMain Hub"
        content: "Welcome, {player}!"
        buttons:
          open_shop:
            text: "Shop"
            onClick:
              - |
                open {
                  - "shop_menu"
                }
          ping:
            text: "Ping"
            onClick:
              - |
                message {
                  - "&aPong!"
                }
    ```

### Modal Form
- Two buttons (Yes/No-style), optional content
- Configuration
  - ```yaml
    forms:
      confirm_buy:
        type: "MODAL"
        title: "Confirm Purchase"
        content: "Buy the item for $250?"
        buttons:
          yes:
            text: "Yes"
            onClick:
              - |
                economy {
                  - "check:250"
                  - "remove:250"
                }
          no:
            text: "No"
            onClick:
              - |
                message {
                  - "&eCancelled"
                }
    ```

### Custom Form
- Structured inputs and interactive components; optional global actions
- Components
  - `input`: `text`, `placeholder`, `default`, optional `action`
  - `slider`: `text`, `min`, `max`, `step`, `default`, optional `action`
  - `dropdown`: `text`, `options`, `default`, optional `action`
  - `toggle`: `text`, `default`, optional `action`
- Configuration
  - ```yaml
    forms:
      feedback_form:
        type: "CUSTOM"
        title: "Feedback"
        components:
          name:
            type: "input"
            text: "Your name"
            placeholder: "Steve"
            default: ""
            action: |
              message {
                - "Thanks, $name!"
              }
          rating:
            type: "slider"
            text: "Rate 1–10"
            min: 1
            max: 10
            step: 1
            default: 5
          category:
            type: "dropdown"
            text: "Category"
            options: [ "Bug", "Suggestion", "Other" ]
            default: 0
          anonymous:
            type: "toggle"
            text: "Send anonymously?"
            default: false
        global_actions:
          - |
            message {
              - "Submitted by $name"
            }
          - |
            message {
              - "Category $category, Rating $rating, Anonymous $anonymous"
            }
    ```

### Conditional Buttons (Simple/Modal)
- Show/hide or change text/image/onClick based on conditions
- Keys: `show_condition`, `alternative_text`, `alternative_image`, `alternative_onClick`, `conditions.*`
- Evaluated when the form is built

## Tips
- Use placeholders to personalize messages and actions (e.g., `{player}`, `${vault_eco_balance}`)
- Combine actions for guided flows using multiple list items under a button’s `onClick`

## Menu Configuration Quick Reference
- Under `forms.<menu_name>`
  - `type`: `SIMPLE | MODAL | CUSTOM`
  - `title`: text (color codes supported)
  - `content`: optional (Simple/Modal)
  - `permission`: optional required permission
  - `buttons`: for Simple/Modal (with `text`, optional `image`, `onClick` list)
  - `components`: for Custom (with fields per type)
  - `global_actions`: actions run after Custom submit
- Command binding example in `config.yml`
  - ```yaml
    commands:
      gui:
        description: "Open the main BedrockGUI hub"
        permission: "bedrockgui.use"
        aliases: [ "menu", "hub", "main" ]
        form: "main_hub"
    ```

## Placeholders
- Built-ins: `{player}`, `{player_level}`, `{player_gamemode}`, `{player_world}`, `${vault_eco_balance}`
- Applied inside action handlers before execution; injected into `ActionContext`

## Admin Command
- Paper: `/bedrockgui` with `bedrockgui.admin` permission
- Velocity: `/bedrockgui reload`, `/bedrockgui open <menu> [player]`

## Composition Examples
- Guided sequence
  - ```
    message { - "&eStarting..." }
    sound { - "ui.button.click" }
    delay { - "1000" - "message:Step 2" }
    random { - "title:Prize A" - "title:Prize B" }
    ```
- Server operations
  - ```
    server { - "tp {player} spawn" - "gamemode survival {player}" }
    ```