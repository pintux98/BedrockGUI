package it.pintux.life.essentialsaddon.service;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.essentialsaddon.config.EssentialsAddonConfiguration;
import it.pintux.life.essentialsaddon.util.BukkitFormPlayer;
import it.pintux.life.essentialsaddon.util.EssentialsActionPayloads;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public final class BedrockCrateOpeningService {
    private final EssentialsAddonConfiguration configuration;
    private final CrateCatalogService crateCatalog;
    private final BedrockPlayerDetector bedrockPlayerDetector;

    public BedrockCrateOpeningService(
            EssentialsAddonConfiguration configuration,
            CrateCatalogService crateCatalog,
            BedrockPlayerDetector bedrockPlayerDetector
    ) {
        this.configuration = configuration;
        this.crateCatalog = crateCatalog;
        this.bedrockPlayerDetector = bedrockPlayerDetector;
    }

    public boolean shouldHandle(Player player) {
        return player != null && bedrockPlayerDetector.isBedrockPlayer(player);
    }

    public void openCrateOpenMenu(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!ensureCrateCatalog(player)) return;

        List<String> crates = crateCatalog.getAccessibleCrates(player);
        if (crates.isEmpty()) {
            player.sendMessage("&cNo crates are currently available.");
            return;
        }

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm("&bOpen Crate");
        form.content("&7Select a crate to open.");

        for (String crateId : crates) {
            String displayName = crateCatalog.getDisplayName(crateId);
            form.button("&e" + displayName, formPlayer ->
                    api.executeActionString(formPlayer,
                            "crate_open:" + EssentialsActionPayloads.encodeCrate(crateId),
                            context("crate-open-menu", crateId)));
        }

        form.send(new BukkitFormPlayer(player));
    }

    public void openCrate(Player player, String crateId) {
        if (!ensureCrateCatalog(player)) return;

        if (!crateCatalog.getAccessibleCrates(player).contains(crateId)) {
            player.sendMessage("&cYou do not have permission to open this crate.");
            return;
        }

        boolean success = crateCatalog.getProvider().openPreview(player, crateId);
        if (success) {
            player.sendMessage("&aOpening crate: " + crateCatalog.getDisplayName(crateId));
        } else {
            player.sendMessage("&cFailed to open crate.");
        }
    }

    public void showCratePreviewWithOpen(Player player, String crateId) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!ensureCrateCatalog(player)) return;

        if (!crateCatalog.getAccessibleCrates(player).contains(crateId)) {
            player.sendMessage("&cYou do not have permission to preview this crate.");
            return;
        }

        String displayName = crateCatalog.getDisplayName(crateId);
        String description = crateCatalog.getDescription(crateId);

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm("&bPreview: " + displayName);
        if (!description.isEmpty()) {
            form.content("&7" + description);
        }

        List<ItemStack> contents = crateCatalog.getPreviewContents(crateId);
        if (contents.isEmpty()) {
            form.button("&cNo items in this crate", ignored -> {});
        } else {
            for (ItemStack item : contents) {
                String itemName = getItemDisplayName(item);
                String itemLore = getItemLore(item);
                String buttonText = "&e" + itemName;
                if (!itemLore.isEmpty()) {
                    buttonText += "\n&7" + itemLore;
                }
                form.button(buttonText, ignored -> {});
            }
        }

        form.button("&aOpen This Crate", formPlayer ->
                api.executeActionString(formPlayer,
                        "crate_open:" + EssentialsActionPayloads.encodeCrate(crateId),
                        context("crate-open", crateId)));

        form.button("&7Back to Crates", formPlayer ->
                api.executeActionString(formPlayer, "crate_main:",
                        context("crate-back", "")));

        form.send(new BukkitFormPlayer(player));
    }

    private BedrockGUIApi requireApi(Player player) {
        try {
            BedrockGUIApi api = BedrockGUIApi.getInstance();
            if (api == null) {
                player.sendMessage("&cBedrockGUI API is not available.");
            }
            return api;
        } catch (IllegalStateException e) {
            player.sendMessage("&cBedrockGUI API is not available.");
            return null;
        }
    }

    private boolean ensureCrateCatalog(Player player) {
        if (!crateCatalog.isReady()) {
            crateCatalog.refresh();
        }
        if (!crateCatalog.isReady()) {
            if (crateCatalog.getProvider() == null) {
                player.sendMessage("&cNo crate provider is available.");
            } else {
                player.sendMessage("&eThe crate backend is not loaded yet.");
            }
            return false;
        }
        return true;
    }

    private ActionSystem.ActionContext context(String source, String metadata) {
        return ActionSystem.ActionContext.builder()
                .menuName(source)
                .formType("bedrock-crates")
                .metadata("feature", metadata)
                .build();
    }

    private String getItemDisplayName(org.bukkit.inventory.ItemStack item) {
        if (item == null) return "Unknown Item";
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        return item.getType().name().replace('_', ' ');
    }

    private String getItemLore(org.bukkit.inventory.ItemStack item) {
        if (item == null) return "";
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasLore()) {
            return String.join("\n", meta.getLore());
        }
        return "";
    }
}
