package it.pintux.life.homesteadaddon.listener;

import it.pintux.life.homesteadaddon.service.BedrockRegionService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;
import java.util.Set;

public final class HomesteadCommandListener implements Listener {
    private static final Set<String> REGION_ROOTS = Set.of("region", "rg", "hs", "homestead");
    private static final Set<String> ADMIN_ROOTS = Set.of("homesteadadmin", "hsadmin");
    private static final Set<String> GUI_SUBCOMMANDS = Set.of("menu", "gui");

    private final BedrockRegionService regionService;

    public HomesteadCommandListener(BedrockRegionService regionService) {
        this.regionService = regionService;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Player player = event.getPlayer();
        if (!regionService.shouldHandle(player)) {
            return;
        }
        String message = event.getMessage();
        if (message == null || message.length() < 2 || message.charAt(0) != '/') {
            return;
        }
        String[] parts = message.substring(1).trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return;
        }
        String root = parts[0].toLowerCase(Locale.ROOT);
        boolean guiInvocation = parts.length == 1 || GUI_SUBCOMMANDS.contains(parts[1].toLowerCase(Locale.ROOT));
        if (!guiInvocation) {
            return;
        }

        if (REGION_ROOTS.contains(root)) {
            event.setCancelled(true);
            regionService.openRegionList(player, false, 1);
        } else if (ADMIN_ROOTS.contains(root)) {
            event.setCancelled(true);
            regionService.openRegionList(player, true, 1);
        }
    }
}
