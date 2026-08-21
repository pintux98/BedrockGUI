package it.pintux.life.essentialsaddon.model;

/**
 * A public home as its plugin describes it.
 *
 * <p>The owner and the name are taken from the plugin rather than split out of the identifier:
 * a Floodgate name starts with a dot, so {@code .Player.home} cannot be cut at the first one.</p>
 */
public record PublicHomeView(String identifier, String name, String owner) {
}
