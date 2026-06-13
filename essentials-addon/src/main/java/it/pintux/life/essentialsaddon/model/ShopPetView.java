package it.pintux.life.essentialsaddon.model;

public record ShopPetView(
        String shopId,
        String petId,
        String displayName,
        String petType,
        double price,
        boolean owned
) {
}
