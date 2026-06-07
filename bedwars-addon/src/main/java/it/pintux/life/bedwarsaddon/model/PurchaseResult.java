package it.pintux.life.bedwarsaddon.model;

/** Outcome of a purchase attempt, so services give clean feedback without parsing exceptions. */
public record PurchaseResult(boolean success, String reason) {
    public static PurchaseResult ok() { return new PurchaseResult(true, null); }
    public static PurchaseResult fail(String reason) { return new PurchaseResult(false, reason); }
}
