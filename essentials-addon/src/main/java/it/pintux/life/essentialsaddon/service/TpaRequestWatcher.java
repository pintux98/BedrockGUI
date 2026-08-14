package it.pintux.life.essentialsaddon.service;

import it.pintux.life.essentialsaddon.api.BedrockPlayerDetector;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pushes the Accept/Deny form at a Bedrock player as soon as a teleport request arrives.
 *
 * <p>None of the supported TPA plugins fire an event we can hook, so this polls the active
 * {@link it.pintux.life.essentialsaddon.api.TpaProvider} instead — which keeps the popup working
 * for EssentialsX, CMI and HuskHomes alike, and also catches requests sent from console or by
 * another plugin.</p>
 */
public final class TpaRequestWatcher {
    private final Plugin plugin;
    private final TpaCatalogService tpaCatalog;
    private final BedrockTpaService tpaService;
    private final BedrockPlayerDetector bedrockPlayerDetector;
    private final long intervalTicks;

    /** Sender we last prompted for, per player, so a standing request is not re-popped every tick. */
    private final Map<UUID, String> prompted = new ConcurrentHashMap<>();
    private BukkitTask task;

    public TpaRequestWatcher(
            Plugin plugin,
            TpaCatalogService tpaCatalog,
            BedrockTpaService tpaService,
            BedrockPlayerDetector bedrockPlayerDetector,
            long intervalTicks
    ) {
        this.plugin = plugin;
        this.tpaCatalog = tpaCatalog;
        this.tpaService = tpaService;
        this.bedrockPlayerDetector = bedrockPlayerDetector;
        this.intervalTicks = Math.max(5L, intervalTicks);
    }

    public void start() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::poll, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        prompted.clear();
    }

    private void poll() {
        if (!tpaCatalog.isReady()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!bedrockPlayerDetector.isBedrockPlayer(player)) {
                continue;
            }
            UUID uuid = player.getUniqueId();
            if (!tpaCatalog.hasPendingRequest(player)) {
                prompted.remove(uuid);
                continue;
            }
            String sender = tpaCatalog.getPendingRequestSender(player);
            if (sender == null || sender.equals(prompted.get(uuid))) {
                continue;
            }
            prompted.put(uuid, sender);
            tpaService.showIncomingRequestForm(player, sender);
        }
        prompted.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }
}
