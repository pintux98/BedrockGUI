package it.pintux.life.essentialsaddon.model;

/**
 * Outcome of a home write.
 *
 * <p>{@code playerNotified} is set when the backing plugin already told the player why it refused,
 * so the addon does not follow its explanation with a vaguer one of its own.</p>
 */
public record HomeWriteResult(boolean success, boolean playerNotified, String reason) {

    public static HomeWriteResult ok() {
        return new HomeWriteResult(true, false, null);
    }

    public static HomeWriteResult failed(String reason) {
        return new HomeWriteResult(false, false, reason);
    }

    public static HomeWriteResult reportedToPlayer(String reason) {
        return new HomeWriteResult(false, true, reason);
    }
}
