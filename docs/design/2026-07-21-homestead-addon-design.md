# Homestead Addon — Design Spec

**Date:** 2026-07-21
**Status:** Approved
**Target:** New BedrockGUI addon that renders all Homestead land-claiming GUIs as native Bedrock forms.

## 1. Goal & Scope

Homestead (SpigotMC resource 121873, `tfagaming/projects/minecraft/homestead`) is a land-claiming plugin with 27 chest-inventory GUI menus. Bedrock players (via Geyser/Floodgate) get raw item grids that render badly. This addon detects Bedrock players and serves equivalent **native Bedrock forms** instead. Java players are untouched — they keep the native Homestead GUIs.

**In scope:** all 27 menus, Bedrock-native adapted (not stubbed).
**Out of scope:** any change to Homestead itself; Java-side behavior; new land-claiming features.

### The 27 menus
`GlobalPlayerFlags, MapColorMenu, MapIconMenu, MiscellaneousSettings, PlayerInfo, RegionBannedPlayers, RegionClaimedChunks, RegionInfoMenu, RegionLevels, RegionLogs, RegionMemberControlFlags, RegionMemberFlags, RegionMembersMenu, RegionMenu, RegionPlayersInvited, RegionPlayersManagement, RegionRating, RegionWorldFlags, RegionsMenu, RegionsWithWelcomeSigns, Rewards, SubAreaFlagsMenu, SubAreaMemberFlags, SubAreaMembers, SubAreaMenu, SubAreasMenu, TopRegionsMenu`

## 2. Placement & Build

- New Gradle module `homestead-addon`, added to `settings.gradle` and a `buildHomesteadAddon` task in root `build.gradle`.
- Jar: `BedrockGUI-HomesteadAddon.jar`. Package root: `it.pintux.life.homesteadaddon`.
- Version property `homesteadAddonVersion=1.0.0` and `homesteadVersion=5.2.0.0` in `gradle.properties`.
- Dependencies (all `compileOnly`): `:common`, Paper API, Floodgate (inherited), **Homestead**.
- **Homestead dependency strategy:** `compileOnly files('lib/homestead.jar')` — the (unshaded, non-premium) Homestead jar placed in `homestead-addon/lib/`. `lib/` is gitignored; the jar must be present to build. The gateway calls the Homestead API with **direct typed calls** (no reflection), so every signature is compile-checked against the real jar.
- `plugin.yml`: `depend: [BedrockGUI]`, `softdepend: [Homestead]` (addon loads but disables its features with a warning if Homestead is absent). Registers command `homesteadaddon`.

## 3. Bedrock Adaptation Rules

Bedrock forms = three primitives only: **SimpleForm** (title + content + tappable button list), **CustomForm** (input / slider / dropdown / toggle, single submit), **ModalForm** (title + content + 2 buttons). No chest grids, no item icons in a grid, no left/right/shift-click. Adaptation rules:

- **Multi-click → explicit buttons / sub-forms.** Java RegionsMenu uses left=open, right=teleport, shift-left=set target, shift-right=info. Bedrock: tap a region → `RegionMenu`, which exposes Teleport / Info / Set-Target / etc. as their own buttons.
- **Flag grids → CustomForm toggles.** `RegionWorldFlags`, `GlobalPlayerFlags`, `RegionMemberFlags`, `RegionMemberControlFlags`, `SubAreaFlagsMenu`, `SubAreaMemberFlags`: one CustomForm, a toggle per flag, initial state = current flag; on submit, diff toggles vs current and apply only the changes (single round-trip). Admin-disabled flags are shown read-only (label-marked, toggle omitted or ignored on submit). If a flag set is large, split into paged CustomForms by category.
- **Item-icon pickers → lists.** `MapColorMenu` → SimpleForm color buttons (or CustomForm dropdown); `MapIconMenu` → paginated SimpleForm / dropdown.
- **Chunk grid → chunk list.** `RegionClaimedChunks` → button per `x,z (world)`; tap → sub-form (Teleport / Unclaim-with-confirm).
- **Confirmations → ModalForm.** End Rent, Leave Region, delete sub-area, revoke invite, unban, purchase level, unclaim chunk.
- **Pagination.** Java menus paginate at 18–36 slots. Bedrock scrolls, but keep a configurable `items-per-page` (default 18) with Previous/Next buttons for very long lists, matching the essentials-addon home menu.
- **State via payload, not session.** Every menu that operates on a region/member/sub-area receives the target's **snowflake ID** through the action payload, not `TargetRegionSession`. The session target is set only immediately before a Homestead call that requires it.

## 4. Menu → Form-Type Map

| # | Menu | Form | Notes |
|---|------|------|-------|
| 1 | RegionsMenu | Simple (paged) | Root. Owned + member regions; operator "show all" toggle button. Tap → RegionMenu. |
| 2 | RegionMenu | Simple | Per-region hub. Buttons: Players Mgmt, Claimed Chunks, Flags→(Global/World), Misc Settings, Sub-Areas, Rewards, Levels, Logs, Weather/Time, End Rent(Modal), Leave(Modal), Info, Back. |
| 3 | RegionInfoMenu | Simple (read) | Stats via content placeholders + Back. |
| 4 | RegionPlayersManagement | Simple | Members / Invited / Banned / Invite-player(input). |
| 5 | RegionMembersMenu | Simple (paged) | Button per member → PlayerInfo. |
| 6 | PlayerInfo | Simple | Member detail + actions: Member Flags, Control Flags, Kick(Modal). |
| 7 | RegionMemberFlags | Custom | Toggle per member flag. |
| 8 | RegionMemberControlFlags | Custom | Toggle per control flag. |
| 9 | RegionPlayersInvited | Simple (paged) | Button per invite → Revoke(Modal). |
| 10 | RegionBannedPlayers | Simple (paged) | Button per ban → Unban(Modal); Ban-player(input). |
| 11 | RegionClaimedChunks | Simple (paged) | Button per chunk → Teleport / Unclaim(Modal). |
| 12 | RegionWorldFlags | Custom | Toggle per world flag (diff-apply). |
| 13 | GlobalPlayerFlags | Custom | Toggle per global player flag. |
| 14 | MiscellaneousSettings | Custom | Rename input, welcome msg, PvP/other toggles. |
| 15 | RegionLevels | Simple (paged) | Button per level (state-labeled) → Purchase(Modal). |
| 16 | RegionLogs | Simple (read, paged) | Log lines as content + Back. |
| 17 | RegionRating | Custom | Dropdown/slider 1–5 + current rating; submit rates. |
| 18 | RegionsWithWelcomeSigns | Simple (paged) | Button per region → Teleport. |
| 19 | SubAreasMenu | Simple (paged) | Button per sub-area → SubAreaMenu; Create(input). |
| 20 | SubAreaMenu | Simple | Members, Flags, Rename(input), Delete(Modal), Back. |
| 21 | SubAreaMembers | Simple (paged) | Button per member → SubAreaMemberFlags. |
| 22 | SubAreaMemberFlags | Custom | Toggle per sub-area member flag. |
| 23 | SubAreaFlagsMenu | Custom | Toggle per sub-area flag. |
| 24 | Rewards | Simple (paged) | Button per reward → Claim. |
| 25 | MapColorMenu | Simple | Color buttons / dropdown; sets region map color. |
| 26 | MapIconMenu | Simple (paged) | Icon buttons / dropdown; sets region map icon. |
| 27 | TopRegionsMenu | Simple (read) | Leaderboard content; buttons switch sort (bank/chunks/members/rating). |

## 5. Architecture

Mirrors `essentials-addon`. Package layout under `it.pintux.life.homesteadaddon`:

- **`HomesteadAddonPlugin`** (`extends JavaPlugin`) — on enable: verify BedrockGUI API + Floodgate + Homestead present; load config; build gateway; register services, command, listeners; wire modules. On reload: re-wire without restart (PlugMan-safe, per prior essentials fix). On disable: clean shutdown.
- **`gateway/HomesteadGateway`** (interface) + **`HomesteadGatewayImpl`** (typed impl) — single-provider facade over Homestead. Calls its static managers (`RegionManager`, `MemberManager`, `BanManager`, `InviteManager`, `LevelManager`, `LogManager`, `RateManager`, `SubAreaManager`, `ChunkManager`) + models (`Region`, `RegionMember`, `RegionBan`, `RegionInvite`, `RegionLog`, `RegionRate`, `RegionChunk`, `SubArea`, `Level`) + `WorldFlags`/`PlayerFlags`/`ControlFlags`. Read methods return **view DTOs**; write methods perform Homestead mutations. `isAvailable()` (checks the Homestead plugin is loaded) gates every data method in the services, so a missing Homestead never reaches a typed call.
- **`api/BedrockPlayerDetector`** + **`service/FloodgateBedrockPlayerDetector`** — reused pattern from essentials-addon.
- **`model/`** — view DTOs: `RegionView, MemberView, FlagView, SubAreaView, LevelView, LogView, BanView, InviteView, ChunkView, RatingView, RewardView`. Immutable snapshots for rendering.
- **`service/`** — ~8 `Bedrock*Service` classes, each building forms via `BedrockGUIApi` and grouping related menus:
  - `BedrockRegionService` → RegionsMenu, RegionMenu, RegionInfoMenu, TopRegionsMenu, RegionsWithWelcomeSigns
  - `BedrockMemberService` → RegionPlayersManagement, RegionMembersMenu, PlayerInfo, RegionPlayersInvited, RegionBannedPlayers
  - `BedrockFlagService` → RegionWorldFlags, GlobalPlayerFlags, RegionMemberFlags, RegionMemberControlFlags
  - `BedrockSubAreaService` → SubAreasMenu, SubAreaMenu, SubAreaMembers, SubAreaMemberFlags, SubAreaFlagsMenu
  - `BedrockChunkService` → RegionClaimedChunks, MapColorMenu, MapIconMenu
  - `BedrockLevelService` → RegionLevels, Rewards
  - `BedrockLogService` → RegionLogs
  - `BedrockMiscService` → MiscellaneousSettings, RegionRating
- **`action/`** — Action classes registered on the BedrockGUI `ActionRegistry` for **navigation** so `/homesteadaddon open <menu>` and reload-safety work: e.g. `hs_regions`, `hs_region_menu:<id>`, `hs_members:<id>`, `hs_flags_world:<id>`, `hs_subareas:<id>`, `hs_chunks:<id>:<page>`, etc. Terminal mutations (toggle apply, teleport, claim) use inline lambdas inside services.
- **`config/HomesteadAddonConfiguration`** + `resources/config.yml` — every title / button / content / message, per-menu enable toggles, `items-per-page`, i18n placeholders. `config/ConfigMigrator` for version bumps. `render(template, placeholders)` helper.
- **`command/HomesteadAddonCommand`** — `/homesteadaddon` (alias `/hsaddon`): `reload | open <menu> [regionId] | openfor <player> <menu> [regionId]`. Perm `homesteadaddon.admin` for open/openfor, `homesteadaddon.reload` for reload.
- **`listener/HomesteadCommandListener`** — intercept `/region` (+ aliases `rg`, `hs`, `homestead`) GUI-opening invocation and `/homesteadadmin` (+ `hsadmin`) for **Bedrock players only** → serve the form instead of the chest GUI. PlugMan-reload-safe registration. Exact intercepted sub-invocation confirmed against Homestead's `commands/standard` classes during implementation.
- **`util/`** — `BukkitFormPlayer`, `FormPlayerResolver`, `HomesteadActionPayloads` (snowflake-id + arg codec), `BedrockSoundFeedback`.

## 6. Navigation, State, Threading

- **Identity:** regions/members/sub-areas addressed by Homestead **snowflake ID** carried in payloads. Back buttons carry parent menu + id.
- **Threading:** Homestead managers are in-memory (RegionsMenu reads synchronously in the Java plugin), so forms build and mutations run on the main server thread. No async fetch needed; if any manager call proves I/O-bound during implementation, fetch async → open form on main thread.
- **Session:** set `TargetRegionSession` only when a Homestead call requires the currently-targeted region; otherwise pass ids explicitly.

## 7. Permissions & Errors

- Delegate all authorization to Homestead via the gateway: owner / member checks, control-flags, admin-disabled flags. Also honor `homestead.commands.region` for the intercept.
- Configured, localized error messages: `homestead-unavailable`, `region-not-found`, `no-permission`, `member-not-found`, `sub-area-not-found`, generic `action-failed`. Match essentials-addon message style.

## 8. Testing

- **Unit:** `HomesteadActionPayloads` codec (round-trip), config rendering / placeholder substitution, `HomesteadReflection` accessors (Mockito), flag diff computation.
- **Manual matrix (Geyser):** every menu opened via `/region` intercept and via `/homesteadaddon open`, each navigation edge, each mutation (toggle flag, add/remove member, ban/unban, invite/revoke, create/delete sub-area, purchase level, claim reward, rate, set map color/icon, teleport, unclaim, leave, end rent), permission-denied and region-not-found paths.

## 9. Implementation Phases

- **P0 — Foundation:** module + build wiring + local Homestead jar compiling; config + configuration class; `HomesteadGateway` (+ reflection + tests); Floodgate detector; `HomesteadAddonPlugin`; command; command listener skeleton. First vertical slice: **RegionsMenu → RegionMenu → RegionInfoMenu** working end-to-end via intercept and dedicated command.
- **P1 — Members:** RegionPlayersManagement, RegionMembersMenu, PlayerInfo, RegionPlayersInvited, RegionBannedPlayers (+ invite/ban/kick/unban/revoke mutations).
- **P2 — Flags:** RegionWorldFlags, GlobalPlayerFlags, RegionMemberFlags, RegionMemberControlFlags (CustomForm toggle diff-apply).
- **P3 — Sub-areas:** SubAreasMenu, SubAreaMenu, SubAreaMembers, SubAreaMemberFlags, SubAreaFlagsMenu (+ create/rename/delete).
- **P4 — Economy/meta:** RegionLevels, Rewards, RegionLogs, RegionRating, MiscellaneousSettings.
- **P5 — Map/leaderboard:** RegionClaimedChunks, MapColorMenu, MapIconMenu, TopRegionsMenu, RegionsWithWelcomeSigns.

Each phase: build forms + wire actions + config keys + manual verify before the next.

## 10. Risks

- **Homestead API surface** — resolved: the gateway compiles against the real `homestead.jar`, so every call is type-checked. All 27 menus map to public API.
- **Missing Homestead at runtime** — `isAvailable()` gates every data path; forms report "Homestead unavailable" instead of throwing.
- **Flag volume** — many flags in one CustomForm; mitigation: category paging if needed.
- **Command intercept specificity** — intercepts only the GUI-opening invocation of `/region`, not `/claim`/`/unclaim` sub-actions.
