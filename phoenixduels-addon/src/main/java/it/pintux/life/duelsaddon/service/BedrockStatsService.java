package it.pintux.life.duelsaddon.service;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.duelsaddon.api.BedrockPlayerDetector;
import it.pintux.life.duelsaddon.config.DuelsAddonConfiguration;
import it.pintux.life.duelsaddon.gateway.DuelsGateway;
import it.pintux.life.duelsaddon.model.LeaderboardEntry;
import it.pintux.life.duelsaddon.model.StatsKind;
import it.pintux.life.duelsaddon.model.StatsView;
import it.pintux.life.duelsaddon.util.Formatting;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Bedrock forms for personal statistics and the leaderboard.
 *
 * <p>The leaderboard is rendered as form content rather than as buttons, because its rows are not
 * actionable; only the metric switches and pagination are buttons.</p>
 */
public final class BedrockStatsService extends BedrockServiceSupport {

    public BedrockStatsService(DuelsAddonConfiguration config, DuelsGateway gateway,
                               BedrockPlayerDetector detector) {
        super(config, gateway, detector);
    }

    public void openMain(Player player) {
        openStats(player, StatsKind.UNRANKED);
    }

    public void openStats(Player player, StatsKind kind) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        Optional<StatsView> stats = gateway.stats(player.getUniqueId(), player.getName(), kind);

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(
                render("stats.title", Map.of("player", player.getName())));
        if (stats.isEmpty()) {
            form.content(text("stats.no-data"));
        } else {
            StatsView view = stats.get();
            form.content(render("stats.content", Map.of(
                    "wins", String.valueOf(view.wins()),
                    "losses", String.valueOf(view.losses()),
                    "draws", String.valueOf(view.draws()),
                    "kills", String.valueOf(view.kills()),
                    "deaths", String.valueOf(view.deaths()),
                    "kd", Formatting.ratio(view.kills(), view.deaths()),
                    "winrate", Formatting.percent(view.wins(), view.playedMatches()))));
        }

        if (kind != StatsKind.UNRANKED) {
            form.button(text("stats.button-unranked"), fp -> openStats(player, StatsKind.UNRANKED));
        }
        if (kind != StatsKind.RANKED) {
            form.button(text("stats.button-ranked"), fp -> openStats(player, StatsKind.RANKED));
        }
        if (kind != StatsKind.CHALLENGE) {
            form.button(text("stats.button-challenge"), fp -> openStats(player, StatsKind.CHALLENGE));
        }
        form.button(text("stats.button-leaderboard"), fp -> openLeaderboard(player, kind, "wins", 1));
        form.button(text("common.close-button"), fp -> {
        });
        form.send(wrap(player));
    }

    public void openLeaderboard(Player player, StatsKind kind, String metric, int page) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        List<LeaderboardEntry> entries = gateway.leaderboard(kind, metric, config.itemsPerPage() * 3);

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(
                render("stats.leaderboard-title", Map.of("metric", metricLabel(metric))));
        if (entries.isEmpty()) {
            form.content(text("stats.leaderboard-empty"));
        } else {
            StringBuilder content = new StringBuilder(text("stats.leaderboard-content"));
            Pagination pagination = new Pagination(entries.size(), page);
            for (int i = pagination.start; i < pagination.end; i++) {
                LeaderboardEntry entry = entries.get(i);
                content.append(render("stats.leaderboard-line", Map.of(
                        "rank", String.valueOf(entry.rank()),
                        "player", entry.playerName() == null ? "?" : entry.playerName(),
                        "value", String.valueOf(entry.value())))).append('\n');
            }
            form.content(content.toString());
            pagination.addNav(form, p -> openLeaderboard(player, kind, metric, p));
        }

        for (String candidate : new String[]{"wins", "kills", "losses", "deaths"}) {
            if (!candidate.equalsIgnoreCase(metric)) {
                form.button(metricLabel(candidate), fp -> openLeaderboard(player, kind, candidate, 1));
            }
        }
        form.button(text("common.back-button"), fp -> openStats(player, kind));
        form.send(wrap(player));
    }

    private String metricLabel(String metric) {
        return switch (metric == null ? "" : metric.toLowerCase()) {
            case "kills" -> text("stats.metric-kills");
            case "losses" -> text("stats.metric-losses");
            case "deaths" -> text("stats.metric-deaths");
            default -> text("stats.metric-wins");
        };
    }
}
