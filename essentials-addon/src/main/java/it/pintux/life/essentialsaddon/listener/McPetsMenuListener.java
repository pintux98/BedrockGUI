package it.pintux.life.essentialsaddon.listener;

import fr.nocsy.mcpets.data.Pet;
import fr.nocsy.mcpets.data.inventories.PetInventoryHolder;
import it.pintux.life.essentialsaddon.service.BedrockPetService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class McPetsMenuListener implements Listener {

    private final JavaPlugin plugin;
    private final BedrockPetService service;
    private final Set<UUID> pending = new HashSet<>();

    public McPetsMenuListener(JavaPlugin plugin, BedrockPetService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof PetInventoryHolder holder)
                || !(event.getPlayer() instanceof Player player)
                || !service.shouldHandle(player)) {
            return;
        }
        String petId = null;
        switch (holder.getType()) {
            case PET_INTERACTION_MENU, MOUNT_INTERACTION_MENU -> {
                Pet pet = Pet.getFromLastInteractedWith(player);
                if (pet == null || !player.getUniqueId().equals(pet.getOwner())) {
                    return;
                }
                petId = pet.getId();
            }
            case PET_MENU, MOUNT_MENU, CATEGORIES_MENU, CATEGORY_MENU -> {
            }
            default -> {
                return;
            }
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
}
