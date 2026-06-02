package it.pintux.life.essentialsaddon.shop.util;

import org.bukkit.Material;

/**
 * Converts Bukkit Material names to Bedrock form button image paths.
 * Bedrock Cumulus supports two image types:
 * - PATH: "textures/..." prefix resolves to client-side texture paths
 * - URL: any other string is treated as a remote image URL
 */
public final class BedrockIconResolver {

    private BedrockIconResolver() {}

    /**
     * Resolve a Bukkit Material name to a Bedrock texture path.
     * Returns null if the material is null, blank, or AIR (no icon).
     */
    public static String resolveTexturePath(String materialName) {
        if (materialName == null || materialName.isBlank()) {
            return null;
        }
        String normalized = materialName.toUpperCase().trim();
        if (normalized.equals("AIR") || normalized.equals("CAVE_AIR") || normalized.equals("VOID_AIR")) {
            return null;
        }
        return "textures/" + toBedrockPath(normalized);
    }

    /**
     * Resolve a Bukkit Material enum to a Bedrock texture path.
     */
    public static String resolveTexturePath(Material material) {
        if (material == null || material.isAir()) {
            return null;
        }
        return "textures/" + toBedrockPath(material.name());
    }

    private static String toBedrockPath(String bukkitName) {
        if (isItem(bukkitName)) {
            return "items/" + bukkitName.toLowerCase().replace('_', '.');
        }
        return "blocks/" + bukkitName.toLowerCase().replace('_', '.');
    }

    /**
     * Heuristic: if the material name matches common item categories, use items/ path.
     */
    private static boolean isItem(String bukkitName) {
        return bukkitName.contains("POTION") ||
               bukkitName.contains("SCROLL") ||
               bukkitName.contains("BUCKET") ||
               bukkitName.contains("BOWL") ||
               bukkitName.contains("STICK") ||
               bukkitName.contains("STRING") ||
               bukkitName.contains("FEATHER") ||
               bukkitName.contains("FLINT") ||
               bukkitName.contains("BONE") ||
               bukkitName.contains("EGG") ||
               bukkitName.contains("MILK") ||
               bukkitName.contains("CAKE") ||
               bukkitName.contains("COOKIE") ||
               bukkitName.contains("MUSHROOM_STEW") ||
               bukkitName.contains("RABBIT_STEW") ||
               bukkitName.contains("BEETROOT_SOUP") ||
               bukkitName.contains("SUSPICIOUS_STEW") ||
               bukkitName.contains("ARMOR") ||
               bukkitName.contains("HORSE_ARMOR") ||
               bukkitName.contains("SADDLE") ||
               bukkitName.contains("LEAD") ||
               bukkitName.contains("NAME_TAG") ||
               bukkitName.contains("MAP") ||
               bukkitName.contains("COMPASS") ||
               bukkitName.contains("CLOCK") ||
               bukkitName.contains("SPYGLASS") ||
               bukkitName.contains("RECOVERY_COMPASS") ||
               bukkitName.contains("GOAT_HORN") ||
               bukkitName.contains("BUNDLE") ||
               bukkitName.contains("FIREWORK") ||
               bukkitName.contains("ENCHANTED_BOOK") ||
               bukkitName.contains("WRITABLE_BOOK") ||
               bukkitName.equals("BOOK") ||
               bukkitName.equals("PAPER") ||
               bukkitName.equals("BOW") ||
               bukkitName.equals("ARROW") ||
               bukkitName.equals("SPECTRAL_ARROW") ||
               bukkitName.equals("TIPPED_ARROW") ||
               bukkitName.equals("TRIDENT") ||
               bukkitName.equals("FISHING_ROD") ||
               bukkitName.equals("FLINT_AND_STEEL") ||
               bukkitName.equals("SHEARS") ||
               bukkitName.equals("BRUSH") ||
               bukkitName.equals("SHIELD") ||
               bukkitName.equals("CROSSBOW") ||
               bukkitName.equals("CARVED_PUMPKIN") ||
               bukkitName.equals("PLAYER_HEAD") ||
               bukkitName.equals("ZOMBIE_HEAD") ||
               bukkitName.equals("SKELETON_SKULL") ||
               bukkitName.equals("WITHER_SKELETON_SKULL") ||
               bukkitName.equals("CREEPER_HEAD") ||
               bukkitName.equals("DRAGON_HEAD") ||
               bukkitName.equals("PIGLIN_HEAD") ||
               bukkitName.equals("TOTEM_OF_UNDYING") ||
               bukkitName.equals("ENDER_EYE") ||
               bukkitName.equals("ENDER_PEARL") ||
               bukkitName.equals("EXPERIENCE_BOTTLE") ||
               bukkitName.equals("GLOWSTONE_DUST") ||
               bukkitName.equals("REDSTONE") ||
               bukkitName.equals("GUNPOWDER") ||
               bukkitName.equals("SLIME_BALL") ||
               bukkitName.equals("BLAZE_ROD") ||
               bukkitName.equals("BLAZE_POWDER") ||
               bukkitName.equals("GHAST_TEAR") ||
               bukkitName.equals("SPIDER_EYE") ||
               bukkitName.equals("FERMENTED_SPIDER_EYE") ||
               bukkitName.equals("MAGMA_CREAM") ||
               bukkitName.equals("GLISTERING_MELON_SLICE") ||
               bukkitName.equals("RABBIT_FOOT") ||
               bukkitName.equals("RABBIT_HIDE") ||
               bukkitName.equals("PHANTOM_MEMBRANE") ||
               bukkitName.equals("SCUTE") ||
               bukkitName.equals("TURTLE_HELMET") ||
               bukkitName.equals("NAUTILUS_SHELL") ||
               bukkitName.equals("HEART_OF_THE_SEA") ||
               bukkitName.equals("DRAGON_BREATH") ||
               bukkitName.equals("SHULKER_SHELL") ||
               bukkitName.equals("ELYTRA") ||
               bukkitName.equals("NETHER_STAR") ||
               bukkitName.equals("PRISMARINE_SHARD") ||
               bukkitName.equals("PRISMARINE_CRYSTALS") ||
               bukkitName.equals("SPONGE") ||
               bukkitName.equals("AMETHYST_SHARD") ||
               bukkitName.equals("ECHO_SHARD") ||
               bukkitName.equals("DISC_FRAGMENT") ||
               bukkitName.startsWith("MUSIC_DISC_") ||
               bukkitName.startsWith("POTION") ||
               bukkitName.startsWith("SPLASH_POTION") ||
               bukkitName.startsWith("LINGERING_POTION") ||
               bukkitName.startsWith("TIPPED_ARROW") ||
               bukkitName.startsWith("ENCHANTED_GOLDEN_APPLE") ||
               bukkitName.equals("GOLDEN_APPLE") ||
               bukkitName.equals("GOLDEN_CARROT") ||
               bukkitName.equals("GLISTERING_MELON_SLICE");
    }
}
