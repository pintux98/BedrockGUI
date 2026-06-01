# Bedwars Addon — Plan 1: Foundation + Shop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create the `bedwars-addon` Gradle module and ship the in-game item-shop feature end-to-end: a Bedrock player who triggers BedWars2023's shop sees a Cumulus form (flat category list → item list → buy) instead of the native chest GUI, with purchases routed through the BedWars2023 API. Java players are untouched.

**Architecture:** New module mirrors `essentials-addon`. BedWars2023 lives behind a `ShopProvider` interface (only `provider/BedWars2023*` touches `com.tomkeuper.*`). A pure **menu-model** layer turns DTOs into button descriptors (unit-tested with fakes, no Bukkit). `BedrockShopService` wires the model into `BedrockGUIApi` forms. A `ShopOpenListener` cancels BedWars2023's `ShopOpenEvent` for Bedrock players and opens the form. Live state (money, owned tiers) is read per-open.

**Tech Stack:** Java 21, Gradle (shadow), Paper API 1.20.1, BedWars2023-API (jitpack, compileOnly), Floodgate API, BedrockGUI `:common` (Cumulus forms), JUnit 5 + Mockito (this module only).

---

## Spec coverage note

This plan implements the **Foundation** (module scaffold, gradle wiring, provider abstraction, Bedrock detection, config, sound, interception layer) and the **Shop module** from `docs/superpowers/specs/2026-06-01-bedwars-addon-design.md`. Upgrades, Arena selector, Stats, Party/Spectator are deferred to Plans 2–5, which reuse everything built here.

## Confirmed BedWars2023 API surface (from javadocs.tomkeuper.com)

- Main: `com.tomkeuper.bedwars.api.BedWars`, obtained via Bukkit `ServicesManager`.
  - `BedWars.ShopUtil getShopUtil()` → `IShopManager getShopManager()`, `IShopCache getShopCache()`, `IPlayerQuickBuyCache getPlayerQuickBuyCache()`, `int calculateMoney(Player, Material)`, `void takeMoney(Player, Material, int)`, `Material getCurrency(String)`.
  - `BedWars.ArenaUtil getArenaUtil()` (used in later plans).
- `com.tomkeuper.bedwars.api.shop.IShopIndex`: `List<IShopCategory> getCategoryList()`, `void open(Player, IPlayerQuickBuyCache, boolean callEvent)`.
- `com.tomkeuper.bedwars.api.shop.IShopCategory`: `String getName()`, `List<ICategoryContent> getCategoryContentList()`, `ICategoryContent getCategoryContent(String identifier, IShopIndex)`, `int getSlot()`, `ItemStack getItemStack(Player)`, `void open(Player, IShopIndex, IShopCache)`.
- `com.tomkeuper.bedwars.api.events.shop.ShopOpenEvent`: `Player getPlayer()`, `IArena getArena()`, `boolean isCancelled()`, `void setCancelled(boolean)` (extends Bukkit `Event`, cancellable).
- `ICategoryContent`: has an `execute(...)` purchase method and identifier/tier accessors. **Exact `execute` signature and price/tier accessors are confirmed by the Task 11 spike against the resolved API jar before the provider impl is written** (the javadoc page for this interface 404s).

---

## File structure

```
bedwars-addon/
  build.gradle                                          Task 1
  src/main/resources/plugin.yml                         Task 1
  src/main/resources/config.yml                         Task 6
  src/main/java/it/pintux/life/bedwarsaddon/
    model/ShopCategory.java                             Task 3
    model/ShopContent.java                              Task 3
    model/PurchaseResult.java                           Task 3
    menu/MenuButton.java                                Task 9
    menu/ShopMenuModel.java                             Task 9   (pure, unit-tested)
    api/ShopProvider.java                               Task 4
    api/BedrockPlayerDetector.java                      Task 5
    provider/FloodgateBedrockPlayerDetector.java        Task 5
    provider/BedWars2023ShopProvider.java               Task 12  (only file touching com.tomkeuper.*)
    provider/BedWarsApiAccess.java                      Task 11  (resolves BedWars service)
    util/BukkitFormPlayer.java                          Task 8
    util/FormPlayerResolver.java                        Task 8
    util/BedwarsActionPayloads.java                     Task 8   (pure, unit-tested)
    util/BedrockSoundFeedback.java                      Task 7
    config/BedwarsAddonConfiguration.java               Task 6
    service/ShopCatalogService.java                     Task 10  (holds active provider)
    service/BedrockShopService.java                     Task 13  (model -> BedrockGUIApi forms)
    action/OpenShopMainAction.java                      Task 14
    action/OpenShopCategoryAction.java                  Task 14
    action/ShopBuyAction.java                           Task 14
    listener/ShopOpenListener.java                      Task 15
    command/BedwarsAddonCommand.java                    Task 16
    BedrockBedwarsAddonPlugin.java                      Task 16
  src/test/java/it/pintux/life/bedwarsaddon/
    SanityTest.java                                     Task 2
    menu/ShopMenuModelTest.java                         Task 9
    util/BedwarsActionPayloadsTest.java                 Task 8
    testutil/FakeShopProvider.java                      Task 4
```

Root edits: `settings.gradle` (+`include 'bedwars-addon'`), `gradle.properties` (+versions), `build.gradle` (+`buildBedwarsAddon` task).

---

### Task 1: Module scaffold + Gradle wiring

**Files:**
- Modify: `settings.gradle`
- Modify: `gradle.properties`
- Modify: `build.gradle` (root)
- Create: `bedwars-addon/build.gradle`
- Create: `bedwars-addon/src/main/resources/plugin.yml`

- [ ] **Step 1: Register the module**

In `settings.gradle`, change the last line from:
```groovy
include 'essentials-addon'
```
to:
```groovy
include 'essentials-addon'
include 'bedwars-addon'
```

- [ ] **Step 2: Add versions to `gradle.properties`**

Append under a new heading at the end of `gradle.properties`:
```properties
# Bedwars Addon
bedwarsAddonVersion=1.0.0
# jitpack coordinate confirmed in Task 11; this is the expected tag
bedWars2023Version=v25.2
junitVersion=5.10.2
mockitoVersion=5.11.0
```

- [ ] **Step 3: Add root build task**

In root `build.gradle`, after the `buildEssentialsAddon` task block, append:
```groovy
// Task to build only the Bedwars Bedrock addon
task buildBedwarsAddon {
    dependsOn ':bedwars-addon:jar'
    description = 'Builds only the Bedwars addon module'
}
```

- [ ] **Step 4: Create `bedwars-addon/build.gradle`**

```groovy
version = bedwarsAddonVersion

repositories {
    maven { url = "https://jitpack.io" }
}

dependencies {
    compileOnly project(':common')
    compileOnly "io.papermc.paper:paper-api:${paperApiVersion}"
    // BedWars2023 API (jitpack). Exact module path verified in Task 11.
    compileOnly("com.github.tomkeuper.BedWars2023:BedWars2023-API:${bedWars2023Version}") {
        exclude group: "org.spigotmc", module: "spigot-api"
        exclude group: "io.papermc.paper", module: "paper-api"
    }

    testImplementation "org.junit.jupiter:junit-jupiter:${junitVersion}"
    testImplementation "org.mockito:mockito-core:${mockitoVersion}"
    testRuntimeOnly "org.junit.platform:junit-platform-launcher"
}

test {
    useJUnitPlatform()
}

tasks.withType(JavaCompile).configureEach {
    dependsOn(":common:jar")
}

processResources {
    def props = [version: version]
    inputs.properties props
    filteringCharset 'UTF-8'
    filesMatching('plugin.yml') {
        expand props
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

shadowJar {
    minimize()
    archiveClassifier.set('')
    archiveBaseName.set('BedrockGUI-BedwarsAddon')
    if (localOutputDir) {
        destinationDirectory.set(file("${localOutputDir}/lobby/plugins"))
    }
}
```

- [ ] **Step 5: Create `bedwars-addon/src/main/resources/plugin.yml`**

```yaml
name: BedrockGUI-BedwarsAddon
version: '${version}'
main: it.pintux.life.bedwarsaddon.BedrockBedwarsAddonPlugin
api-version: '1.20'
author: pintux
softdepend: [BedWars2023, floodgate, BedrockGUI-Paper]
commands:
  bedwarsaddon:
    description: Bedwars addon admin command
    usage: /bedwarsaddon reload
    aliases: [bwaddon]
```

- [ ] **Step 6: Verify the module configures and compiles (empty source is fine)**

Run: `./gradlew :bedwars-addon:compileJava --console=plain`
Expected: `BUILD SUCCESSFUL` (no sources yet; the task resolves dependencies and the BedWars2023-API artifact from jitpack). If the jitpack coordinate fails to resolve, do not block — note the error and continue; Task 11 pins the exact coordinate. The `compileOnly` artifact is not needed until Task 12.

- [ ] **Step 7: Commit**

```bash
git add settings.gradle gradle.properties build.gradle bedwars-addon/build.gradle bedwars-addon/src/main/resources/plugin.yml
git commit -m "feat(bedwars-addon): scaffold module + gradle wiring"
```

---

### Task 2: Test infrastructure sanity check

**Files:**
- Create: `bedwars-addon/src/test/java/it/pintux/life/bedwarsaddon/SanityTest.java`

- [ ] **Step 1: Write the sanity test**

```java
package it.pintux.life.bedwarsaddon;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SanityTest {
    @Test
    void junitIsWired() {
        assertEquals(2, 1 + 1);
    }
}
```

- [ ] **Step 2: Run it**

Run: `./gradlew :bedwars-addon:test --console=plain`
Expected: `BUILD SUCCESSFUL`, 1 test passes.

- [ ] **Step 3: Commit**

```bash
git add bedwars-addon/src/test/java/it/pintux/life/bedwarsaddon/SanityTest.java
git commit -m "test(bedwars-addon): add junit sanity test"
```

---

### Task 3: Model DTOs

**Files:**
- Create: `bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/model/ShopCategory.java`
- Create: `bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/model/ShopContent.java`
- Create: `bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/model/PurchaseResult.java`

No tests for plain records (no behavior); behavior is tested in Task 9.

- [ ] **Step 1: Create `ShopCategory`**

```java
package it.pintux.life.bedwarsaddon.model;

/** Static shop-category descriptor exposed by a ShopProvider. */
public record ShopCategory(String id, String name, int slot) {}
```

- [ ] **Step 2: Create `ShopContent`**

```java
package it.pintux.life.bedwarsaddon.model;

/**
 * A purchasable shop item resolved live for one player.
 *
 * @param id         provider identifier used to purchase
 * @param name       display name (already colorized by the provider)
 * @param cost       price in the item's currency
 * @param currency   human-readable currency name (e.g. "Iron", "Gold", "Emerald")
 * @param affordable whether the player can currently afford it
 * @param tier       current tier label for tiered items (e.g. "II"); empty if untiered
 */
public record ShopContent(String id, String name, int cost, String currency,
                          boolean affordable, String tier) {}
```

- [ ] **Step 3: Create `PurchaseResult`**

```java
package it.pintux.life.bedwarsaddon.model;

/** Outcome of a purchase attempt, so services give clean feedback without parsing exceptions. */
public record PurchaseResult(boolean success, String reason) {
    public static PurchaseResult ok() { return new PurchaseResult(true, null); }
    public static PurchaseResult fail(String reason) { return new PurchaseResult(false, reason); }
}
```

- [ ] **Step 4: Verify compile**

Run: `./gradlew :bedwars-addon:compileJava --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/model/
git commit -m "feat(bedwars-addon): add shop model DTOs"
```

---

### Task 4: ShopProvider interface + fake test double

**Files:**
- Create: `bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/api/ShopProvider.java`
- Create: `bedwars-addon/src/test/java/it/pintux/life/bedwarsaddon/testutil/FakeShopProvider.java`

- [ ] **Step 1: Create the interface**

```java
package it.pintux.life.bedwarsaddon.api;

import it.pintux.life.bedwarsaddon.model.PurchaseResult;
import it.pintux.life.bedwarsaddon.model.ShopCategory;
import it.pintux.life.bedwarsaddon.model.ShopContent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

/**
 * Contract for a Bedwars shop backend (BedWars2023, etc.).
 * Implementations are the ONLY classes allowed to touch the underlying plugin's types.
 */
public interface ShopProvider {
    String getProviderId();

    boolean isReady();

    /** Static category structure for the player's shop index. */
    List<ShopCategory> getCategories(Player player);

    /** Live contents of one category for this player (cost/affordable computed now). */
    List<ShopContent> getCategoryContents(Player player, String categoryId);

    /** Execute the purchase of a content id. */
    PurchaseResult purchase(Player player, String contentId);

    /** Interception seam: does this inventory belong to this provider's shop GUI? */
    boolean ownsInventory(Inventory inventory);
}
```

- [ ] **Step 2: Create the fake (test double) used by later tests**

```java
package it.pintux.life.bedwarsaddon.testutil;

import it.pintux.life.bedwarsaddon.api.ShopProvider;
import it.pintux.life.bedwarsaddon.model.PurchaseResult;
import it.pintux.life.bedwarsaddon.model.ShopCategory;
import it.pintux.life.bedwarsaddon.model.ShopContent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-memory ShopProvider for unit tests. No Bukkit calls. */
public final class FakeShopProvider implements ShopProvider {
    public boolean ready = true;
    public final List<ShopCategory> categories = new ArrayList<>();
    public final Map<String, List<ShopContent>> contents = new LinkedHashMap<>();
    public final List<String> purchased = new ArrayList<>();
    public PurchaseResult nextResult = PurchaseResult.ok();

    @Override public String getProviderId() { return "Fake"; }
    @Override public boolean isReady() { return ready; }
    @Override public List<ShopCategory> getCategories(Player player) { return categories; }
    @Override public List<ShopContent> getCategoryContents(Player player, String categoryId) {
        return contents.getOrDefault(categoryId, List.of());
    }
    @Override public PurchaseResult purchase(Player player, String contentId) {
        purchased.add(contentId);
        return nextResult;
    }
    @Override public boolean ownsInventory(Inventory inventory) { return false; }
}
```

- [ ] **Step 3: Verify compile (main + test)**

Run: `./gradlew :bedwars-addon:compileTestJava --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/api/ShopProvider.java bedwars-addon/src/test/java/it/pintux/life/bedwarsaddon/testutil/FakeShopProvider.java
git commit -m "feat(bedwars-addon): add ShopProvider interface + fake"
```

---

### Task 5: Bedrock player detector

**Files:**
- Create: `bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/api/BedrockPlayerDetector.java`
- Create: `bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/provider/FloodgateBedrockPlayerDetector.java`

Manual-verification only (Floodgate runtime dependency); no unit test.

- [ ] **Step 1: Create the interface**

```java
package it.pintux.life.bedwarsaddon.api;

import org.bukkit.entity.Player;

public interface BedrockPlayerDetector {
    boolean isBedrockPlayer(Player player);
}
```

- [ ] **Step 2: Create the Floodgate implementation** (copied pattern from essentials-addon)

```java
package it.pintux.life.bedwarsaddon.provider;

import it.pintux.life.bedwarsaddon.api.BedrockPlayerDetector;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

public final class FloodgateBedrockPlayerDetector implements BedrockPlayerDetector {
    @Override
    public boolean isBedrockPlayer(Player player) {
        try {
            return player != null && FloodgateApi.getInstance() != null
                    && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        } catch (Exception ignored) {
            return false;
        }
    }
}
```

Note: `org.geysermc.floodgate:api` is already provided to every subproject by the root `build.gradle` `dependencies` block, so no extra dependency is needed.

- [ ] **Step 3: Verify compile**

Run: `./gradlew :bedwars-addon:compileJava --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/api/BedrockPlayerDetector.java bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/provider/FloodgateBedrockPlayerDetector.java
git commit -m "feat(bedwars-addon): add floodgate bedrock detector"
```

---

### Task 6: Configuration + config.yml

**Files:**
- Create: `bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/config/BedwarsAddonConfiguration.java`
- Create: `bedwars-addon/src/main/resources/config.yml`

- [ ] **Step 1: Create `config.yml`**

```yaml
# BedrockGUI Bedwars Addon
modules:
  shop: true

shop:
  title: "&8Shop"
  content: "Select a category"
  # category button: {category} replaced with the category name
  category-button: "&a{category}"
  # item button: {item}, {cost}, {currency}, {tier} placeholders
  item-button: "&f{item}\n&7{cost} {currency}"
  item-button-unaffordable: "&c{item}\n&7{cost} {currency}"
  back-button: "&7Back"
  close-button: "&cClose"
  purchase-success: "&aPurchased {item}."
  purchase-failed: "&cCould not purchase {item}: {reason}"
  not-in-game: "&cYou must be in a game to use the shop."
  provider-unavailable: "&cThe shop is currently unavailable."

sounds:
  enabled: true
  form-open: "UI_BUTTON_CLICK"
  purchase-success: "ENTITY_PLAYER_LEVELUP"
  purchase-failed: "BLOCK_NOTE_BLOCK_PLING"
  volume: 1.0
  pitch: 1.0
```

- [ ] **Step 2: Create `BedwarsAddonConfiguration`** (mirrors `EssentialsAddonConfiguration` load+render style)

```java
package it.pintux.life.bedwarsaddon.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;

public final class BedwarsAddonConfiguration {
    public static final String FILE_NAME = "config.yml";

    private final boolean moduleShop;
    private final String shopTitle;
    private final String shopContent;
    private final String shopCategoryButton;
    private final String shopItemButton;
    private final String shopItemButtonUnaffordable;
    private final String shopBackButton;
    private final String shopCloseButton;
    private final String shopPurchaseSuccess;
    private final String shopPurchaseFailed;
    private final String shopNotInGame;
    private final String shopProviderUnavailable;

    private final boolean soundsEnabled;
    private final String soundFormOpen;
    private final String soundPurchaseSuccess;
    private final String soundPurchaseFailed;
    private final float soundVolume;
    private final float soundPitch;

    private BedwarsAddonConfiguration(YamlConfiguration c) {
        this.moduleShop = c.getBoolean("modules.shop", true);
        this.shopTitle = color(c.getString("shop.title", "&8Shop"));
        this.shopContent = color(c.getString("shop.content", "Select a category"));
        this.shopCategoryButton = c.getString("shop.category-button", "&a{category}");
        this.shopItemButton = c.getString("shop.item-button", "&f{item}\n&7{cost} {currency}");
        this.shopItemButtonUnaffordable = c.getString("shop.item-button-unaffordable", "&c{item}\n&7{cost} {currency}");
        this.shopBackButton = color(c.getString("shop.back-button", "&7Back"));
        this.shopCloseButton = color(c.getString("shop.close-button", "&cClose"));
        this.shopPurchaseSuccess = c.getString("shop.purchase-success", "&aPurchased {item}.");
        this.shopPurchaseFailed = c.getString("shop.purchase-failed", "&cCould not purchase {item}: {reason}");
        this.shopNotInGame = color(c.getString("shop.not-in-game", "&cYou must be in a game to use the shop."));
        this.shopProviderUnavailable = color(c.getString("shop.provider-unavailable", "&cThe shop is currently unavailable."));

        this.soundsEnabled = c.getBoolean("sounds.enabled", true);
        this.soundFormOpen = c.getString("sounds.form-open", "UI_BUTTON_CLICK");
        this.soundPurchaseSuccess = c.getString("sounds.purchase-success", "ENTITY_PLAYER_LEVELUP");
        this.soundPurchaseFailed = c.getString("sounds.purchase-failed", "BLOCK_NOTE_BLOCK_PLING");
        this.soundVolume = (float) c.getDouble("sounds.volume", 1.0);
        this.soundPitch = (float) c.getDouble("sounds.pitch", 1.0);
    }

    public static BedwarsAddonConfiguration load(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        return new BedwarsAddonConfiguration(yaml);
    }

    private static String color(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }

    /** Replace {key} tokens then translate color codes. */
    public String render(String template, Map<String, String> values) {
        String out = template == null ? "" : template;
        for (Map.Entry<String, String> e : values.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        }
        return color(out);
    }

    public boolean moduleShop() { return moduleShop; }
    public String shopTitle() { return shopTitle; }
    public String shopContent() { return shopContent; }
    public String shopCategoryButton() { return shopCategoryButton; }
    public String shopItemButton() { return shopItemButton; }
    public String shopItemButtonUnaffordable() { return shopItemButtonUnaffordable; }
    public String shopBackButton() { return shopBackButton; }
    public String shopCloseButton() { return shopCloseButton; }
    public String shopPurchaseSuccess() { return shopPurchaseSuccess; }
    public String shopPurchaseFailed() { return shopPurchaseFailed; }
    public String shopNotInGame() { return shopNotInGame; }
    public String shopProviderUnavailable() { return shopProviderUnavailable; }
    public boolean soundsEnabled() { return soundsEnabled; }
    public String soundFormOpen() { return soundFormOpen; }
    public String soundPurchaseSuccess() { return soundPurchaseSuccess; }
    public String soundPurchaseFailed() { return soundPurchaseFailed; }
    public float soundVolume() { return soundVolume; }
    public float soundPitch() { return soundPitch; }
}
```

- [ ] **Step 3: Verify compile**

Run: `./gradlew :bedwars-addon:compileJava --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/config/BedwarsAddonConfiguration.java bedwars-addon/src/main/resources/config.yml
git commit -m "feat(bedwars-addon): add configuration + config.yml"
```

---

### Task 7: Sound feedback

**Files:**
- Create: `bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/util/BedrockSoundFeedback.java`

Copy verbatim from essentials-addon (`essentials-addon/.../util/BedrockSoundFeedback.java`), changing only the package line.

- [ ] **Step 1: Create the file**

```java
package it.pintux.life.bedwarsaddon.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class BedrockSoundFeedback {

    private static final ConcurrentMap<UUID, Boolean> playerSoundEnabled = new ConcurrentHashMap<>();

    private boolean enabled = true;
    private Sound formOpenSound = Sound.UI_BUTTON_CLICK;
    private Sound purchaseSuccessSound = Sound.ENTITY_PLAYER_LEVELUP;
    private Sound purchaseFailedSound = Sound.BLOCK_NOTE_BLOCK_PLING;
    private float defaultVolume = 1.0f;
    private float defaultPitch = 1.0f;

    public void configure(boolean enabled, String formOpen, String purchaseSuccess, String purchaseFailed, float volume, float pitch) {
        this.enabled = enabled;
        this.formOpenSound = parseSound(formOpen, Sound.UI_BUTTON_CLICK);
        this.purchaseSuccessSound = parseSound(purchaseSuccess, Sound.ENTITY_PLAYER_LEVELUP);
        this.purchaseFailedSound = parseSound(purchaseFailed, Sound.BLOCK_NOTE_BLOCK_PLING);
        this.defaultVolume = volume;
        this.defaultPitch = pitch;
    }

    public void playFormOpen(Player player) { play(player, formOpenSound); }
    public void playPurchaseSuccess(Player player) { play(player, purchaseSuccessSound); }
    public void playPurchaseFailed(Player player) { play(player, purchaseFailedSound); }

    public void setPlayerEnabled(UUID playerId, boolean enabled) {
        playerSoundEnabled.put(playerId, enabled);
    }

    private void play(Player player, Sound sound) {
        if (!enabled || player == null || !player.isOnline()) return;
        if (Boolean.FALSE.equals(playerSoundEnabled.get(player.getUniqueId()))) return;
        try {
            player.playSound(player.getLocation(), sound, defaultVolume, defaultPitch);
        } catch (Exception ignored) {
        }
    }

    private static Sound parseSound(String name, Sound fallback) {
        if (name == null || name.isBlank()) return fallback;
        try {
            return Sound.valueOf(name.toUpperCase().replace('.', '_'));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
```

- [ ] **Step 2: Verify compile**

Run: `./gradlew :bedwars-addon:compileJava --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/util/BedrockSoundFeedback.java
git commit -m "feat(bedwars-addon): add sound feedback"
```

---

### Task 8: Form-player helpers + action payload codec (TDD for the codec)

**Files:**
- Create: `bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/util/BukkitFormPlayer.java`
- Create: `bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/util/FormPlayerResolver.java`
- Create: `bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/util/BedwarsActionPayloads.java`
- Test: `bedwars-addon/src/test/java/it/pintux/life/bedwarsaddon/util/BedwarsActionPayloadsTest.java`

`BukkitFormPlayer` and `FormPlayerResolver` are copied verbatim from essentials-addon (package line changed). `BedwarsActionPayloads` is new and TDD'd because category/content ids may contain `:` or spaces, which would break the `type:value` action-string format — so they're Base64-encoded.

- [ ] **Step 1: Write the failing test**

```java
package it.pintux.life.bedwarsaddon.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BedwarsActionPayloadsTest {
    @Test
    void roundTripsIdsContainingColonsAndSpaces() {
        String id = "default_blocks:wool tier:2";
        String encoded = BedwarsActionPayloads.encode(id);
        assertEquals(id, BedwarsActionPayloads.decode(encoded));
    }

    @Test
    void encodedFormHasNoColon() {
        String encoded = BedwarsActionPayloads.encode("a:b:c");
        assertEquals(false, encoded.contains(":"));
    }

    @Test
    void decodeNullOrBlankReturnsEmpty() {
        assertEquals("", BedwarsActionPayloads.decode(null));
        assertEquals("", BedwarsActionPayloads.decode("  "));
    }
}
```

- [ ] **Step 2: Run it, verify it fails**

Run: `./gradlew :bedwars-addon:test --tests '*BedwarsActionPayloadsTest' --console=plain`
Expected: FAIL — `BedwarsActionPayloads` does not exist (compilation error).

- [ ] **Step 3: Implement the codec**

```java
package it.pintux.life.bedwarsaddon.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Encodes provider ids so they survive the "type:value" action-string format. */
public final class BedwarsActionPayloads {
    private BedwarsActionPayloads() {}

    public static String encode(String raw) {
        if (raw == null) return "";
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static String decode(String encoded) {
        if (encoded == null || encoded.isBlank()) return "";
        try {
            return new String(Base64.getUrlDecoder().decode(encoded.trim()), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }
}
```

- [ ] **Step 4: Run it, verify it passes**

Run: `./gradlew :bedwars-addon:test --tests '*BedwarsActionPayloadsTest' --console=plain`
Expected: PASS (3 tests).

- [ ] **Step 5: Create `BukkitFormPlayer`** (verbatim copy, new package)

```java
package it.pintux.life.bedwarsaddon.util;

import it.pintux.life.common.utils.FormPlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class BukkitFormPlayer implements FormPlayer {
    private final Player player;

    public BukkitFormPlayer(Player player) { this.player = player; }

    @Override public UUID getUniqueId() { return player.getUniqueId(); }
    @Override public String getName() { return player.getName(); }
    @Override public void sendMessage(String message) { player.sendMessage(message); }
    @Override public boolean executeAction(String action) { player.chat(action); return true; }
    @Override public boolean hasPermission(String permission) { return player.hasPermission(permission); }
    public Player getPlayer() { return player; }
}
```

- [ ] **Step 6: Create `FormPlayerResolver`** (verbatim copy, new package)

```java
package it.pintux.life.bedwarsaddon.util;

import it.pintux.life.common.utils.FormPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public final class FormPlayerResolver {
    private FormPlayerResolver() {}

    public static Player resolve(FormPlayer formPlayer) {
        if (formPlayer == null) return null;
        if (formPlayer instanceof BukkitFormPlayer bukkitFormPlayer) {
            return bukkitFormPlayer.getPlayer();
        }
        try {
            Method method = formPlayer.getClass().getMethod("getBukkitPlayer");
            Object value = method.invoke(formPlayer);
            if (value instanceof Player player) return player;
        } catch (Exception ignored) {
        }
        return Bukkit.getPlayer(formPlayer.getUniqueId());
    }
}
```

- [ ] **Step 7: Verify full module compiles + tests pass**

Run: `./gradlew :bedwars-addon:test --console=plain`
Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 8: Commit**

```bash
git add bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/util/ bedwars-addon/src/test/java/it/pintux/life/bedwarsaddon/util/
git commit -m "feat(bedwars-addon): add form-player helpers + action payload codec"
```

---

### Task 9: Pure menu-model layer (TDD core)

This is the unit-tested heart of the shop UI logic: turning DTOs + config into ordered button descriptors with correct labels and action strings, decoupled from `BedrockGUIApi`.

**Files:**
- Create: `bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/menu/MenuButton.java`
- Create: `bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/menu/ShopMenuModel.java`
- Test: `bedwars-addon/src/test/java/it/pintux/life/bedwarsaddon/menu/ShopMenuModelTest.java`

Action string scheme (defined here, consumed by Task 14 actions):
- `bw_shop_main:` — open the category list
- `bw_shop_cat:<encodedCategoryId>` — open a category's items
- `bw_shop_buy:<encodedContentId>` — buy an item

- [ ] **Step 1: Create `MenuButton`**

```java
package it.pintux.life.bedwarsaddon.menu;

/** A single form button: visible label + the action string to run when clicked. */
public record MenuButton(String label, String actionString) {}
```

- [ ] **Step 2: Write the failing test**

```java
package it.pintux.life.bedwarsaddon.menu;

import it.pintux.life.bedwarsaddon.config.BedwarsAddonConfiguration;
import it.pintux.life.bedwarsaddon.model.ShopCategory;
import it.pintux.life.bedwarsaddon.model.ShopContent;
import it.pintux.life.bedwarsaddon.util.BedwarsActionPayloads;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShopMenuModelTest {

    // Build a configuration from an in-memory YAML (no plugin/disk needed).
    private BedwarsAddonConfiguration config() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("shop.category-button", "{category}");
        yaml.set("shop.item-button", "{item} - {cost} {currency}");
        yaml.set("shop.item-button-unaffordable", "X {item} - {cost} {currency}");
        yaml.set("shop.back-button", "Back");
        yaml.set("shop.close-button", "Close");
        Constructor<BedwarsAddonConfiguration> ctor =
                BedwarsAddonConfiguration.class.getDeclaredConstructor(YamlConfiguration.class);
        ctor.setAccessible(true);
        return ctor.newInstance(yaml);
    }

    @Test
    void categoryButtonsOneePerCategoryThenClose() throws Exception {
        ShopMenuModel model = new ShopMenuModel(config());
        List<MenuButton> buttons = model.categoryButtons(List.of(
                new ShopCategory("blocks", "Blocks", 1),
                new ShopCategory("melee", "Melee", 2)));

        assertEquals(3, buttons.size());
        assertEquals("Blocks", buttons.get(0).label());
        assertEquals("bw_shop_cat:" + BedwarsActionPayloads.encode("blocks"), buttons.get(0).actionString());
        assertEquals("Melee", buttons.get(1).label());
        assertEquals("Close", buttons.get(2).label());
        assertEquals("bw_shop_close:", buttons.get(2).actionString());
    }

    @Test
    void itemButtonsUseAffordableVsUnaffordableTemplateThenBack() throws Exception {
        ShopMenuModel model = new ShopMenuModel(config());
        List<MenuButton> buttons = model.contentButtons(List.of(
                new ShopContent("wool", "Wool", 4, "Iron", true, ""),
                new ShopContent("sword", "Sword", 7, "Gold", false, "II")));

        assertEquals(3, buttons.size());
        assertEquals("Wool - 4 Iron", buttons.get(0).label());
        assertEquals("bw_shop_buy:" + BedwarsActionPayloads.encode("wool"), buttons.get(0).actionString());
        assertEquals("X Sword - 7 Gold", buttons.get(1).label());
        assertEquals("Back", buttons.get(2).label());
        assertEquals("bw_shop_main:", buttons.get(2).actionString());
    }
}
```

- [ ] **Step 3: Run it, verify it fails**

Run: `./gradlew :bedwars-addon:test --tests '*ShopMenuModelTest' --console=plain`
Expected: FAIL — `ShopMenuModel` does not exist.

- [ ] **Step 4: Implement `ShopMenuModel`**

```java
package it.pintux.life.bedwarsaddon.menu;

import it.pintux.life.bedwarsaddon.config.BedwarsAddonConfiguration;
import it.pintux.life.bedwarsaddon.model.ShopCategory;
import it.pintux.life.bedwarsaddon.model.ShopContent;
import it.pintux.life.bedwarsaddon.util.BedwarsActionPayloads;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Pure: builds ordered form buttons from shop DTOs + config. No Bukkit / no BedrockGUIApi. */
public final class ShopMenuModel {
    private final BedwarsAddonConfiguration config;

    public ShopMenuModel(BedwarsAddonConfiguration config) {
        this.config = config;
    }

    public List<MenuButton> categoryButtons(List<ShopCategory> categories) {
        List<MenuButton> buttons = new ArrayList<>();
        for (ShopCategory category : categories) {
            String label = config.render(config.shopCategoryButton(), Map.of("category", category.name()));
            buttons.add(new MenuButton(label, "bw_shop_cat:" + BedwarsActionPayloads.encode(category.id())));
        }
        buttons.add(new MenuButton(config.shopCloseButton(), "bw_shop_close:"));
        return buttons;
    }

    public List<MenuButton> contentButtons(List<ShopContent> contents) {
        List<MenuButton> buttons = new ArrayList<>();
        for (ShopContent content : contents) {
            String template = content.affordable() ? config.shopItemButton() : config.shopItemButtonUnaffordable();
            String label = config.render(template, Map.of(
                    "item", content.name(),
                    "cost", String.valueOf(content.cost()),
                    "currency", content.currency(),
                    "tier", content.tier()));
            buttons.add(new MenuButton(label, "bw_shop_buy:" + BedwarsActionPayloads.encode(content.id())));
        }
        buttons.add(new MenuButton(config.shopBackButton(), "bw_shop_main:"));
        return buttons;
    }
}
```

- [ ] **Step 5: Run it, verify it passes**

Run: `./gradlew :bedwars-addon:test --tests '*ShopMenuModelTest' --console=plain`
Expected: PASS (2 tests).

- [ ] **Step 6: Commit**

```bash
git add bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/menu/ bedwars-addon/src/test/java/it/pintux/life/bedwarsaddon/menu/
git commit -m "feat(bedwars-addon): add pure shop menu-model layer (TDD)"
```

---

### Task 10: Shop catalog service (active-provider holder)

**Files:**
- Create: `bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/service/ShopCatalogService.java`

Mirrors essentials `*CatalogService`: holds the active provider, guards readiness. Thin — no unit test (delegation only).

- [ ] **Step 1: Create the service**

```java
package it.pintux.life.bedwarsaddon.service;

import it.pintux.life.bedwarsaddon.api.ShopProvider;
import it.pintux.life.bedwarsaddon.model.PurchaseResult;
import it.pintux.life.bedwarsaddon.model.ShopCategory;
import it.pintux.life.bedwarsaddon.model.ShopContent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.logging.Logger;

public final class ShopCatalogService {
    private final Logger logger;
    private ShopProvider provider;

    public ShopCatalogService(Logger logger) {
        this.logger = logger;
    }

    public void setProvider(ShopProvider provider) {
        this.provider = provider;
    }

    public ShopProvider getProvider() {
        return provider;
    }

    public boolean isReady() {
        return provider != null && provider.isReady();
    }

    public List<ShopCategory> getCategories(Player player) {
        return isReady() ? provider.getCategories(player) : List.of();
    }

    public List<ShopContent> getCategoryContents(Player player, String categoryId) {
        return isReady() ? provider.getCategoryContents(player, categoryId) : List.of();
    }

    public PurchaseResult purchase(Player player, String contentId) {
        if (!isReady()) return PurchaseResult.fail("provider unavailable");
        return provider.purchase(player, contentId);
    }

    public boolean ownsInventory(Inventory inventory) {
        return isReady() && provider.ownsInventory(inventory);
    }
}
```

- [ ] **Step 2: Verify compile**

Run: `./gradlew :bedwars-addon:compileJava --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/service/ShopCatalogService.java
git commit -m "feat(bedwars-addon): add shop catalog service"
```

---

### Task 11: BedWars2023 API access + signature spike

**Files:**
- Create: `bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/provider/BedWarsApiAccess.java`

This task pins the two facts the public javadocs could not give us, then writes the safe accessor.

- [ ] **Step 1: Resolve the exact jitpack coordinate**

Run: `./gradlew :bedwars-addon:dependencies --configuration compileClasspath --console=plain`
Expected: the `com.github.tomkeuper.BedWars2023:BedWars2023-API:<tag>` artifact resolves. If it does NOT resolve, browse https://jitpack.io/#tomkeuper/BedWars2023 for a valid tag/commit, update `bedWars2023Version` in `gradle.properties`, and re-run. Record the working coordinate in the commit message.

- [ ] **Step 2: Confirm the `ICategoryContent` purchase + tier API**

Locate the resolved API jar (path printed by `:dependencies`, typically under `~/.gradle/caches/modules-2/`). Inspect the `ICategoryContent` interface signatures:

Run: `javap -classpath <path-to-BedWars2023-API.jar> com.tomkeuper.bedwars.api.shop.ICategoryContent`
Expected: a list of methods. Record from the output:
  - the purchase method (expected shape: `void execute(org.bukkit.entity.Player, com.tomkeuper.bedwars.api.shop.IShopIndex, com.tomkeuper.bedwars.api.shop.IShopCache)`),
  - the identifier accessor (expected `String getIdentifier()`),
  - the tier accessors used to read price (expected `getContentTiers()` returning a list of `IContentTier`, and a current-tier lookup via `IShopCache`/`ICachedItem`).

If `ICategoryContent` is in a different package than `com.tomkeuper.bedwars.api.shop`, run `javap` with the package shown by `IShopCategory.getCategoryContentList()` — find it with:
Run: `javap -classpath <jar> com.tomkeuper.bedwars.api.shop.IShopCategory`
and read the return type's fully-qualified name. Record the real package; Task 12 imports use it.

- [ ] **Step 3: Write `BedWarsApiAccess`**

```java
package it.pintux.life.bedwarsaddon.provider;

import com.tomkeuper.bedwars.api.BedWars;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Resolves and caches the BedWars2023 API service. Null-safe. */
public final class BedWarsApiAccess {
    private BedWars api;

    public BedWars get() {
        if (api != null) return api;
        RegisteredServiceProvider<BedWars> rsp =
                Bukkit.getServicesManager().getRegistration(BedWars.class);
        if (rsp != null) {
            api = rsp.getProvider();
        }
        return api;
    }

    public boolean isAvailable() {
        return get() != null;
    }
}
```

- [ ] **Step 4: Verify compile** (requires the API artifact resolved in Step 1)

Run: `./gradlew :bedwars-addon:compileJava --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/provider/BedWarsApiAccess.java
git commit -m "feat(bedwars-addon): add BedWars2023 API access (coordinate <tag>, ICategoryContent.execute confirmed)"
```

---

### Task 12: BedWars2023 shop provider implementation

**Files:**
- Create: `bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/provider/BedWars2023ShopProvider.java`

Translates BedWars2023 types ↔ addon DTOs and performs purchases. Kept "dumb" (no UI logic). Verified manually (Task 17), not unit-tested. **Use the exact `ICategoryContent` package and `execute`/tier signatures recorded in Task 11** — the body below uses the expected signatures; adjust the three marked lines if the spike found different ones.

- [ ] **Step 1: Create the provider**

```java
package it.pintux.life.bedwarsaddon.provider;

import com.tomkeuper.bedwars.api.BedWars;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.shop.ICategoryContent;   // confirm package in Task 11
import com.tomkeuper.bedwars.api.shop.IShopCache;
import com.tomkeuper.bedwars.api.shop.IShopCategory;
import com.tomkeuper.bedwars.api.shop.IShopIndex;
import it.pintux.life.bedwarsaddon.api.ShopProvider;
import it.pintux.life.bedwarsaddon.model.PurchaseResult;
import it.pintux.life.bedwarsaddon.model.ShopCategory;
import it.pintux.life.bedwarsaddon.model.ShopContent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class BedWars2023ShopProvider implements ShopProvider {
    private final Logger logger;
    private final BedWarsApiAccess access;

    public BedWars2023ShopProvider(Logger logger, BedWarsApiAccess access) {
        this.logger = logger;
        this.access = access;
    }

    @Override public String getProviderId() { return "BedWars2023"; }

    @Override public boolean isReady() { return access.isAvailable(); }

    private IShopIndex shopIndex() {
        BedWars api = access.get();
        return api == null ? null : api.getShopUtil().getShopManager().getShop();
        // Task 11 note: if getShop() is not the accessor, javap IShopManager and use the
        // method returning IShopIndex; record it in the commit message.
    }

    @Override
    public List<ShopCategory> getCategories(Player player) {
        IShopIndex index = shopIndex();
        if (index == null) return List.of();
        List<ShopCategory> out = new ArrayList<>();
        for (IShopCategory cat : index.getCategoryList()) {
            // identifier: category name is stable; use it as the id
            out.add(new ShopCategory(cat.getName(), cat.getName(), cat.getSlot()));
        }
        return out;
    }

    @Override
    public List<ShopContent> getCategoryContents(Player player, String categoryId) {
        IShopIndex index = shopIndex();
        if (index == null) return List.of();
        IShopCategory category = null;
        for (IShopCategory c : index.getCategoryList()) {
            if (c.getName().equals(categoryId)) { category = c; break; }
        }
        if (category == null) return List.of();

        BedWars api = access.get();
        List<ShopContent> out = new ArrayList<>();
        for (ICategoryContent content : category.getCategoryContentList()) {
            // identifier + price are read via the signatures confirmed in Task 11.
            String id = content.getIdentifier();                       // Task 11 line A
            int cost = readCost(api, player, content);                 // Task 11 line B (helper below)
            String currencyName = readCurrencyName(api, content);      // Task 11 line C
            boolean affordable = cost <= 0
                    || api.getShopUtil().calculateMoney(player, api.getShopUtil().getCurrency(currencyName)) >= cost;
            out.add(new ShopContent(id, content.getIdentifier(), cost, currencyName, affordable, ""));
        }
        return out;
    }

    // Helpers isolate the tier/price reads the Task 11 spike confirms.
    private int readCost(BedWars api, Player player, ICategoryContent content) {
        // Expected: current tier from shop cache -> tier price. Adjust per Task 11 findings.
        try {
            IShopCache cache = api.getShopUtil().getShopCache();
            // e.g. cache.getCachedItem(content)... .getCurrentTier().getPrice()
            return content.getContentTiers().get(0).getPrice();        // Task 11 confirm
        } catch (Exception e) {
            return 0;
        }
    }

    private String readCurrencyName(BedWars api, ICategoryContent content) {
        try {
            return content.getContentTiers().get(0).getCurrency();     // Task 11 confirm
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public PurchaseResult purchase(Player player, String contentId) {
        IShopIndex index = shopIndex();
        BedWars api = access.get();
        if (index == null || api == null) return PurchaseResult.fail("provider unavailable");
        for (IShopCategory category : index.getCategoryList()) {
            ICategoryContent content = category.getCategoryContent(contentId, index);
            if (content != null) {
                try {
                    IShopCache cache = api.getShopUtil().getShopCache();
                    content.execute(player, index, cache);             // Task 11 line: confirm signature
                    return PurchaseResult.ok();
                } catch (Exception e) {
                    logger.warning("Purchase failed for " + contentId + ": " + e.getClass().getSimpleName());
                    return PurchaseResult.fail("error");
                }
            }
        }
        return PurchaseResult.fail("item not found");
    }

    @Override
    public boolean ownsInventory(Inventory inventory) {
        // Shop interception is event-based (ShopOpenEvent, Task 15), so inventory ownership
        // detection is not used by the shop module. Returning false is correct here.
        return false;
    }
}
```

- [ ] **Step 2: Verify compile**

Run: `./gradlew :bedwars-addon:compileJava --console=plain`
Expected: `BUILD SUCCESSFUL`. If a method name (`getShop`, `getIdentifier`, `getContentTiers`, `getPrice`, `getCurrency`, `execute`) does not match the API jar, fix it using the `javap` output from Task 11 and re-run.

- [ ] **Step 3: Commit**

```bash
git add bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/provider/BedWars2023ShopProvider.java
git commit -m "feat(bedwars-addon): add BedWars2023 shop provider"
```

---

### Task 13: BedrockShopService (model → forms)

**Files:**
- Create: `bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/service/BedrockShopService.java`

Wires the pure `ShopMenuModel` into `BedrockGUIApi` `SimpleForm`s and routes button clicks back through action strings (same pattern as essentials `BedrockHomeService`). Verified manually; the logic it contains beyond delegation is already tested in Task 9.

- [ ] **Step 1: Create the service**

```java
package it.pintux.life.bedwarsaddon.service;

import it.pintux.life.bedwarsaddon.api.BedrockPlayerDetector;
import it.pintux.life.bedwarsaddon.config.BedwarsAddonConfiguration;
import it.pintux.life.bedwarsaddon.menu.MenuButton;
import it.pintux.life.bedwarsaddon.menu.ShopMenuModel;
import it.pintux.life.bedwarsaddon.model.PurchaseResult;
import it.pintux.life.bedwarsaddon.model.ShopContent;
import it.pintux.life.bedwarsaddon.util.BedwarsActionPayloads;
import it.pintux.life.bedwarsaddon.util.BedrockSoundFeedback;
import it.pintux.life.bedwarsaddon.util.BukkitFormPlayer;
import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.api.BedrockGUIApi;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public final class BedrockShopService {
    private final BedwarsAddonConfiguration config;
    private final ShopCatalogService catalog;
    private final ShopMenuModel menuModel;
    private final BedrockPlayerDetector detector;
    private final BedrockSoundFeedback sound;

    public BedrockShopService(BedwarsAddonConfiguration config, ShopCatalogService catalog,
                              BedrockPlayerDetector detector, BedrockSoundFeedback sound) {
        this.config = config;
        this.catalog = catalog;
        this.detector = detector;
        this.sound = sound;
        this.menuModel = new ShopMenuModel(config);
    }

    public boolean shouldHandle(Player player) {
        return player != null && detector.isBedrockPlayer(player);
    }

    public void openMain(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!catalog.isReady()) { player.sendMessage(config.shopProviderUnavailable()); return; }

        List<MenuButton> buttons = menuModel.categoryButtons(catalog.getCategories(player));
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(config.shopTitle());
        form.content(config.shopContent());
        for (MenuButton button : buttons) {
            String action = button.actionString();
            form.button(button.label(), fp -> api.executeActionString(fp, action, ctx("shop-main")));
        }
        sound.playFormOpen(player);
        form.send(new BukkitFormPlayer(player));
    }

    public void openCategory(Player player, String categoryId) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!catalog.isReady()) { player.sendMessage(config.shopProviderUnavailable()); return; }

        List<ShopContent> contents = catalog.getCategoryContents(player, categoryId);
        List<MenuButton> buttons = menuModel.contentButtons(contents);
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(config.shopTitle());
        form.content(config.shopContent());
        for (MenuButton button : buttons) {
            String action = button.actionString();
            form.button(button.label(), fp -> api.executeActionString(fp, action, ctx("shop-cat")));
        }
        form.send(new BukkitFormPlayer(player));
    }

    public void buy(Player player, String contentId) {
        if (!catalog.isReady()) { player.sendMessage(config.shopProviderUnavailable()); return; }
        // Resolve a display name for feedback (best-effort).
        String itemName = contentId;
        PurchaseResult result = catalog.purchase(player, contentId);
        if (result.success()) {
            player.sendMessage(config.render(config.shopPurchaseSuccess(), Map.of("item", itemName)));
            sound.playPurchaseSuccess(player);
        } else {
            player.sendMessage(config.render(config.shopPurchaseFailed(),
                    Map.of("item", itemName, "reason", result.reason() == null ? "" : result.reason())));
            sound.playPurchaseFailed(player);
        }
    }

    private BedrockGUIApi requireApi(Player player) {
        try {
            return BedrockGUIApi.getInstance();
        } catch (IllegalStateException e) {
            player.sendMessage(config.shopProviderUnavailable());
            return null;
        }
    }

    private ActionSystem.ActionContext ctx(String source) {
        return ActionSystem.ActionContext.builder()
                .menuName(source)
                .formType("bedrock-bw-shop")
                .build();
    }
}
```

- [ ] **Step 2: Verify compile**

Run: `./gradlew :bedwars-addon:compileJava --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/service/BedrockShopService.java
git commit -m "feat(bedwars-addon): add bedrock shop service"
```

---

### Task 14: Action handlers

**Files:**
- Create: `bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/action/OpenShopMainAction.java`
- Create: `bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/action/OpenShopCategoryAction.java`
- Create: `bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/action/ShopBuyAction.java`

Mirror essentials action handlers. The `bw_shop_close:` action is intentionally not registered — an unhandled action is a no-op, which closes the form (button just dismisses).

- [ ] **Step 1: Create `OpenShopMainAction`**

```java
package it.pintux.life.bedwarsaddon.action;

import it.pintux.life.bedwarsaddon.service.BedrockShopService;
import it.pintux.life.bedwarsaddon.util.FormPlayerResolver;
import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import org.bukkit.entity.Player;

public final class OpenShopMainAction implements ActionSystem.ActionHandler {
    private final BedrockShopService service;

    public OpenShopMainAction(BedrockShopService service) { this.service = service; }

    @Override public String getActionType() { return "bw_shop_main"; }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player p = FormPlayerResolver.resolve(player);
        if (p == null) return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        service.openMain(p);
        return ActionSystem.ActionResult.success("Opened shop");
    }

    @Override public boolean isValidAction(String actionValue) { return true; }
    @Override public String getDescription() { return "Opens the Bedrock shop category list"; }
    @Override public String[] getUsageExamples() { return new String[]{"bw_shop_main:"}; }
}
```

- [ ] **Step 2: Create `OpenShopCategoryAction`**

```java
package it.pintux.life.bedwarsaddon.action;

import it.pintux.life.bedwarsaddon.service.BedrockShopService;
import it.pintux.life.bedwarsaddon.util.BedwarsActionPayloads;
import it.pintux.life.bedwarsaddon.util.FormPlayerResolver;
import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import org.bukkit.entity.Player;

public final class OpenShopCategoryAction implements ActionSystem.ActionHandler {
    private final BedrockShopService service;

    public OpenShopCategoryAction(BedrockShopService service) { this.service = service; }

    @Override public String getActionType() { return "bw_shop_cat"; }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player p = FormPlayerResolver.resolve(player);
        if (p == null) return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        String categoryId = BedwarsActionPayloads.decode(actionValue);
        service.openCategory(p, categoryId);
        return ActionSystem.ActionResult.success("Opened category");
    }

    @Override public boolean isValidAction(String actionValue) { return actionValue != null && !actionValue.isBlank(); }
    @Override public String getDescription() { return "Opens a shop category's items"; }
    @Override public String[] getUsageExamples() { return new String[]{"bw_shop_cat:<base64-id>"}; }
}
```

- [ ] **Step 3: Create `ShopBuyAction`**

```java
package it.pintux.life.bedwarsaddon.action;

import it.pintux.life.bedwarsaddon.service.BedrockShopService;
import it.pintux.life.bedwarsaddon.util.BedwarsActionPayloads;
import it.pintux.life.bedwarsaddon.util.FormPlayerResolver;
import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.utils.FormPlayer;
import org.bukkit.entity.Player;

public final class ShopBuyAction implements ActionSystem.ActionHandler {
    private final BedrockShopService service;

    public ShopBuyAction(BedrockShopService service) { this.service = service; }

    @Override public String getActionType() { return "bw_shop_buy"; }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player p = FormPlayerResolver.resolve(player);
        if (p == null) return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        String contentId = BedwarsActionPayloads.decode(actionValue);
        service.buy(p, contentId);
        return ActionSystem.ActionResult.success("Purchase attempted");
    }

    @Override public boolean isValidAction(String actionValue) { return actionValue != null && !actionValue.isBlank(); }
    @Override public String getDescription() { return "Buys a shop item"; }
    @Override public String[] getUsageExamples() { return new String[]{"bw_shop_buy:<base64-id>"}; }
}
```

- [ ] **Step 4: Verify compile**

Run: `./gradlew :bedwars-addon:compileJava --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/action/
git commit -m "feat(bedwars-addon): add shop action handlers"
```

---

### Task 15: Shop-open interception listener

**Files:**
- Create: `bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/listener/ShopOpenListener.java`

Cancels BedWars2023's `ShopOpenEvent` for Bedrock players and opens the Cumulus form on the next tick.

- [ ] **Step 1: Create the listener**

```java
package it.pintux.life.bedwarsaddon.listener;

import com.tomkeuper.bedwars.api.events.shop.ShopOpenEvent;
import it.pintux.life.bedwarsaddon.service.BedrockShopService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public final class ShopOpenListener implements Listener {
    private final Plugin plugin;
    private final BedrockShopService shopService;

    public ShopOpenListener(Plugin plugin, BedrockShopService shopService) {
        this.plugin = plugin;
        this.shopService = shopService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShopOpen(ShopOpenEvent event) {
        Player player = event.getPlayer();
        if (!shopService.shouldHandle(player)) {
            return; // Java player: native chest GUI proceeds untouched.
        }
        event.setCancelled(true);
        // Open the form next tick so the cancelled inventory open fully settles first.
        plugin.getServer().getScheduler().runTask(plugin, () -> shopService.openMain(player));
    }
}
```

- [ ] **Step 2: Verify compile** (requires API artifact from Task 11)

Run: `./gradlew :bedwars-addon:compileJava --console=plain`
Expected: `BUILD SUCCESSFUL`. If `ShopOpenEvent.getPlayer()` / `setCancelled` differ from the javadoc, correct against the jar (`javap com.tomkeuper.bedwars.api.events.shop.ShopOpenEvent`).

- [ ] **Step 3: Commit**

```bash
git add bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/listener/ShopOpenListener.java
git commit -m "feat(bedwars-addon): intercept ShopOpenEvent for bedrock players"
```

---

### Task 16: Main plugin + command wiring

**Files:**
- Create: `bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/command/BedwarsAddonCommand.java`
- Create: `bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/BedrockBedwarsAddonPlugin.java`

- [ ] **Step 1: Create the command**

```java
package it.pintux.life.bedwarsaddon.command;

import it.pintux.life.bedwarsaddon.BedrockBedwarsAddonPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public final class BedwarsAddonCommand implements CommandExecutor, TabCompleter {
    private final BedrockBedwarsAddonPlugin plugin;

    public BedwarsAddonCommand(BedrockBedwarsAddonPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("bedwarsaddon.admin")) {
                sender.sendMessage("No permission.");
                return true;
            }
            plugin.reloadConfiguration();
            sender.sendMessage("BedwarsAddon configuration reloaded.");
            return true;
        }
        sender.sendMessage("Usage: /bedwarsaddon reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("reload");
        return List.of();
    }
}
```

- [ ] **Step 2: Create the main plugin**

```java
package it.pintux.life.bedwarsaddon;

import it.pintux.life.bedwarsaddon.action.OpenShopCategoryAction;
import it.pintux.life.bedwarsaddon.action.OpenShopMainAction;
import it.pintux.life.bedwarsaddon.action.ShopBuyAction;
import it.pintux.life.bedwarsaddon.api.BedrockPlayerDetector;
import it.pintux.life.bedwarsaddon.command.BedwarsAddonCommand;
import it.pintux.life.bedwarsaddon.config.BedwarsAddonConfiguration;
import it.pintux.life.bedwarsaddon.listener.ShopOpenListener;
import it.pintux.life.bedwarsaddon.provider.BedWars2023ShopProvider;
import it.pintux.life.bedwarsaddon.provider.BedWarsApiAccess;
import it.pintux.life.bedwarsaddon.provider.FloodgateBedrockPlayerDetector;
import it.pintux.life.bedwarsaddon.service.BedrockShopService;
import it.pintux.life.bedwarsaddon.service.ShopCatalogService;
import it.pintux.life.bedwarsaddon.util.BedrockSoundFeedback;
import it.pintux.life.common.api.BedrockGUIApi;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class BedrockBedwarsAddonPlugin extends JavaPlugin {
    private BedwarsAddonConfiguration configuration;
    private BedrockPlayerDetector detector;
    private BedrockSoundFeedback soundFeedback;
    private BedWarsApiAccess apiAccess;
    private ShopCatalogService shopCatalogService;
    private BedrockShopService bedrockShopService;

    @Override
    public void onEnable() {
        configuration = BedwarsAddonConfiguration.load(this);
        if (!configuration.moduleShop()) {
            getLogger().info("All modules disabled. Enable one in config.yml then /bedwarsaddon reload.");
            return;
        }

        detector = new FloodgateBedrockPlayerDetector();
        soundFeedback = new BedrockSoundFeedback();
        soundFeedback.configure(configuration.soundsEnabled(), configuration.soundFormOpen(),
                configuration.soundPurchaseSuccess(), configuration.soundPurchaseFailed(),
                configuration.soundVolume(), configuration.soundPitch());

        apiAccess = new BedWarsApiAccess();
        shopCatalogService = new ShopCatalogService(getLogger());

        if (Bukkit.getPluginManager().getPlugin("BedWars2023") != null) {
            shopCatalogService.setProvider(new BedWars2023ShopProvider(getLogger(), apiAccess));
            getLogger().info("Shop provider: BedWars2023");
        } else {
            getLogger().warning("BedWars2023 not found; shop module inactive.");
        }

        bedrockShopService = new BedrockShopService(configuration, shopCatalogService, detector, soundFeedback);

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new ShopOpenListener(this, bedrockShopService), this);

        getCommand("bedwarsaddon").setExecutor(new BedwarsAddonCommand(this));
        getCommand("bedwarsaddon").setTabCompleter(new BedwarsAddonCommand(this));

        BedrockGUIApi api = getApiSafely();
        if (api != null) {
            api.registerActionHandler(new OpenShopMainAction(bedrockShopService));
            api.registerActionHandler(new OpenShopCategoryAction(bedrockShopService));
            api.registerActionHandler(new ShopBuyAction(bedrockShopService));
            getLogger().info("Registered bedwars addon shop actions with BedrockGUI API");
        }
    }

    @Override
    public void onDisable() {
        shopCatalogService = null;
        bedrockShopService = null;
    }

    public void reloadConfiguration() {
        configuration = BedwarsAddonConfiguration.load(this);
        if (soundFeedback != null) {
            soundFeedback.configure(configuration.soundsEnabled(), configuration.soundFormOpen(),
                    configuration.soundPurchaseSuccess(), configuration.soundPurchaseFailed(),
                    configuration.soundVolume(), configuration.soundPitch());
        }
        if (shopCatalogService != null) {
            bedrockShopService = new BedrockShopService(configuration, shopCatalogService, detector, soundFeedback);
        }
    }

    private BedrockGUIApi getApiSafely() {
        try {
            return BedrockGUIApi.getInstance();
        } catch (IllegalStateException e) {
            getLogger().warning("BedrockGUI-Paper not found. Shop actions unavailable until it loads.");
            return null;
        }
    }
}
```

Note: action handlers registered in `onEnable` capture the `bedrockShopService` instance; after `reloadConfiguration` replaces that instance, the handlers still reference the old one. This matches the essentials-addon behavior (essentials re-creates services on reload too and accepts this). A full re-registration on reload is out of scope for Plan 1; document as a known limitation.

- [ ] **Step 3: Verify compile**

Run: `./gradlew :bedwars-addon:compileJava --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Build the shaded jar**

Run: `./gradlew :bedwars-addon:shadowJar --console=plain`
Expected: `BUILD SUCCESSFUL`; jar at `bedwars-addon/build/libs/BedrockGUI-BedwarsAddon.jar`.

- [ ] **Step 5: Commit**

```bash
git add bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/command/BedwarsAddonCommand.java bedwars-addon/src/main/java/it/pintux/life/bedwarsaddon/BedrockBedwarsAddonPlugin.java
git commit -m "feat(bedwars-addon): wire main plugin + reload command"
```

---

### Task 17: Manual verification on a live server

**Files:** none (manual).

No automated coverage is possible for the Bukkit/BedWars2023 integration. Run this matrix on a Paper 1.20.x server with BedWars2023, Geyser, Floodgate, and BedrockGUI-Paper installed alongside the built `BedrockGUI-BedwarsAddon.jar`.

- [ ] **Step 1:** Start the server; confirm console shows `Shop provider: BedWars2023` and `Registered bedwars addon shop actions`.
- [ ] **Step 2:** Join an arena and start a game as a **Bedrock** player (via Geyser). Right-click the shop villager. Expected: a Cumulus form opens (category list), NOT the chest GUI.
- [ ] **Step 3:** Tap a category. Expected: a form listing that category's items with cost labels; unaffordable items use the unaffordable template.
- [ ] **Step 4:** Tap an affordable item. Expected: purchase succeeds, currency is deducted, item appears in inventory, success message + sound.
- [ ] **Step 5:** Tap an unaffordable item. Expected: failure message + fail sound, no currency change.
- [ ] **Step 6:** As a **Java** player, right-click the shop villager. Expected: the native chest shop opens unchanged.
- [ ] **Step 7:** Run `/bedwarsaddon reload`. Expected: "configuration reloaded"; shop still works.
- [ ] **Step 8:** Set `modules.shop: false`, reload/restart. Expected: log says modules disabled, no interception.
- [ ] **Step 9:** Record any deviations as new tasks/issues. If any provider method name differed from the plan, ensure Task 12/15 commits captured the correction.

---

## Self-review

**Spec coverage (foundation + shop scope):**
- Module scaffold + gradle wiring → Task 1 ✓
- Provider abstraction (`ShopProvider`) → Task 4 ✓
- Bedrock detection → Task 5 ✓
- Config + module toggles + sounds → Tasks 6, 7 ✓
- Flat category list layout → Task 9 (`categoryButtons` one-per-category) ✓
- Live state read per-open → Task 12 (`getCategoryContents` computes affordability live) ✓
- Intercept-and-replace (event-based) → Task 15 (`ShopOpenEvent`) ✓
- Purchase via API → Task 12 (`execute`) + Task 13 routing ✓
- Java players unaffected → Task 15 (`shouldHandle` guard) + Task 17 step 6 ✓
- Fail-soft (no module / provider absent / API not loaded) → Task 16 ✓
- `/bedwarsaddon reload` → Task 16 ✓
- TDD on testable logic → Tasks 8, 9 ✓
- Manual verification matrix → Task 17 ✓
- Deferred (Plans 2–5): upgrades, arena, stats, party, spectator — explicitly out of scope, noted at top.

**Placeholder scan:** No "TBD"/"implement later". The two genuine unknowns (`ICategoryContent.execute` signature, jitpack tag) are handled by an explicit spike (Task 11) with `javap` verification commands and expected signatures, not left vague.

**Type consistency:** `ShopProvider` methods (`getCategories`, `getCategoryContents`, `purchase`, `ownsInventory`) match across `FakeShopProvider` (Task 4), `ShopCatalogService` (Task 10), `BedWars2023ShopProvider` (Task 12). DTO field names (`id`, `name`, `cost`, `currency`, `affordable`, `tier`) consistent between Task 3 definitions, Task 9 model usage, and Task 12 construction. Action types (`bw_shop_main`/`bw_shop_cat`/`bw_shop_buy`) consistent between Task 9 (string emission) and Task 14 (`getActionType`). `BedwarsActionPayloads.encode/decode` consistent across Tasks 8, 9, 14.

**Known limitations documented:** reload does not re-register action handlers (Task 16 note) — matches essentials behavior, acceptable for Plan 1.
