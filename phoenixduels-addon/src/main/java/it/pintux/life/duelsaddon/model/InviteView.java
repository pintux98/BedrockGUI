package it.pintux.life.duelsaddon.model;

/**
 * A pending duel challenge, as rendered in the Bedrock accept/decline form.
 *
 * <p>The inviter's uuid is not carried: the caller already holds it, since it is the key the
 * invitation was looked up with.</p>
 *
 * @param inviterName       who sent the challenge
 * @param modeName          display name of the challenged mode
 * @param rounds            rounds to win
 * @param expiresInSeconds  PhoenixDuels' configured invitation lifetime
 */
public record InviteView(String inviterName,
                         String modeName,
                         int rounds,
                         int expiresInSeconds) {
}
