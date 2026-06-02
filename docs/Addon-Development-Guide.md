# BedrockGUI Addon Development Guide

This guide is the single reference for building a new BedrockGUI **addon** — a separate plugin/module
that gives Bedrock (Geyser/Floodgate) players native Cumulus **forms** in place of another plugin's
chest GUIs, and/or registers custom **actions** with BedrockGUI.

It captures the BedrockGUI API surface and the proven addon recipe so you don't have to re-read the
core code or the API each time. The `essentials-addon` and `bedwars-addon` modules are the reference
implementations.

---

## 1. What an addon is

An addon is a normal Paper plugin that:

1. Depends on `:common` (the BedrockGUI API) and the **target plugin's** API at compile time only.
2. Detects Bedrock players (via Floodgate) and shows them Cumulus forms.
3. Either **intercepts** a native chest GUI / command and replaces it with a form, or registers
   **action handlers** that other forms (or `config.yml` menus) can call.
4. Talks to the target plugin behind a small **provider interface**, so support for additional target
   plugins is added by writing another provider — not by touching the form/UI code.

You never bundle the BedrockGUI API or the target plugin into your jar; both are present at runtime.

---

## 2. Module setup

### settings.gradle

Add the module:

```groovy
include 'my-addon'
```

### gradle.properties

Centralize versions here (one place for all modules):

```properties
myAddonVersion=1.0.0
targetPluginVersion=1.2.3
junitVersion=5.10.2
mockitoVersion=5.11.0
```

### my-addon/build.gradle

```groovy
version = myAddonVersion

// Declare ONLY the repositories this module needs. Override the inherited list if a shared
// repository is down or pulls in conflicting artifacts.
repositories.clear()
repositories {
    mavenCentral()
    maven { url = "https://repo.papermc.io/repository/maven-public/" }
    maven { url = "https://repo.opencollab.dev/main/" }      // floodgate
    maven { url = "<target-plugin-repo-url>" }
}

dependencies {
    compileOnly project(':common')
    compileOnly "io.papermc.paper:paper-api:${paperApiVersion}"

    // Target plugin API — compile-only. Use transitive=false if its POM drags server-only deps.
    compileOnly("group:target-api:${targetPluginVersion}") { transitive = false }

    // Tests (this module only — the rest of the repo has no test setup)
    testImplementation "org.junit.jupiter:junit-jupiter:${junitVersion}"
    testImplementation "org.mockito:mockito-core:${mockitoVersion}"
    testRuntimeOnly "org.junit.platform:junit-platform-launcher"
    testImplementation "io.papermc.paper:paper-api:${paperApiVersion}" // compileOnly does NOT reach tests
}

test { useJUnitPlatform() }

tasks.withType(JavaCompile).configureEach { dependsOn(":common:jar") }

processResources {
    def props = [version: version]
    inputs.properties props
    filteringCharset 'UTF-8'
    filesMatching('plugin.yml') { expand props }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

shadowJar {
    minimize()
    archiveClassifier.set('')
    archiveBaseName.set('BedrockGUI-MyAddon')
}
```

> **compileOnly does not propagate to the test classpath.** If your tests use Bukkit/`YamlConfiguration`,
> add `paper-api` as `testImplementation` too.

> **Resolving a target plugin API:** confirm the live Maven repo + coordinates (some authors move
> repos). If the published jar drags server-only transitive deps you don't have, mark it
> `transitive = false`. Verify the exact method signatures you'll call with `javap`:
> `javap -classpath <api.jar> some.package.SomeInterface` — don't assume; APIs differ across versions
> and forks.

### plugin.yml

```yaml
name: BedrockGUI-MyAddon
version: '${version}'
main: com.example.myaddon.MyAddonPlugin
api-version: '1.20'
softdepend: [TargetPlugin, floodgate, BedrockGUI-Paper]
commands:
  myaddon:
    description: My addon admin command
    usage: /myaddon reload
```

Use `softdepend` (not `depend`) so the addon loads even if a piece is missing, and fail soft at runtime.

---

## 3. The BedrockGUI API surface

Obtain the API (never construct it):

```java
BedrockGUIApi api;
try {
    api = BedrockGUIApi.getInstance();   // throws IllegalStateException if BedrockGUI-Paper isn't loaded yet
} catch (IllegalStateException e) {
    // log + skip; the API isn't available
}
```

### Building forms

`BedrockGUIApi` builds three Cumulus form types. All take a `FormPlayer` to `send(...)`.

```java
// Simple form: title + content + buttons
BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm("Title");
form.content("Body text");
form.button("Click me", fp -> { /* onClick: fp is the FormPlayer */ });
form.button("With image", "https://url/icon.png", fp -> { ... });
form.send(formPlayer);

// Modal form: title + content + exactly two buttons (good for read-only screens / confirmations)
BedrockGUIApi.ModalFormBuilder modal = api.createModalForm("Title", "Body text");
modal.button1("Close", fp -> {});
modal.button2("Refresh", fp -> reopen(fp));
modal.send(formPlayer);

// Custom form: inputs / toggles / dropdowns / sliders
api.createCustomForm("Title")
   .input("Player name", "placeholder", "")
   .onSubmit((fp, results) -> {
        String value = String.valueOf(results.values().iterator().next());
        ...
   })
   .send(formPlayer);
```

Button images: a URL (`https://…`) or a Bedrock texture path (`textures/…`) — resolved automatically.

### FormPlayer

Forms operate on `FormPlayer` (platform-neutral), not Bukkit `Player`. Wrap and unwrap:

```java
// Bukkit Player -> FormPlayer
form.send(new BukkitFormPlayer(player));

// FormPlayer -> Bukkit Player (inside an action/button handler)
Player player = FormPlayerResolver.resolve(formPlayer);
```

Copy `BukkitFormPlayer` and `FormPlayerResolver` from a reference addon (they're tiny adapters).

### Bedrock detection

Only intercept/serve forms for Bedrock players. Java players keep the native GUI.

```java
public interface BedrockPlayerDetector { boolean isBedrockPlayer(Player player); }

// Floodgate implementation:
FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
```

Wrap it in a try/catch and behind your own interface so it's swappable and null-safe.

### Checking form capability

```java
api.canReceiveForms(formPlayer);   // true only for Bedrock players
```

---

## 4. The action system

Actions let a button (or a `config.yml` menu) trigger logic by a string `type:value`.

### Registering a handler

```java
public final class OpenMyMenuAction implements ActionSystem.ActionHandler {
    private final BedrockMyService service;
    public OpenMyMenuAction(BedrockMyService service) { this.service = service; }

    @Override public String getActionType() { return "my_open"; }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {
        Player p = FormPlayerResolver.resolve(player);
        if (p == null) return ActionSystem.ActionResult.failure("Bukkit player context is unavailable");
        service.open(p, actionValue);
        return ActionSystem.ActionResult.success("Opened");
    }

    @Override public boolean isValidAction(String actionValue) { return true; }
    @Override public String getDescription() { return "Opens my menu"; }
    @Override public String[] getUsageExamples() { return new String[]{"my_open:"}; }
}

// register once, after the API is available:
api.registerActionHandler(new OpenMyMenuAction(service));
```

### Invoking an action from a button

```java
form.button(label, fp -> api.executeActionString(fp, "my_open:" + payload, context("my-menu")));

private ActionSystem.ActionContext context(String source) {
    return ActionSystem.ActionContext.builder()
        .menuName(source)
        .formType("bedrock-my")
        .build();
}
```

### Encoding payloads

Action strings split on the first `:`. If your value can contain `:` or spaces (ids, names),
Base64-encode it so it survives:

```java
// encode when building the action string, decode in the handler
Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
Base64.getUrlDecoder().decode(encoded);
```

`ActionResult` messages and the action `description`/`usageExamples` are internal — they are **not**
shown to players. Player-facing text must go through your config (see §6).

---

## 5. The recommended architecture

This is the structure used by the reference addons. It isolates target-plugin coupling and keeps the
UI logic unit-testable.

```
my-addon/src/main/java/com/example/myaddon/
  MyAddonPlugin.java          wires everything; detects backend; registers actions + listeners
  api/<Feature>Provider.java  per-feature contract — returns plain DTOs (no target-plugin types leak out)
  api/BedrockPlayerDetector.java
  model/*.java                DTO records (ShopItem, ArenaInfo, ...)
  menu/MenuButton.java        record(label, actionString)
  menu/<Feature>MenuModel.java PURE: DTOs + config -> List<MenuButton> / content string (unit-tested)
  provider/<Plugin><Feature>Provider.java  the ONLY classes importing the target plugin's types
  provider/<Plugin>ApiAccess.java          resolves the target plugin's API handle
  provider/FloodgateBedrockPlayerDetector.java
  service/<Feature>CatalogService.java     holds the active provider; null/ready guards
  service/Bedrock<Feature>Service.java     builds forms from the menu-model; handles flow
  action/*.java               thin ActionHandlers routing to the service
  listener/*.java             interception listeners (see §7)
  config/MyAddonConfiguration.java   module toggles + every player-facing string + render()
  util/{BukkitFormPlayer,FormPlayerResolver,ActionPayloads,SoundFeedback}.java
```

**Layering rules**

- **Provider** is the *only* place that imports the target plugin's classes. It returns DTOs. This is
  what makes multi-plugin support cheap: add another provider, change nothing else.
- **CatalogService** holds the active provider and guards readiness (`isReady()` → provider present).
- **Menu-model** is pure (no Bukkit, no `BedrockGUIApi`): it turns DTOs + config into ordered
  `MenuButton`s (label + action string) or a content string. **This is your unit-test surface.**
- **Bedrock\<Feature\>Service** wires the menu-model into `BedrockGUIApi` forms and re-opens forms to
  refresh after an action.
- **Action handlers** are thin: resolve the player, decode the payload, call the service.

### Live vs. cached data

If the target data is static (warps, categories), cache it in the catalog service. If it changes
constantly (player balance, owned tiers, arena state), read it **live** each time the form opens, and
re-open the form after a purchase/change to refresh.

---

## 6. Configuration: everything player-facing is configurable

No player-facing string is hardcoded. Put titles, button labels, content, and messages in `config.yml`
and load them into a configuration object with a `render` helper:

```java
public String render(String template, Map<String, String> values) {
    String out = template == null ? "" : template;
    for (var e : values.entrySet()) out = out.replace("{" + e.getKey() + "}", e.getValue());
    return ChatColor.translateAlternateColorCodes('&', out);
}
```

Conventions:

- **Module toggles**: `modules.<feature>: true` — let admins disable parts.
- **Templates with placeholders** (e.g. `"{cost} {currency}"`) are stored raw and colored by `render`.
- **Static labels/messages** are colored at load.
- Provide rich **code defaults** (`getString(path, default)`) so the addon works even if the server's
  `config.yml` predates a newly added key (`saveDefaultConfig()` never overwrites an existing file).
- Console `getLogger()` output is operational, not player-facing — it does not need to be configurable.

---

## 7. Interception strategies

To replace a native chest GUI with a form, you must detect it opening and cancel it for Bedrock
players. Pick the cleanest mechanism the target plugin allows (in order of preference):

1. **Dedicated cancellable open-event** *(best)* — e.g. a `ShopOpenEvent`. Listen at `HIGH`, if the
   player is Bedrock, `setCancelled(true)` and open your form on the next tick:
   ```java
   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onOpen(SomeShopOpenEvent e) {
       if (!service.shouldHandle(e.getPlayer())) return;
       e.setCancelled(true);
       plugin.getServer().getScheduler().runTask(plugin, () -> service.open(e.getPlayer()));
   }
   ```

2. **`InventoryOpenEvent` + custom holder** — if the GUI uses a recognizable `InventoryHolder`:
   ```java
   if (event.getInventory().getHolder() != null
       && event.getInventory().getHolder().getClass().getName().contains("TheirHolder")) { ... }
   ```
   (Match by class-name when the holder type is plugin-internal and not on your classpath.)

3. **`InventoryOpenEvent` + title match** — if the GUI uses a `null` holder, match
   `event.getView().getTitle()` against a **configurable** substring. Title is available at open time,
   so you can cancel directly (no flash). Make the substring a config key (locales differ).

4. **`InventoryOpenEvent` + next-tick state flag** — if the plugin sets its "is-open" flag *after*
   `openInventory` (so it's not set during the event), schedule a next-tick check; if the flag is now
   set, close the chest and open your form. (One-tick chest flash is possible.)

5. **Trigger interception** — `PlayerInteractEntityEvent` on the NPC, or
   `PlayerCommandPreprocessEvent` for a command. Use when there's no usable inventory signal.

6. **Command-opened form** — when no GUI exists to intercept (e.g. command-only features), expose a
   `/myaddon <feature>` entry point that opens the form directly.

### The backing-inventory caveat

Some plugins' "buy" methods write the purchased item into the player's **currently open inventory**
(e.g. `player.getOpenInventory().getTopInventory().setItem(slot, …)`). If you cancelled the chest,
that write can throw `ArrayIndexOutOfBoundsException` (the player's top inventory is the 2×2 crafting
grid) — possibly *after* taking money. Two options:

- Prefer a buy API that has an "open inventory" / "refresh" flag you can pass as **false**.
- Otherwise, open a throwaway sized inventory around the buy call so the write lands harmlessly:
  ```java
  player.openInventory(Bukkit.createInventory(player, 54));
  try { content.buy(player, ...); } finally { player.closeInventory(); }
  ```

### Don't double-handle messages/sounds

If the target plugin's buy method already sends "purchased"/"insufficient funds" messages and sounds,
don't send your own — let it. Only send your own messages for cases the plugin won't (e.g. provider
unavailable).

---

## 8. Form layout patterns

- **Flat list**: one `SimpleForm` button per item + a Close button.
- **Category → items**: a category `SimpleForm`; each button opens an item sub-form; include a Back
  button (an action that re-opens the parent).
- **Read-only screen**: a `ModalForm` with the data in the body and Close / Refresh buttons.
- **Text input**: a `CustomForm` with an `input`; read the value in `onSubmit`, then re-open the parent.
- **Pagination**: page the list at a fixed size, with Prev/Next buttons carrying the page number in the
  action payload.
- **Refresh after change**: re-open the same form after a purchase/action so live values update.

---

## 9. Supporting multiple target plugins

Keep one provider interface per feature; write one provider impl per target plugin. At startup, detect
which plugin is installed and wire the matching providers (and the matching typed listeners):

```java
private String detectBackend(PluginManager pm) {
    if (pm.getPlugin("PluginA") != null) return "PluginA";
    if (pm.getPlugin("PluginB") != null) return "PluginB";
    return null;
}
// then per feature: catalog.setProvider(switch on backend);
```

- **Services, menu-models, actions, config are backend-agnostic** — written once.
- **Listeners that reference a plugin-typed event** (e.g. `PluginA`'s `ShopOpenEvent`) must be
  per-plugin; register only the one matching the detected backend.
- **Forks share APIs**: a fork often mirrors the parent's package (`com.parent.x` ↔ `com.fork.x`).
  Re-`javap` though — forks add/rename methods (e.g. a buy method present in one and not the other).
- **When the public API lacks what you need**, you can reflect into internals as a last resort: guard
  every reflective call, log failures, and degrade gracefully. Treat it as experimental and
  version-fragile. If even the internals offer no callable entry point (purely event/click-driven),
  prefer leaving that feature to the native (Geyser-translated) GUI rather than shipping unreliable code.

---

## 10. Testing

The pure **menu-model** is the unit-test target — it needs no server:

```java
// Build config from in-memory YAML via the private constructor (reflection), feed DTOs, assert buttons.
var model = new MyMenuModel(config);
List<MenuButton> buttons = model.itemButtons(List.of(new ShopItem("id", "Name", 4, "Iron", true)));
assertEquals("Name - 4 Iron", buttons.get(0).label());
assertEquals("my_buy:" + ActionPayloads.encode("id"), buttons.get(0).actionString());
```

Keep provider impls "dumb" (pure translation between the target API and your DTOs) so the testable
logic lives in the menu-model and services. Provider impls and listeners are verified manually on a
live server with the target plugin + Geyser/Floodgate installed.

---

## 11. Main plugin wiring (shape)

```java
public final class MyAddonPlugin extends JavaPlugin {
    @Override public void onEnable() {
        config = MyAddonConfiguration.load(this);
        if (noModulesEnabled()) { getLogger().info("All modules disabled."); return; }

        detector = new FloodgateBedrockPlayerDetector();
        String backend = detectBackend(getServer().getPluginManager());

        // per feature: catalogService = new ...; set provider by backend; bedrockService = new ...;
        // register the matching interception listener(s).

        getCommand("myaddon").setExecutor(new MyAddonCommand(this));

        BedrockGUIApi api = getApiSafely();   // null-safe getInstance()
        if (api != null) {
            api.registerActionHandler(new OpenMyMenuAction(service));
            // ... register all action handlers
        }
    }

    public void reloadConfiguration() {
        config = MyAddonConfiguration.load(this);
        // rebuild services with the new config (providers/catalogs persist)
    }
}
```

---

## 12. New-addon checklist

- [ ] Module added to `settings.gradle`; versions in `gradle.properties`; `build.gradle` from the template.
- [ ] `plugin.yml` with `softdepend: [TargetPlugin, floodgate, BedrockGUI-Paper]` and a command.
- [ ] Target API resolved; signatures confirmed with `javap`.
- [ ] Per-feature provider interface (DTO-based) + at least one impl; `ApiAccess` resolver.
- [ ] Floodgate detector; `BukkitFormPlayer` + `FormPlayerResolver` copied.
- [ ] Pure menu-model + JUnit tests.
- [ ] Catalog service + Bedrock service building forms via `BedrockGUIApi`.
- [ ] Action handlers (Base64-encode ids in payloads).
- [ ] Interception listener using the cleanest available strategy (§7).
- [ ] Config: module toggles + **every** player-facing string, with rich code defaults.
- [ ] Main plugin wiring + `reload`; action registration null-safe.
- [ ] Built; tests pass; verified on a live server.

---

The `essentials-addon` (command-interception + shop bridges) and `bedwars-addon` (event/holder/title
interception, multi-backend, live data) modules are complete worked examples of everything above.
