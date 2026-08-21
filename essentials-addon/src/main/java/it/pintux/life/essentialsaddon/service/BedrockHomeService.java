package it.pintux.life.essentialsaddon.service;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.essentialsaddon.api.BedrockPlayerDetector;
import it.pintux.life.essentialsaddon.config.EssentialsAddonConfiguration;
import it.pintux.life.essentialsaddon.util.BukkitFormPlayer;
import it.pintux.life.essentialsaddon.util.EssentialsActionPayloads;
import it.pintux.life.essentialsaddon.util.FormPlayerResolver;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public final class BedrockHomeService {
    private final EssentialsAddonConfiguration configuration;
    private final HomeCatalogService homeCatalog;
    private final BedrockPlayerDetector bedrockPlayerDetector;
    private static final int ITEMS_PER_PAGE = 18;

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
        api.createCustomForm(configuration.homeTitle())
                .namedInput("home_name", configuration.homeNameInputText(),
                        configuration.homeNameInputPlaceholder(), "")
                .onSubmit((p, results) -> {
                    String homeName = (String) results.get("home_name");
                    Player bukkitPlayer = FormPlayerResolver.resolve(p);
                    if (bukkitPlayer == null) return;
                    if (homeName == null || homeName.trim().isEmpty()) {
                        bukkitPlayer.sendMessage(configuration.homeSetInvalid());
                        return;
                    }
                    String trimmed = homeName.trim();
                    homeCatalog.setHome(bukkitPlayer, trimmed, success -> {
                        if (success) {
                            bukkitPlayer.sendMessage(configuration.render(configuration.homeSetSuccess(), Map.of("home_name", trimmed)));
                        } else {
                            bukkitPlayer.sendMessage(configuration.homeSetFailed());
                        }
                    });
                })
                .send(new BukkitFormPlayer(player));
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
