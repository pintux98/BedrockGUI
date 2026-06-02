package it.pintux.life.essentialsaddon.service;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.essentialsaddon.config.EssentialsAddonConfiguration;
import it.pintux.life.essentialsaddon.util.BukkitFormPlayer;
import it.pintux.life.essentialsaddon.util.EssentialsActionPayloads;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

public final class BedrockCratePreviewService {
    private final EssentialsAddonConfiguration configuration;
    private final CrateCatalogService crateCatalog;
    private final BedrockPlayerDetector bedrockPlayerDetector;

    public BedrockCratePreviewService(
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

    public void openCrateMenu(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!ensureCrateCatalog(player)) return;

        List<String> crates = crateCatalog.getAccessibleCrates(player);
        if (crates.isEmpty()) {
            player.sendMessage("&cNo crates are currently available.");
            return;
        }

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm("&bCrates");
        form.content("&7Select a crate to preview.");

        for (String crateId : crates) {
            String displayName = crateCatalog.getDisplayName(crateId);
            String description = crateCatalog.getDescription(crateId);
            String buttonText = "&e" + displayName;
            if (!description.isEmpty()) {
                buttonText += "\n&7" + description;
            }
            form.button(buttonText, formPlayer ->
                    api.executeActionString(formPlayer,
                            "crate_preview:" + EssentialsActionPayloads.encodeCrate(crateId),
                            api.createActionContext(Map.of(), Map.of(), Map.of("source", "crate-menu", "crateId", crateId), "crate-menu", "simple")));
        }

        form.send(new BukkitFormPlayer(player));
    }

    public void openCratePreview(Player player, String crateId) {
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

        form.button("&7Back to Crates", formPlayer ->
                api.executeActionString(formPlayer, "crate_main:",
                        api.createActionContext(Map.of(), Map.of(), Map.of("source", "crate-back"), "crate-back", "simple")));

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

    private String getItemDisplayName(ItemStack item) {
        if (item == null) return "Unknown Item";
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        return item.getType().name().replace('_', ' ');
    }

    private String getItemLore(ItemStack item) {
        if (item == null) return "";
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasLore()) {
            return String.join("\n", meta.getLore());
        }
        return "";
    }
}
