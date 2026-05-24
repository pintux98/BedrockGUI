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

        List<String> homes = homeCatalog.getAccessibleHomes(player);
        if (homes.isEmpty()) {
            player.sendMessage(configuration.noHomesMessage());
            return;
        }

        int totalPages = (int) Math.ceil((double) homes.size() / ITEMS_PER_PAGE);
        int currentPage = Math.max(1, Math.min(page, totalPages));

        int start = (currentPage - 1) * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, homes.size());

        int count = homeCatalog.getHomeCount(player);
        int max = homeCatalog.getMaxHomes(player);
        String limitText = max > 0
                ? configuration.render(configuration.homeLimitText(), Map.of("count", String.valueOf(count), "max", String.valueOf(max)))
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

        List<String> homes = homeCatalog.getAccessibleHomes(player);
        if (!homes.contains(homeName)) {
            player.sendMessage(configuration.render(configuration.homeNotFound(), Map.of("home_name", homeName)));
            return;
        }

        boolean success = homeCatalog.teleportHome(player, homeName);
        if (success) {
            player.sendMessage(configuration.render(configuration.homeTeleportSuccess(), Map.of("home_name", homeName)));
        } else {
            player.sendMessage(configuration.render(configuration.homeTeleportFailed(), Map.of("home_name", homeName)));
        }
    }

    public void showSetHomeForm(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!ensureHomeCatalog(player)) return;

        int count = homeCatalog.getHomeCount(player);
        int max = homeCatalog.getMaxHomes(player);
        if (max > 0 && count >= max) {
            player.sendMessage(configuration.render(configuration.homeLimitReached(),
                    Map.of("count", String.valueOf(count), "max", String.valueOf(max))));
            return;
        }

        api.createCustomForm(configuration.homeTitle())
                .input(configuration.homeContent(), "home_name", "")
                .onSubmit((p, results) -> {
                    String homeName = (String) results.get("home_name");
                    if (homeName == null || homeName.trim().isEmpty()) {
                        Player bukkitPlayer = FormPlayerResolver.resolve(p);
                        if (bukkitPlayer != null) bukkitPlayer.sendMessage(configuration.homeSetInvalid());
                        return;
                    }
                    homeName = homeName.trim();
                    Player bukkitPlayer = FormPlayerResolver.resolve(p);
                    if (bukkitPlayer != null && homeCatalog.setHome(bukkitPlayer, homeName)) {
                        bukkitPlayer.sendMessage(configuration.render(configuration.homeSetSuccess(), Map.of("home_name", homeName)));
                    } else if (bukkitPlayer != null) {
                        bukkitPlayer.sendMessage(configuration.homeSetFailed());
                    }
                })
                .send(new BukkitFormPlayer(player));
    }

    public void showDeleteHomeForm(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!ensureHomeCatalog(player)) return;

        List<String> homes = homeCatalog.getAccessibleHomes(player);
        if (homes.isEmpty()) {
            player.sendMessage(configuration.homeNoDelete());
            return;
        }

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(configuration.homeTitle());
        form.content(configuration.homeContent());

        for (String homeName : homes) {
            String buttonText = configuration.render(configuration.homeDeleteButton(), Map.of("home_name", homeName));
            form.button(buttonText, formPlayer -> {
                Player bukkitPlayer = FormPlayerResolver.resolve(formPlayer);
                if (bukkitPlayer != null && homeCatalog.deleteHome(bukkitPlayer, homeName)) {
                    bukkitPlayer.sendMessage(configuration.render(configuration.homeDeleteSuccess(), Map.of("home_name", homeName)));
                    openHomeMenu(bukkitPlayer, 1);
                } else if (bukkitPlayer != null) {
                    bukkitPlayer.sendMessage(configuration.homeDeleteFailed());
                }
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
