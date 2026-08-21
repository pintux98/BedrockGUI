package it.pintux.life.essentialsaddon.service;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.essentialsaddon.api.BedrockPlayerDetector;
import it.pintux.life.essentialsaddon.config.EssentialsAddonConfiguration;
import it.pintux.life.essentialsaddon.model.HomeView;
import it.pintux.life.essentialsaddon.model.PublicHomeView;
import it.pintux.life.essentialsaddon.util.BukkitFormPlayer;
import it.pintux.life.essentialsaddon.util.EssentialsActionPayloads;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import it.pintux.life.essentialsaddon.util.MainThread;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public final class BedrockHomeService {
    private final EssentialsAddonConfiguration configuration;
    private final HomeCatalogService homeCatalog;
    private final BedrockPlayerDetector bedrockPlayerDetector;
    private static final int ITEMS_PER_PAGE = 18;
    // HuskHomes writes privacy asynchronously, so give it a tick before reading it back.
    private static final long PRIVACY_REFRESH_TICKS = 10L;

    public BedrockHomeService(
            EssentialsAddonConfiguration configuration,
            HomeCatalogService homeCatalog,
            BedrockPlayerDetector bedrockPlayerDetector
    ) {
        this.configuration = configuration;
        this.homeCatalog = homeCatalog;
        this.bedrockPlayerDetector = bedrockPlayerDetector;
    }

    public boolean shouldHandle(Player player) {
        return player != null && bedrockPlayerDetector.isBedrockPlayer(player);
    }

    public void openHomeMenu(Player player, int page) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!ensureHomeCatalog(player)) return;

        homeCatalog.homeNames(player, homes ->
                homeCatalog.homeLimit(player, max -> renderHomeMenu(player, api, page, homes, max)));
    }

    private void renderHomeMenu(Player player, BedrockGUIApi api, int page, List<String> homes, int max) {
        if (homes.isEmpty()) {
            player.sendMessage(configuration.noHomesMessage());
            return;
        }

        int totalPages = (int) Math.ceil((double) homes.size() / ITEMS_PER_PAGE);
        int currentPage = Math.max(1, Math.min(page, totalPages));

        int start = (currentPage - 1) * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, homes.size());

        String limitText = max > 0
                ? configuration.render(configuration.homeLimitText(),
                        Map.of("count", String.valueOf(homes.size()), "max", String.valueOf(max)))
                : "";

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(configuration.homeTitle() + limitText);
        form.content(configuration.homeContent());

        for (int i = start; i < end; i++) {
            String homeName = homes.get(i);
            String buttonText = configuration.render(configuration.homeButton(), Map.of("home_name", homeName));
            form.button(buttonText, formPlayer ->
                    api.executeActionString(formPlayer,
                            "home_teleport:" + EssentialsActionPayloads.encodeHome(homeName),
                            context("home-menu", homeName)));
        }

        if (currentPage > 1) {
            form.button(configuration.previousButton(), formPlayer ->
                    api.executeActionString(formPlayer,
                            "home_main:" + (currentPage - 1),
                            context("home-prev", "")));
        }

        if (manageMenuEnabled()) {
            form.button(configuration.homeManageButton(), formPlayer ->
                    api.executeActionString(formPlayer, "home_manage_main:",
                            context("home-manage", "")));
        }

        form.button(configuration.mainButton(), formPlayer ->
                api.executeActionString(formPlayer, "essentials_hub:",
                        context("home-main", "")));

        if (currentPage < totalPages) {
            form.button(configuration.nextButton(), formPlayer ->
                    api.executeActionString(formPlayer,
                            "home_main:" + (currentPage + 1),
                            context("home-next", "")));
        }

        form.send(new BukkitFormPlayer(player));
    }

    /** Public homes need both the provider's support and the config switch. */
    public boolean supportsPublicHomes() {
        return configuration.homePublicHomesEnabled() && homeCatalog.supportsPublicHomes();
    }

    public boolean manageMenuEnabled() {
        return configuration.homeManageMenuEnabled();
    }

    private boolean privacyAvailable() {
        return configuration.homePrivacyEnabled() && homeCatalog.supportsPublicHomes();
    }

    private boolean renameAvailable() {
        return configuration.homeRenameEnabled() && homeCatalog.supportsRename();
    }

    public void openPublicHomeMenu(Player player, int page) {
        if (!supportsPublicHomes()) return;
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!ensureHomeCatalog(player)) return;

        homeCatalog.publicHomes(player, homes -> renderPublicHomeMenu(player, api, page, homes));
    }

    private void renderPublicHomeMenu(Player player, BedrockGUIApi api, int page, List<PublicHomeView> homes) {
        if (homes.isEmpty()) {
            player.sendMessage(configuration.noPublicHomesMessage());
            return;
        }

        int totalPages = (int) Math.ceil((double) homes.size() / ITEMS_PER_PAGE);
        int currentPage = Math.max(1, Math.min(page, totalPages));
        int start = (currentPage - 1) * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, homes.size());

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(configuration.publicHomeTitle());
        form.content(configuration.publicHomeContent());

        for (int i = start; i < end; i++) {
            PublicHomeView home = homes.get(i);
            String buttonText = configuration.render(configuration.publicHomeButton(),
                    Map.of("home_name", home.name(), "owner", home.owner(), "identifier", home.identifier()));
            form.button(buttonText, formPlayer ->
                    api.executeActionString(formPlayer,
                            "public_home_teleport:" + EssentialsActionPayloads.encodeHome(home.identifier()),
                            context("public-home-menu", home.identifier())));
        }

        if (currentPage > 1) {
            form.button(configuration.previousButton(), formPlayer ->
                    api.executeActionString(formPlayer,
                            "public_home_main:" + (currentPage - 1),
                            context("public-home-prev", "")));
        }

        form.button(configuration.mainButton(), formPlayer ->
                api.executeActionString(formPlayer, "essentials_hub:",
                        context("public-home-main", "")));

        if (currentPage < totalPages) {
            form.button(configuration.nextButton(), formPlayer ->
                    api.executeActionString(formPlayer,
                            "public_home_main:" + (currentPage + 1),
                            context("public-home-next", "")));
        }

        form.send(new BukkitFormPlayer(player));
    }

    public void teleportPublicHome(Player player, String identifier) {
        if (!supportsPublicHomes() || !ensureHomeCatalog(player)) return;

        homeCatalog.teleportPublicHome(player, identifier, success -> {
            Map<String, String> values = Map.of(
                    "home_name", publicHomeName(identifier),
                    "owner", publicHomeOwner(identifier),
                    "identifier", identifier);
            player.sendMessage(configuration.render(success
                    ? configuration.publicHomeTeleportSuccess()
                    : configuration.publicHomeTeleportFailed(), values));
        });
    }

    /**
     * Splits an owner-and-name identifier for the teleport messages, which only carry the
     * identifier. The separator comes from the provider, and the last one is the one that counts:
     * a Bedrock username carries a prefix that may be the separator itself, so cutting at the
     * first would leave the owner empty. A home name cannot contain the separator, so the tail is
     * always the name.
     */
    private String publicHomeName(String identifier) {
        int split = identifier.lastIndexOf(homeCatalog.publicHomeSeparator());
        int nameStart = split + homeCatalog.publicHomeSeparator().length();
        return split >= 0 && nameStart < identifier.length() ? identifier.substring(nameStart) : identifier;
    }

    private String publicHomeOwner(String identifier) {
        int split = identifier.lastIndexOf(homeCatalog.publicHomeSeparator());
        return split > 0 ? identifier.substring(0, split) : "";
    }

    public void showManageHomesForm(Player player) {
        if (!manageMenuEnabled()) return;
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!ensureHomeCatalog(player)) return;

        homeCatalog.homeDetails(player, homes -> {
            if (homes.isEmpty()) {
                player.sendMessage(configuration.noHomesMessage());
                return;
            }
            BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(configuration.homeManageTitle());
            form.content(configuration.homeManageContent());

            for (HomeView home : homes) {
                String label = configuration.render(configuration.homeButton(),
                        Map.of("home_name", home.name()))
                        + (home.isPublic() ? configuration.homePublicSuffix() : "");
                form.button(label, formPlayer ->
                        api.executeActionString(formPlayer,
                                "home_manage:" + EssentialsActionPayloads.encodeHome(home.name()),
                                context("home-manage-list", home.name())));
            }

            form.button(configuration.backButton(), formPlayer ->
                    api.executeActionString(formPlayer, "home_main:1", context("home-manage-back", "")));

            form.send(new BukkitFormPlayer(player));
        });
    }

    public void showHomeManageForm(Player player, String homeName) {
        if (!manageMenuEnabled()) return;
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!ensureHomeCatalog(player)) return;

        homeCatalog.homeDetails(player, homes -> {
            HomeView selected = null;
            for (HomeView candidate : homes) {
                if (candidate.name().equalsIgnoreCase(homeName)) {
                    selected = candidate;
                    break;
                }
            }
            if (selected == null) {
                player.sendMessage(configuration.render(configuration.homeNotFound(), Map.of("home_name", homeName)));
                return;
            }
            renderHomeManageForm(player, api, selected);
        });
    }

    private void renderHomeManageForm(Player player, BedrockGUIApi api, HomeView home) {
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(
                configuration.homeManageTitle() + (home.isPublic() ? configuration.homePublicSuffix() : ""));
        form.content(configuration.render(configuration.homeButton(), Map.of("home_name", home.name())));

        form.button(configuration.homeManageTeleportButton(), formPlayer ->
                api.executeActionString(formPlayer,
                        "home_teleport:" + EssentialsActionPayloads.encodeHome(home.name()),
                        context("home-manage-teleport", home.name())));

        if (privacyAvailable()) {
            String action = home.isPublic() ? "home_make_private:" : "home_make_public:";
            String label = home.isPublic()
                    ? configuration.homeMakePrivateButton()
                    : configuration.homeMakePublicButton();
            form.button(label, formPlayer ->
                    api.executeActionString(formPlayer,
                            action + EssentialsActionPayloads.encodeHome(home.name()),
                            context("home-manage-privacy", home.name())));
        }

        if (renameAvailable()) {
            form.button(configuration.homeRenameButton(), formPlayer ->
                    api.executeActionString(formPlayer,
                            "home_rename:" + EssentialsActionPayloads.encodeHome(home.name()),
                            context("home-manage-rename", home.name())));
        }

        form.button(configuration.homeManageDeleteButton(), formPlayer ->
                api.executeActionString(formPlayer,
                        "home_delete_confirm:" + EssentialsActionPayloads.encodeHome(home.name()),
                        context("home-manage-delete", home.name())));

        form.button(configuration.backButton(), formPlayer ->
                api.executeActionString(formPlayer, "home_manage_main:", context("home-manage-back", "")));

        form.send(new BukkitFormPlayer(player));
    }

    public void showRenameHomeForm(Player player, String homeName) {
        if (!renameAvailable()) return;
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!ensureHomeCatalog(player)) return;

        api.createCustomForm(configuration.homeRenameTitle())
                .namedInput("new_name",
                        configuration.render(configuration.homeRenameInputText(), Map.of("home_name", homeName)),
                        configuration.homeNameInputPlaceholder(), homeName)
                .onSubmit((formPlayer, results) -> {
                    Player bukkitPlayer = FormPlayerResolver.resolve(formPlayer);
                    if (bukkitPlayer == null) return;
                    Object answer = results.get("new_name");
                    String newName = answer == null ? "" : answer.toString().trim();
                    if (newName.isEmpty()) {
                        bukkitPlayer.sendMessage(configuration.homeSetInvalid());
                        return;
                    }
                    if (newName.equalsIgnoreCase(homeName)) {
                        showHomeManageForm(bukkitPlayer, homeName);
                        return;
                    }
                    renameHome(bukkitPlayer, homeName, newName);
                })
                .send(new BukkitFormPlayer(player));
    }

    public void renameHome(Player player, String homeName, String newName) {
        if (!renameAvailable() || !ensureHomeCatalog(player)) return;

        homeCatalog.renameHome(player, homeName, newName, result -> {
            if (!result.success()) {
                if (!result.playerNotified()) {
                    player.sendMessage(configuration.render(configuration.homeRenameFailed(),
                            Map.of("home_name", homeName, "new_name", newName)));
                }
                return;
            }
            player.sendMessage(configuration.render(configuration.homeRenameSuccess(),
                    Map.of("home_name", homeName, "new_name", newName)));
            MainThread.runLater(() -> showHomeManageForm(player, newName), PRIVACY_REFRESH_TICKS);
        });
    }

    public void setHomePrivacy(Player player, String homeName, boolean isPublic) {
        setHomePrivacy(player, homeName, isPublic, false);
    }

    /**
     * @param reopenManageForm true only when the change came from the manage form, so the player
     *                         lands back where they pressed the button. Setting a home does not
     *                         reopen anything: HuskHomes applies the change asynchronously, and a
     *                         form built straight after the write would still read the old state.
     */
    public void setHomePrivacy(Player player, String homeName, boolean isPublic, boolean reopenManageForm) {
        if (!privacyAvailable() || !ensureHomeCatalog(player)) return;

        homeCatalog.setHomePrivacy(player, homeName, isPublic, result -> {
            if (!result.success()) {
                if (!result.playerNotified()) {
                    player.sendMessage(configuration.render(configuration.homePrivacyFailed(),
                            Map.of("home_name", homeName)));
                }
                return;
            }
            player.sendMessage(configuration.render(isPublic
                            ? configuration.homePrivacyPublicSuccess()
                            : configuration.homePrivacyPrivateSuccess(),
                    Map.of("home_name", homeName)));
            if (reopenManageForm) {
                MainThread.runLater(() -> showHomeManageForm(player, homeName), PRIVACY_REFRESH_TICKS);
            }
        });
    }

    public void deleteHome(Player player, String homeName) {
        if (!ensureHomeCatalog(player)) return;

        homeCatalog.deleteHome(player, homeName, success -> {
            if (success) {
                player.sendMessage(configuration.render(configuration.homeDeleteSuccess(), Map.of("home_name", homeName)));
                openHomeMenu(player, 1);
            } else {
                player.sendMessage(configuration.homeDeleteFailed());
            }
        });
    }

    public void teleportHome(Player player, String homeName) {
        if (!ensureHomeCatalog(player)) return;

        homeCatalog.homeNames(player, homes -> {
            if (!homes.contains(homeName)) {
                player.sendMessage(configuration.render(configuration.homeNotFound(), Map.of("home_name", homeName)));
                return;
            }
            homeCatalog.teleportHome(player, homeName, success -> {
                if (success) {
                    player.sendMessage(configuration.render(configuration.homeTeleportSuccess(), Map.of("home_name", homeName)));
                } else {
                    player.sendMessage(configuration.render(configuration.homeTeleportFailed(), Map.of("home_name", homeName)));
                }
            });
        });
    }

    public void showSetHomeForm(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!ensureHomeCatalog(player)) return;

        homeCatalog.homeNames(player, homes -> homeCatalog.homeLimit(player, max -> {
            if (max > 0 && homes.size() >= max) {
                player.sendMessage(configuration.render(configuration.homeLimitReached(),
                        Map.of("count", String.valueOf(homes.size()), "max", String.valueOf(max))));
                return;
            }
            renderSetHomeForm(player, api);
        }));
    }

    private void renderSetHomeForm(Player player, BedrockGUIApi api) {
        boolean privacyAvailable = privacyAvailable();
        BedrockGUIApi.CustomFormBuilder form = api.createCustomForm(configuration.homeTitle())
                .namedInput("home_name", configuration.homeNameInputText(),
                        configuration.homeNameInputPlaceholder(), "");
        if (privacyAvailable) {
            form.toggle(configuration.homeSetPublicToggle(), false);
        }
        form.onSubmit((p, results) -> {
            String homeName = (String) results.get("home_name");
            Player bukkitPlayer = FormPlayerResolver.resolve(p);
            if (bukkitPlayer == null) return;
            if (homeName == null || homeName.trim().isEmpty()) {
                bukkitPlayer.sendMessage(configuration.homeSetInvalid());
                return;
            }
            String trimmed = homeName.trim();
            boolean makePublic = privacyAvailable && isToggled(results);
            homeCatalog.setHome(bukkitPlayer, trimmed, result -> {
                if (!result.success()) {
                    if (!result.playerNotified()) {
                        bukkitPlayer.sendMessage(configuration.homeSetFailed());
                    }
                    return;
                }
                bukkitPlayer.sendMessage(configuration.render(configuration.homeSetSuccess(), Map.of("home_name", trimmed)));
                if (makePublic) {
                    setHomePrivacy(bukkitPlayer, trimmed, true);
                }
            });
        }).send(new BukkitFormPlayer(player));
    }

    /** The toggle is the only boolean on the set-home form, so its answer is found by type. */
    private boolean isToggled(Map<String, Object> results) {
        for (Object value : results.values()) {
            if (value instanceof Boolean flag) {
                return flag;
            }
        }
        return false;
    }

    public void showDeleteHomeForm(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!ensureHomeCatalog(player)) return;

        homeCatalog.homeNames(player, homes -> {
            if (homes.isEmpty()) {
                player.sendMessage(configuration.homeNoDelete());
                return;
            }
            renderDeleteHomeForm(player, api, homes);
        });
    }

    private void renderDeleteHomeForm(Player player, BedrockGUIApi api, List<String> homes) {
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(configuration.homeTitle());
        form.content(configuration.homeContent());

        for (String homeName : homes) {
            String buttonText = configuration.render(configuration.homeDeleteButton(), Map.of("home_name", homeName));
            form.button(buttonText, formPlayer -> {
                Player bukkitPlayer = FormPlayerResolver.resolve(formPlayer);
                if (bukkitPlayer == null) return;
                homeCatalog.deleteHome(bukkitPlayer, homeName, success -> {
                    if (success) {
                        bukkitPlayer.sendMessage(configuration.render(configuration.homeDeleteSuccess(), Map.of("home_name", homeName)));
                        openHomeMenu(bukkitPlayer, 1);
                    } else {
                        bukkitPlayer.sendMessage(configuration.homeDeleteFailed());
                    }
                });
            });
        }

        form.send(new BukkitFormPlayer(player));
    }

    private BedrockGUIApi requireApi(Player player) {
        try {
            BedrockGUIApi api = BedrockGUIApi.getInstance();
            if (api == null) {
                player.sendMessage(configuration.noBedrockGui());
            }
            return api;
        } catch (IllegalStateException e) {
            player.sendMessage(configuration.noBedrockGui());
            return null;
        }
    }

    private boolean ensureHomeCatalog(Player player) {
        if (!homeCatalog.isReady()) {
            homeCatalog.refresh();
        }
        if (!homeCatalog.isReady()) {
            player.sendMessage(configuration.homeProviderUnavailable());
            return false;
        }
        return true;
    }

    private ActionSystem.ActionContext context(String source, String metadata) {
        return ActionSystem.ActionContext.builder()
                .menuName(source)
                .formType("bedrock-homes")
                .metadata("feature", metadata)
                .build();
    }
}
