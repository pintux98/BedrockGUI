# EssentialsAddon: Potion Display Names + MyPet Integration — Design

Date: 2026-06-13
Module: `essentials-addon`
Target: Paper API 1.20.1, Java 21

Two independent deliverables:

1. **Potion display names** — bug fix in shop item name resolution.
2. **MyPet integration** — new optional module (`/petshop`, `/pet`, `/pcst` Bedrock forms).

---

## Problem 1 — Potion display names

### Root cause

`ShopGuiReflectionSupport.displayName(ItemStack)` returns `prettify(itemStack.getType().name())` when the item has no custom display name. For potion materials this yields the bare material — `POTION` → "Potion", `SPLASH_POTION` → "Splash Potion", `LINGERING_POTION` → "Lingering Potion", `TIPPED_ARROW` → "Tipped Arrow" — dropping the effect. Both `ShopGuiCatalogService.toView` and `EconomyShopCatalogService.toView` route through this single method, so one fix covers ShopGUI+ and EconomyShopGUI.

### Fix

In `displayName()`, after the existing `itemMeta.hasDisplayName()` check and before the generic `prettify` fallback, add a potion branch:

- If `itemMeta instanceof PotionMeta potionMeta`, build the name from `potionMeta.getBasePotionData()` (`PotionData`). The combined `getBasePotionType()` only exists on 1.20.5+; compiling against 1.20.1 requires the old `getBasePotionData()`/`PotionData` API, which remains present (deprecated) on newer runtimes, so it is portable.
- Name format:
  - **prefix** from `itemStack.getType()`: `POTION` → "Potion", `SPLASH_POTION` → "Splash Potion", `LINGERING_POTION` → "Lingering Potion", `TIPPED_ARROW` → "Tipped Arrow".
  - **effect** from `PotionData.getType()` (`PotionType`), rendered with the existing `prettify(...)` helper on the enum name (decision: prettified enum names, no vanilla mapping table). E.g. `SPEED` → "Speed", `INSTANT_HEAL` → "Instant Heal".
  - Append `" II"` when `potionData.isUpgraded()`, `" (Extended)"` when `potionData.isExtended()`.
  - Compose as `"<prefix> of <effect>"`, e.g. `"Splash Potion of Speed II"`, `"Potion of Instant Heal"`.
  - Effect-less base types (`WATER`, `MUNDANE`, `THICK`, `AWKWARD`, and any whose `prettify` yields nothing meaningful): return just the prefix (e.g. "Potion") rather than "Potion of Water". Treat `WATER`/`MUNDANE`/`THICK`/`AWKWARD` as no-effect.

### Isolation & safety

- New private helper `potionDisplayName(ItemStack, PotionMeta)` in `ShopGuiReflectionSupport`. No signature changes, no new files.
- Entire potion branch wrapped in `try/catch (Throwable)`; on any failure fall through to the current `prettify(type.name())` behavior. The fix can never produce a worse result than today.
- Custom-named potions (shop config sets a display name) are unaffected — handled by the earlier `hasDisplayName()` return.

### Testing

- Unit-level: a small test constructing `ItemStack` of each potion material with `PotionData` variants (normal / upgraded / extended / no-effect / water) and asserting the rendered string. Mockito is already a dependency. If constructing real `PotionMeta` is impractical without a server, test the pure name-composition logic by extracting it to a package-visible method taking (`materialName`, `potionTypeName`, `upgraded`, `extended`).

---

## Problem 2 — MyPet integration

New optional module `mypet`, mirroring the existing Warp/Kit/Home/Tpa/Shop architecture. Single provider (only MyPet supplies pets); the provider interface is retained for consistency and graceful no-op when MyPet is absent.

### Scope

Three Bedrock form entry points, each replacing a MyPet Java GUI / command for Floodgate players:

- **`/petshop`** — SimpleForm listing buyable shop pets (name + price, `(Owned)` marker), click → confirm modal → buy.
- **`/pet`** (new command) — SimpleForm listing the player's pets (active one marked), click → pet modal.
- **`/pcst`** — SimpleForm listing skilltrees available to the active pet, current marked, click → switch.

Java players: all three pass through to MyPet untouched. Forms fire only for Bedrock players (`BedrockPlayerDetector`).

### MyPet API anchor points (researched, v3.14.1 + dev branch, group `de.keyle`)

- Entry: `de.Keyle.MyPet.MyPetApi` (static accessors — note capital `K`).
- `getPlayerManager().getMyPetPlayer(Player)` → `MyPetPlayer`.
- Player's full pet list (active + stored), **async**: `getRepository().getMyPets(MyPetPlayer, RepositoryCallback<List<StoredMyPet>>)`.
- Active pet: `getMyPetManager().getMyPet(Player)` → `MyPet` (extends `StoredMyPet`); `hasActiveMyPet(Player)`.
- Pet info fields — `StoredMyPet`: `getPetName()`, `getPetType()` (`MyPetType`), `getExp()`, `getHealth()`, `getSaturation()`, `getSkilltree()` (`Skilltree`, `.getName()`/`.getDisplayName()`). Active `MyPet` additionally: `getExperience().getLevel()`, `getMaxHealth()`, `getStatus()` (`PetState`).
- **Call (activate stored pet):** `getMyPetManager().activateMyPet(StoredMyPet)` → `Optional<MyPet>`, then `owner.setMyPetForWorldGroup(worldGroup, pet.getUUID())` and `pet.createEntity()` (the `/petswitch` path). `createEntity()`/spawn must run on the **main thread**.
- **Put away (active pet):** `myPet.removePet(false)` (despawn; stays active) or `getMyPetManager().deactivateMyPet(owner, true)` (full deactivate). Put-away uses `removePet(false)`.
- **Skilltrees:** `getSkilltreeManager().getSkilltrees()` / `getOrderedSkilltrees()`; available to a pet = filter by `tree.getMobTypes().contains(petType)` + `tree.checkRequirements(myPet)` + level gate (`myPet.getExperience().getLevel()` within `[getRequiredLevel(), getMaxLevel()]`). Switch: `myPet.setSkilltree(tree, MyPetSelectSkilltreeEvent.Source.PlayerCommand)`. A switch fee may apply (handled inside MyPet).
- **Shop:** `getServiceManager().getService(ShopManager.class)`; config `pet-shops.yml` → `Shops.<id>.Pets.<petId>` with `Price`, `PetType`, `Skilltree`, `EXP`, `Name`, `Description`, `Icon`. `ShopMyPet` getters: `getPrice()`, `getPetType()`, `getSkilltree()`, `getPetName()`, etc. Purchase = `HookHelper` Vault economy `canPay`/`withdrawPlayer` → `getInactiveMyPetFromMyPet(template)` → `setOwner` → `Repository.addMyPet` → activate if player has no active pet.

### Components

New package members under `it.pintux.life.essentialsaddon`:

- `api/PetProvider` — interface:
  - `String getProviderId()`, `boolean isReady()`
  - `void listOwnedPets(Player, Consumer<List<PetView>>)` (async)
  - `boolean isActive(Player, petUuid)`
  - `boolean call(Player, petUuid)` / `boolean putAway(Player)`
  - `List<SkilltreeView> listSkilltrees(Player)` (for the active pet), `boolean setSkilltree(Player, treeName)`
  - `List<ShopPetView> listShopEntries(Player)`, `boolean ownsType(Player, petType)`, `BuyResult buyShopEntry(Player, shopId, petId)`
- `provider/MyPetProvider` — concrete impl over `MyPetApi`. Constructed only when plugin `MyPet` is present **and** API classes load (guard mirrors `isEconomyShopApiAvailable`). Translates between MyPet types and the addon's plain view records.
- `model/PetView`, `model/SkilltreeView`, `model/ShopPetView` — immutable view records (uuid, name, type, level, hp/maxHp, hunger, skilltree name, active flag / price / owned flag). Keeps MyPet types out of the service + form layer.
- `service/PetCatalogService` — holds the provider; caches the shop catalog (refreshable like other catalog services); pet lists fetched live (async) per request.
- `service/BedrockPetService` — builds and sends the forms (see Form flows). Owns the call/put-away/buy/skilltree orchestration, message + sound feedback. Main-thread hops for spawn via `Bukkit.getScheduler().runTask`.
- `listener/PetCommandListener` (`PlayerCommandPreprocessEvent`) — for Bedrock players, cancel and open the matching form on `/petshop`, `/pet`, `/pets`, `/pcst`. (`PlayerCommandPreprocessEvent` fires even for unregistered commands, so interception works regardless of registration.)
- `action/*` — BedrockGUI `ActionSystem` handlers for Java-side menu parity: `OpenPetShopAction`, `OpenPetListAction`, `PetCallAction`, `PetSendAwayAction`, `OpenPetSkilltreeAction`, `PetSetSkilltreeAction`, `BuyPetShopAction`, plus a `HubPetAction`. Payload encoding via a new `PetActionPayloads` util (mirrors `EssentialsActionPayloads`).

### Form flows

- **`/petshop`** (`openPetShop`): `listShopEntries(player)`. Empty → configured message. Else SimpleForm; each button text = `render(ui.pet-shop-button, {pet_name, price, owned})` where owned entries show an `(Owned)` suffix. Click → **confirm ModalForm** `render(ui.pet-buy-confirm, {pet_name, price})` with Yes/No. Yes → `buyShopEntry`; on success/failure send message + sound. "Owned" = `provider.ownsType(player, petType)` (player already owns any pet of that `MyPetType`; no separate persistence).
- **`/pet`** (`openPetList`): `listOwnedPets(player, ...)` (async; build + send form inside the callback, on main thread). Empty → configured message. Else SimpleForm; button per pet, active pet marked via `ui.pet-active-suffix`. Click → `openPetModal`.
- **pet modal** (`openPetModal`): ModalForm. Content = `render(ui.pet-info, {name, type, level, hp, max_hp, hunger, skilltree})` (level/maxHp only reliably available for the active pet; stored pets show available fields, level falls back to `—`). `button1` = **Put Away** if this pet is the active+spawned pet, else **Call**. `button2` = **Skilltree** → `openSkilltreeForm`. `onSubmit(true=button1)`: call/put-away; `false=button2`: open skilltree form.
- **`/pcst`** (`openSkilltreeForm`): requires an active pet (else message). `listSkilltrees(player)` filtered to those valid for the active pet; current marked via `ui.pet-skilltree-current-suffix`. Click → `setSkilltree`; message + sound on result.

### Wiring edits

- `BedrockEssentialsAddonPlugin`:
  - Add fields `petCatalogService`, `bedrockPetService`; null them in `onDisable`.
  - Add `mypet` to the `anyModule` check.
  - `initMyPet(pluginManager)`: API-availability guard → build provider, `PetCatalogService`, `BedrockPetService`; register `PetCommandListener` when `moduleMyPet()`.
  - Refresh `petCatalogService` in the deferred `runTask` block and in `/essentialsaddon reload`.
  - `registerActions`: register the pet actions + hub button under `(moduleMyPet() || actionsMyPet())` with a non-null provider.
  - `reloadConfiguration`: rebuild `bedrockPetService`.
- `EssentialsAddonConfiguration`: add `modules.mypet`, `actions-only.mypet` (with the `&& !module` guard), `hub.button-mypet`, and `ui.pet-*` / `messages.pet-*` keys + getters (titles, buttons, info template, confirm template, suffixes, not-ready / no-pets / no-active-pet / buy-success / buy-failed / call-success / putaway-success / skilltree-set / skilltree-failed / cannot-afford messages).
- `ConfigMigrator`: confirm new keys get default-injected (verify its mechanism during implementation).
- `plugin.yml`: add `MyPet` to `softdepend`; register `pet` command (aliases `pets`). `/petshop` and `/pcst` are MyPet's own commands — intercepted via the listener, not registered here.
- `build.gradle`: `compileOnly("de.keyle:mypet:<version>")` (exclude spigot-api). `gradle.properties`: `myPetVersion=...`. Add the MyPet maven repo (`https://repo.mypet-plugin.de/`) to the relevant `repositories` block — verify the exact api artifactId against the repo during setup; fall back to JitPack `com.github.MyPetORG:MyPet:<tag>` if needed.

### Concurrency notes

- `Repository.getMyPets` is async; all form-building that depends on it runs in the callback. Sending a Floodgate form off-thread is acceptable, but any MyPet entity spawn (`createEntity`) and Bukkit world interaction must be scheduled back onto the main thread.
- Shop purchase touches Vault economy — perform on main thread.

### Testing

- `MyPetProvider` is reflection-thin over a third-party API that is hard to mock fully; cover the parts that don't need a live server: view-record mapping, owned-type detection logic, skilltree-eligibility filtering (given mocked type/level inputs), and button-label selection (active vs inactive). Form wiring verified manually in-game on Bedrock.

---

## Out of scope

- Vanilla-accurate potion names (mapping table) — explicitly declined; prettified enum names only.
- Per-purchase dedup persistence for the pet shop — declined in favor of own-a-type detection.
- MyPet skill-point allocation UI (`/petskill`) — `/pcst` is skilltree switching only.
- Java-player forms — Java keeps native MyPet GUIs.
