package it.pintux.life.essentialsaddon.model;

public record PetBuyResult(boolean success, String reason) {
    public static PetBuyResult ok() {
        return new PetBuyResult(true, "");
    }

    public static PetBuyResult fail(String reason) {
        return new PetBuyResult(false, reason);
    }
}
