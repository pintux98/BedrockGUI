package it.pintux.life.essentialsaddon.listener;

import fr.nocsy.mcpets.data.Pet;
import fr.nocsy.mcpets.data.config.Language;
import it.pintux.life.essentialsaddon.service.BedrockPetService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class McPetsMenuListener implements Listener {

    private static final List<String> PET_MENUS = List.of(
            "INVENTORY_PETS_MENU_INTERACTIONS", "INVENTORY_MOUNTS_MENU_INTERACTIONS");
    private static final List<String> LIST_MENUS = List.of(
            "INVENTORY_PETS_MENU", "INVENTORY_MOUNTS_MENU", "CATEGORY_MENU_TITLE");

    private final JavaPlugin plugin;
    private final BedrockPetService service;
    private final Set<UUID> pending = new HashSet<>();

    public McPetsMenuListener(JavaPlugin plugin, BedrockPetService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    @SuppressWarnings("deprecation")
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player) || !service.shouldHandle(player)) {
            return;
        }
        String title = ChatColor.stripColor(event.getView().getTitle());
        String petId = null;
        if (matches(title, PET_MENUS)) {
            Pet pet = Pet.getFromLastInteractedWith(player);
            if (pet == null || !player.getUniqueId().equals(pet.getOwner())) {
                return;
            }
            petId = pet.getId();
        } else if (!matches(title, LIST_MENUS)) {
            return;
        }
        event.setCancelled(true);
        if (!pending.add(player.getUniqueId())) {
            return;
        }
        String target = petId;
        Bukkit.getScheduler().runTask(plugin, () -> {
            pending.remove(player.getUniqueId());
            if (!player.isOnline()) {
                return;
            }
            if (target == null) {
                service.openPetList(player);
            } else {
                service.openPetModal(player, target);
            }
        });
    }

    private static boolean matches(String title, List<String> languageKeys) {
        for (String key : languageKeys) {
            try {
                String menuTitle = ChatColor.stripColor(Language.valueOf(key).getMessage());
                if (menuTitle != null && menuTitle.equalsIgnoreCase(title)) {
                    return true;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return false;
    }
}
