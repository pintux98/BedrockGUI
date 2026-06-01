# BedrockGUI Bedwars Addon — Design

**Date:** 2026-06-01
**Status:** Approved (architecture + module layout); implementation sequencing in plan.

## Goal

A new BedrockGUI addon — `bedwars-addon` — that gives Bedrock (Geyser/Floodgate) players
native Cumulus forms in place of BedWars2023's chest-based GUIs. Built the same way as
`essentials-addon`: a separate Gradle module depending on `:common`, the Paper API, and the
target plugin's API (compile-only). First (and currently only) integration target is
**BedWars2023** by tomkeuper, behind a provider abstraction so other Bedwars plugins
(MBedwars, BedWars1058, …) can be added later without touching the form/service/listener code.

Java players are unaffected — they keep the native chest GUIs. Only Bedrock players are
intercepted and shown forms.

## Scope (all modules; sequenced in the plan)

1. **In-game item shop** — intercept the shop villager GUI, show a flat category list form,
   purchase through the API.
2. **Team upgrades** — same intercept/live-state machinery as the shop.
3. **Arena selector / join** — lobby-side; intercept the arena-selector GUI and/or join command.
4. **Stats / spectator / party / cosmetics** — secondary menus.

Shop layout decision: **flat category list** — open straight to one button per category; each
opens a sub-form of its items (no quick-buy first screen).

## Key difference from essentials-addon

Essentials menus fire from a lobby hub via **command interception**. Bedwars' highest-value
menus (shop, upgrades) open **mid-match** when a player interacts with an NPC, and their
contents are **live** (player money, owned tiers, team upgrade levels change constantly).

Two consequences for the design:

- A new **menu-interception layer** (not present in essentials) recognizes a BedWars2023 GUI,
  cancels it for Bedrock players, and dispatches to the matching service.
- Forms are built **live per-open** from the provider, not from a cached static catalog. The
  `*CatalogService` pattern still applies to **static structure** (category/content definitions,
  arena list); **live state** (cost, owned, affordable, tier, money, stats) is read at open time.

## Architecture (Approach A)

Mirror the `essentials-addon` structure and add the interception layer.

```
bedwars-addon/                              (new gradle module, parallel to essentials-addon)
  build.gradle                              compileOnly :common, paper-api, BedWars2023-API (jitpack)
  src/main/resources/
    plugin.yml                              softdepend: [BedWars2023, floodgate]; command /bedwarsaddon
    config.yml                              module toggles + per-form text/buttons + sounds
  src/main/java/it/pintux/life/bedwarsaddon/
    BedrockBedwarsAddonPlugin.java          wires modules, picks providers, registers actions+listeners
    api/                                    provider interfaces + BedrockPlayerDetector
    provider/                               BedWars2023* impls of each interface (only place touching com.tomkeuper.*)
    model/                                  DTO records (ShopCategory, ShopContent, ArenaInfo, ...)
    service/                                *CatalogService (static) + Bedrock*Service (forms, live state)
    action/                                 *Action handlers (open/navigate/buy/upgrade/join)
    listener/                               MenuInterceptListener (InventoryOpenEvent) + command listeners
    config/                                 BedwarsAddonConfiguration
```

**Gradle wiring:** `settings.gradle` adds `include 'bedwars-addon'`; `gradle.properties` adds
`bedwarsAddonVersion` + `bedWars2023ApiVersion`; root `build.gradle` adds a `buildBedwarsAddon`
task; shaded jar named `BedrockGUI-BedwarsAddon`. jitpack repo is already declared.

**Package** `it.pintux.life.bedwarsaddon`, command `/bedwarsaddon reload` mirroring
`EssentialsAddonCommand`.

### API entry

Resolve `com.tomkeuper.bedwars.api.BedWars` once via
`Bukkit.getServicesManager().getRegistration(BedWars.class).getProvider()`, held inside the
providers. No other class touches BedWars2023 types. Nested utils used:
`ArenaUtil`, `ShopUtil`, `TeamUpgradesUtil`, `ItemUtil`, `ScoreboardUtil`.
`BedrockGUIApi` obtained as essentials does (`getInstance()`, fail-soft if BedrockGUI-Paper
hasn't loaded yet — `getApiSafely`).

Confirmed relevant API surface (BedWars2023 javadocs):
`IShopIndex` → `IShopCategory` → `ICategoryContent`, with `ICategoryContent.execute(player)`
performing the actual purchase (cost check + give + deduct); `IPlayerQuickBuyCache`;
`IArena` + `ArenaUtil` (lookup by player, join); `GameState`; `ArenaEnableEvent`;
`TeamUpgradesUtil` + `UpgradesIndex`; `IStatsManager` + `PlayerStatChangeEvent`;
`Party`; `ArenaSpectateEvent`.

## Provider interfaces

DTO-based — return plain addon records so `service/`, `action/`, `listener/` never import a
`com.tomkeuper.*` type. Only `provider/BedWars2023*` impls do.

```java
interface ShopProvider {
    String getProviderId();
    boolean isReady();
    List<ShopCategory> getCategories(Player p);                    // static structure
    List<ShopContent> getCategoryContents(Player p, String catId); // live: cost, owned, affordable, tier
    PurchaseResult purchase(Player p, String contentId);           // wraps ICategoryContent.execute(p)
    boolean ownsInventory(Inventory inv);                          // interception seam
}

interface ArenaProvider {
    List<ArenaInfo> getJoinableArenas();    // name, group, GameState, players, max, status
    boolean join(Player p, String arena);
    ArenaInfo getArenaOf(Player p);
    boolean ownsInventory(Inventory inv);
}

interface UpgradeProvider {                  // team upgrades, live per-team tier state
    List<UpgradeCategory> getCategories(Player p);
    List<UpgradeContent> getContents(Player p, String catId);      // cost, current tier, maxed, affordable
    PurchaseResult purchase(Player p, String contentId);
    boolean ownsInventory(Inventory inv);
}

interface StatsProvider     { boolean isReady(); PlayerStats getStats(Player p); }
interface PartyProvider     { PartyInfo getParty(Player p); boolean invite/leave/kick/disband(...); }
interface SpectatorProvider { List<SpectateTarget> targets(Player p); boolean teleport(Player p, UUID t); }

interface BedrockPlayerDetector { boolean isBedrockPlayer(Player p); }  // Floodgate impl, reused pattern
```

- **`ownsInventory(Inventory)`** is the seam keeping the interception layer provider-agnostic —
  each provider knows how BedWars2023 marks its own GUIs (inventory holder type or title).
- **DTOs** in `model/`: `ShopCategory{id,name,icon,slot}`, `ShopContent{id,name,cost,currency,
  owned,affordable,tier}`, `ArenaInfo{name,group,state,players,max,status}`,
  `UpgradeCategory`, `UpgradeContent{id,name,cost,currentTier,maxed,affordable}`,
  `PlayerStats`, `PartyInfo`, `SpectateTarget`. `PurchaseResult{success,reason}` for clean
  Bedrock feedback without parsing exceptions.
- **Provider selection:** `registerProvider(map, "BedWars2023", BedWars2023ShopProvider::new)` +
  `pickProvider` auto-pick by plugin presence, exactly as essentials. One bedwars plugin active
  at a time → one provider per feature.

## Interception layer & live-state flow

`MenuInterceptListener implements Listener`, `@EventHandler(priority = HIGH)` on
`InventoryOpenEvent`:

1. `if (!detector.isBedrockPlayer(player)) return;` — Java players keep the native chest GUI.
2. For each active provider, check `ownsInventory(event.getInventory())`. First match wins.
3. `event.setCancelled(true)`; dispatch to that feature's `Bedrock*Service.open(player)` on the
   next tick (`scheduler.runTask`) so the cancel settles before the form is sent.

`InventoryOpenEvent` is chosen because it catches the shop/upgrade GUI regardless of how it was
triggered (NPC click, command, or item).

**Spike risk (first plan task):** if BedWars2023 opens a non-Bukkit / custom GUI that doesn't
raise `InventoryOpenEvent`, the fallback is to intercept the trigger —
`PlayerInteractEntityEvent` on the shop NPC, or a native BedWars2023 shop-open event if one
exists. The plan's first task verifies which mechanism actually fires and the provider exposes
the working detection behind `ownsInventory`/a trigger hook.

**Threading:** forms are sent on the main thread; all BedWars2023 calls run on the main thread
and are guarded by `isReady()`.

**Live refresh:** after a successful purchase the service re-opens (re-renders) the same form so
money/tier/owned reflect the new state — same re-open trick as essentials home-delete.

## Form & action flow (per module)

Reuses the `essentials-addon` action-string + `ActionContext` + `BukkitFormPlayer` pattern verbatim.
Actions are registered with `BedrockGUIApi.registerActionHandler` and invoked via
`api.executeActionString(formPlayer, "action:payload", context)`.

- **Shop:** `bw_shop_main` → flat category `SimpleForm` (one button per `ShopCategory` + close) →
  `bw_shop_cat:<catId>` → content `SimpleForm` (button text = name + cost, marked
  affordable/owned) → `bw_shop_buy:<contentId>` → `provider.purchase()` → sound feedback →
  re-open the category form.
- **Upgrades:** identical shape (`bw_upgrade_main` / `bw_upgrade_cat` / `bw_upgrade_buy`).
- **Arena selector:** `bw_arena_main` → arena `SimpleForm` (name + `GameState` + players/max) →
  `bw_arena_join:<arena>` → `provider.join()`. Triggered by intercepting the arena-selector GUI
  and/or a join-command listener / lobby item.
- **Stats:** `bw_stats` → read-only form (content lines from `PlayerStats`).
- **Party:** `SimpleForm` of actions; invite via `CustomForm` input; leave/kick/disband.
- **Spectator:** `SimpleForm` target list → teleport; settings via `CustomForm` toggles.

Long lists paginate at a fixed page size with prev/next buttons (as essentials home/warp menus).

## Config & error handling

`BedwarsAddonConfiguration` mirrors `EssentialsAddonConfiguration`:

- Module toggles: `moduleShop`, `moduleUpgrades`, `moduleArena`, `moduleStats`, `moduleParty`,
  `moduleSpectator` (and `actions*` equivalents if actions should register independently of
  interception, matching essentials).
- Per-form titles / button text / messages; `render(template, Map)` helper.
- Sound feedback (reuse the essentials `BedrockSoundFeedback` approach: form-open,
  purchase-success, purchase-failed, volume, pitch).

Fail-soft everywhere:

- No module enabled → log a hint and return (as essentials does).
- Provider not ready / plugin absent → user message, skip that module.
- `BedrockGUIApi` not yet loaded → defer/warn (`getApiSafely`).
- Purchase failure → `PurchaseResult.reason` → message + fail sound.
- `/bedwarsaddon reload` rebuilds config + services (mirrors essentials `reloadConfiguration`).

## Testing

TDD, following the existing service/provider split:

- **Unit (no Bukkit / no BedWars2023):** services tested against **mock providers** — interfaces
  make this trivial. Cover DTO → form-button assembly, pagination, `PurchaseResult` feedback
  routing, `ActionContext` shape, and module enable/disable gating.
- **Provider impls kept dumb** (pure API↔DTO translation) so the testable logic lives in services.
- **Manual verification matrix** on a live server with BedWars2023 + Geyser/Floodgate:
  Bedrock player — shop intercept, purchase deducts currency, upgrade raises tier, arena join,
  stats render; Java player — native chest GUIs unchanged; `/bedwarsaddon reload` works;
  module toggles honored.

## Out of scope (this spec)

- Other Bedwars plugins' provider impls (interfaces are built abstraction-ready; impls later).
- Cosmetics deep integration beyond a basic menu (folded into the stats/secondary group; revisit
  once the core modules land).
