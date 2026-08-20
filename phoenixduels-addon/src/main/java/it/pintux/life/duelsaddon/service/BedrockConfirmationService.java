package it.pintux.life.duelsaddon.service;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.duelsaddon.api.BedrockPlayerDetector;
import it.pintux.life.duelsaddon.config.DuelsAddonConfiguration;
import it.pintux.life.duelsaddon.gateway.DuelsGateway;
import it.pintux.life.duelsaddon.model.ConfirmationRequest;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

/**
 * Shows PhoenixDuels' generic confirmation prompt as a Bedrock modal.
 *
 * <p>PhoenixDuels reuses one confirmation menu for every destructive action - disbanding a party
 * being the one players actually hit - so this covers all of them without knowing which is which.
 * The prompt's own title and body are reused, and the buttons run PhoenixDuels' own accept and
 * decline runnables, so behaviour cannot drift from what the chest item would have done.</p>
 */
public final class BedrockConfirmationService extends BedrockServiceSupport {

    private final Plugin plugin;

    public BedrockConfirmationService(Plugin plugin, DuelsAddonConfiguration config,
                                      DuelsGateway gateway, BedrockPlayerDetector detector) {
        super(config, gateway, detector);
        this.plugin = plugin;
    }

    /**
     * @param containerView the cancelled PhoenixDuels view, passed through opaquely
     * @return whether a confirmation form was shown; false means this was not a confirmation and
     *         the caller should leave the Java menu alone
     */
    public boolean open(Player player, Object containerView) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return false;
        }
        Optional<ConfirmationRequest> found = gateway.confirmation(containerView);
        if (found.isEmpty()) {
            return false;
        }
        ConfirmationRequest request = found.get();

        String title = request.title() == null || request.title().isBlank()
                ? text("confirmation.title") : request.title();
        String content = request.description().isEmpty()
                ? text("confirmation.content") : String.join("\n", request.description());

        api.createModalForm(title, content)
                .button1(text("confirmation.accept"), formPlayer -> run(request.onAccept()))
                .button2(text("common.confirm-no"), formPlayer -> run(request.onDecline()))
                .send(wrap(player));
        return true;
    }

    /**
     * PhoenixDuels' runnables touch the world, and a form callback arrives off the main thread, so
     * they cannot be invoked inline.
     */
    private void run(Runnable action) {
        if (action == null) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, action);
    }
}
