package it.pintux.life.bedwarsaddon.model;

/** A player's Bedwars statistics. */
public record PlayerStatsInfo(
        String name,
        int wins,
        int losses,
        int kills,
        int finalKills,
        int deaths,
        int finalDeaths,
        int bedsDestroyed,
        int gamesPlayed,
        int totalKills) {}
