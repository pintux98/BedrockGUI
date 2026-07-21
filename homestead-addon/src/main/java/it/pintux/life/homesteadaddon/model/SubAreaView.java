package it.pintux.life.homesteadaddon.model;

public record SubAreaView(
        long id,
        long regionId,
        String name,
        long playerFlags,
        int volume,
        long createdAt,
        boolean rented
) {
}
