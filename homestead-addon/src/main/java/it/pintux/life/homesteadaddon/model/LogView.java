package it.pintux.life.homesteadaddon.model;

public record LogView(
        String author,
        String message,
        long sentAt,
        boolean read
) {
}
