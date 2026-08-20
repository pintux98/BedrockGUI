package it.pintux.life.duelsaddon.model;

import java.util.List;

/**
 * A PhoenixDuels confirmation prompt, lifted out so it can be shown as a Bedrock modal.
 *
 * <p>PhoenixDuels reuses one generic confirmation menu for anything destructive - disbanding a
 * party is the common one - and holds the outcome as two {@link Runnable}s. Those are carried here
 * verbatim, so accepting runs exactly what clicking the chest item would have run and this addon
 * never has to reimplement the action or guess what the prompt was about.</p>
 *
 * @param title       prompt title, already colour-translated
 * @param description prompt body lines, already colour-translated; may be empty
 * @param onAccept    what PhoenixDuels does on confirm; never {@code null}
 * @param onDecline   what PhoenixDuels does on cancel; may be {@code null}
 */
public record ConfirmationRequest(String title,
                                  List<String> description,
                                  Runnable onAccept,
                                  Runnable onDecline) {
}
