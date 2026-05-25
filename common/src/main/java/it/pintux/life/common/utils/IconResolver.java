package it.pintux.life.common.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Converts Bukkit Material names to Bedrock form button image paths.
 *
 * Strategy:
 * 1. AIR / null / void → null
 * 2. Explicit irregular map (only Java→Bedrock ID differences)
 * 3. Suffix patterns: spawn eggs, music discs
 * 4. Fallback: lowercase the Java material name
 */
public final class IconResolver {

    private IconResolver() {}

    // ─── Irregular blocks (only entries where Bedrock ID != toLowerCase(Java ID)) ──

    private static final Map<String, String> IRREGULAR_BLOCKS = Map.ofEntries(
            Map.entry("SHULKER_BOX", "undyed_shulker_box"),
            Map.entry("GRASS_BLOCK", "grass"),
            Map.entry("SHORT_GRASS", "tallgrass"),
            Map.entry("TALL_GRASS", "double_plant"),
            Map.entry("WATER", "flowing_water"),
            Map.entry("LAVA", "flowing_lava"),
            Map.entry("OAK_SIGN", "standing_sign"),
            Map.entry("OAK_WALL_SIGN", "wall_sign"),
            Map.entry("PISTON_HEAD", "piston_arm_collision"),
            Map.entry("MOVING_PISTON", "movingblock"),
            Map.entry("BRICKS", "brick_block"),
            Map.entry("NETHER_BRICKS", "nether_brick"),
            Map.entry("END_STONE_BRICKS", "end_bricks"),
            Map.entry("GLOWSTONE", "lightstone"),
            Map.entry("JACK_O_LANTERN", "lit_pumpkin"),
            Map.entry("HAY_BLOCK", "hay_bale"),
            Map.entry("TERRACOTTA", "hardened_clay"),
            Map.entry("WHITE_TERRACOTTA", "stained_hardened_clay"),
            Map.entry("ORANGE_TERRACOTTA", "stained_hardened_clay"),
            Map.entry("MAGENTA_TERRACOTTA", "stained_hardened_clay"),
            Map.entry("LIGHT_BLUE_TERRACOTTA", "stained_hardened_clay"),
            Map.entry("YELLOW_TERRACOTTA", "stained_hardened_clay"),
            Map.entry("LIME_TERRACOTTA", "stained_hardened_clay"),
            Map.entry("PINK_TERRACOTTA", "stained_hardened_clay"),
            Map.entry("GRAY_TERRACOTTA", "stained_hardened_clay"),
            Map.entry("LIGHT_GRAY_TERRACOTTA", "stained_hardened_clay"),
            Map.entry("CYAN_TERRACOTTA", "stained_hardened_clay"),
            Map.entry("PURPLE_TERRACOTTA", "stained_hardened_clay"),
            Map.entry("BLUE_TERRACOTTA", "stained_hardened_clay"),
            Map.entry("BROWN_TERRACOTTA", "stained_hardened_clay"),
            Map.entry("GREEN_TERRACOTTA", "stained_hardened_clay"),
            Map.entry("RED_TERRACOTTA", "stained_hardened_clay"),
            Map.entry("BLACK_TERRACOTTA", "stained_hardened_clay"),
            Map.entry("WHITE_STAINED_GLASS", "stained_glass"),
            Map.entry("ORANGE_STAINED_GLASS", "stained_glass"),
            Map.entry("MAGENTA_STAINED_GLASS", "stained_glass"),
            Map.entry("LIGHT_BLUE_STAINED_GLASS", "stained_glass"),
            Map.entry("YELLOW_STAINED_GLASS", "stained_glass"),
            Map.entry("LIME_STAINED_GLASS", "stained_glass"),
            Map.entry("PINK_STAINED_GLASS", "stained_glass"),
            Map.entry("GRAY_STAINED_GLASS", "stained_glass"),
            Map.entry("LIGHT_GRAY_STAINED_GLASS", "stained_glass"),
            Map.entry("CYAN_STAINED_GLASS", "stained_glass"),
            Map.entry("PURPLE_STAINED_GLASS", "stained_glass"),
            Map.entry("BLUE_STAINED_GLASS", "stained_glass"),
            Map.entry("BROWN_STAINED_GLASS", "stained_glass"),
            Map.entry("GREEN_STAINED_GLASS", "stained_glass"),
            Map.entry("RED_STAINED_GLASS", "stained_glass"),
            Map.entry("BLACK_STAINED_GLASS", "stained_glass"),
            Map.entry("WHITE_WOOL", "wool"),
            Map.entry("ORANGE_WOOL", "wool"),
            Map.entry("MAGENTA_WOOL", "wool"),
            Map.entry("LIGHT_BLUE_WOOL", "wool"),
            Map.entry("YELLOW_WOOL", "wool"),
            Map.entry("LIME_WOOL", "wool"),
            Map.entry("PINK_WOOL", "wool"),
            Map.entry("GRAY_WOOL", "wool"),
            Map.entry("LIGHT_GRAY_WOOL", "wool"),
            Map.entry("CYAN_WOOL", "wool"),
            Map.entry("PURPLE_WOOL", "wool"),
            Map.entry("BLUE_WOOL", "wool"),
            Map.entry("BROWN_WOOL", "wool"),
            Map.entry("GREEN_WOOL", "wool"),
            Map.entry("RED_WOOL", "wool"),
            Map.entry("BLACK_WOOL", "wool"),
            Map.entry("WHITE_BED", "bed"),
            Map.entry("ORANGE_BED", "bed"),
            Map.entry("MAGENTA_BED", "bed"),
            Map.entry("LIGHT_BLUE_BED", "bed"),
            Map.entry("YELLOW_BED", "bed"),
            Map.entry("LIME_BED", "bed"),
            Map.entry("PINK_BED", "bed"),
            Map.entry("GRAY_BED", "bed"),
            Map.entry("LIGHT_GRAY_BED", "bed"),
            Map.entry("CYAN_BED", "bed"),
            Map.entry("PURPLE_BED", "bed"),
            Map.entry("BLUE_BED", "bed"),
            Map.entry("BROWN_BED", "bed"),
            Map.entry("GREEN_BED", "bed"),
            Map.entry("RED_BED", "bed"),
            Map.entry("BLACK_BED", "bed"),
            Map.entry("FARMLAND", "farmland_dry"),
            Map.entry("WET_FARMLAND", "farmland_wet"),
            Map.entry("ATTACHED_MELON_STEM", "melon_stem_connected"),
            Map.entry("ATTACHED_PUMPKIN_STEM", "pumpkin_stem_connected"),
            Map.entry("MELON_STEM", "melon_stem_disconnected"),
            Map.entry("PUMPKIN_STEM", "pumpkin_stem_disconnected"),
            Map.entry("FIRE", "fire_0"),
            Map.entry("SOUL_FIRE", "soul_fire_0"),
            Map.entry("NETHER_PORTAL", "portal"),
            Map.entry("COMMAND_BLOCK", "command_block_back"),
            Map.entry("CHAIN_COMMAND_BLOCK", "chain_command_block_back"),
            Map.entry("REPEATING_COMMAND_BLOCK", "repeating_command_block_back"),
            Map.entry("COBBLESTONE_WALL", "cobblestone_wall"),
            Map.entry("MOSSY_COBBLESTONE_WALL", "mossy_cobblestone_wall"),
            Map.entry("RED_NETHER_BRICKS", "red_nether_brick"),
            Map.entry("MAGMA_BLOCK", "magma"),
            Map.entry("SEA_LANTERN", "sealantern"),
            Map.entry("COARSE_DIRT", "coarse_dirt"),
            Map.entry("GRASS_PATH", "grass_path"),
            Map.entry("SUNFLOWER", "double_plant"),
            Map.entry("ROSE_BUSH", "double_plant"),
            Map.entry("PEONY", "double_plant"),
            Map.entry("LILAC", "double_plant"),
            Map.entry("LARGE_FERN", "double_plant"),
            Map.entry("CHISELED_STONE_BRICKS", "stonebrick"),
            Map.entry("CRACKED_STONE_BRICKS", "stonebrick"),
            Map.entry("MOSSY_STONE_BRICKS", "stonebrick"),
            Map.entry("SMOOTH_STONE", "stone"),
            Map.entry("PODZOL", "podzol"),
            Map.entry("CARROTS", "carrots"),
            Map.entry("POTATOES", "potatoes"),
            Map.entry("BEETROOTS", "beetroot"),
            Map.entry("SWEET_BERRY_BUSH", "sweet_berry_bush"),
            Map.entry("CAVE_VINES", "cave_vines"),
            Map.entry("CAVE_VINES_PLANT", "cave_vines_body_with_berries"),
            Map.entry("OBSERVER", "observer"),
            Map.entry("STRUCTURE_VOID", "structure_void"),
            Map.entry("STRUCTURE_BLOCK", "structure_block"),
            Map.entry("REPEATER", "unpowered_repeater"),
            Map.entry("COMPARATOR", "unpowered_comparator"),
            Map.entry("ENCHANTING_TABLE", "enchanting_table"),
            Map.entry("BREWING_STAND", "brewing_stand"),
            Map.entry("CAULDRON", "cauldron"),
            Map.entry("END_PORTAL_FRAME", "end_portal_frame"),
            Map.entry("END_PORTAL", "end_portal"),
            Map.entry("DRAGON_EGG", "dragon_egg"),
            Map.entry("SLIME_BLOCK", "slime"),
            Map.entry("NETHER_WART_BLOCK", "nether_wart_block"),
            Map.entry("BONE_BLOCK", "bone_block"),
            Map.entry("PURPUR_BLOCK", "purpur_block"),
            Map.entry("PURPUR_STAIRS", "purpur_stairs"),
            Map.entry("END_ROD", "end_rod"),
            Map.entry("END_GATEWAY", "end_gateway")
    );

    // ─── Irregular items (only entries where Bedrock ID != toLowerCase(Java ID)) ──

    private static final Map<String, String> IRREGULAR_ITEMS = Map.ofEntries(
            Map.entry("REDSTONE_WIRE", "redstone_dust")
    );

    // Merged at class init — blocks override items for shared keys
    private static final Map<String, String> IRREGULAR;
    static {
        Map<String, String> m = new HashMap<>(IRREGULAR_ITEMS.size() + IRREGULAR_BLOCKS.size());
        m.putAll(IRREGULAR_ITEMS);
        m.putAll(IRREGULAR_BLOCKS);
        IRREGULAR = Map.copyOf(m);
    }

    // ─── Derived item-key set ────────────────────────────────────────

    private static final Set<String> ITEM_KEYS;
    static {
        Set<String> s = new HashSet<>(IRREGULAR_ITEMS.keySet());
        ITEM_KEYS = Set.copyOf(s);
    }

    private static final Set<String> AIR_MATERIALS = Set.of(
            "AIR", "CAVE_AIR", "VOID_AIR", "STRUCTURE_VOID", "BARRIER", "LIGHT"
    );

    // ─── Public API ──────────────────────────────────────────────────

    /**
     * Resolve a material name to a Bedrock texture path.
     * @param materialName Bukkit Material name (e.g. "DIAMOND_SWORD")
     * @return Bedrock texture path (e.g. "textures/items/diamond_sword") or null
     */
    public static String resolve(String materialName) {
        if (materialName == null || materialName.isBlank()) return null;
        String n = materialName.toUpperCase().trim();
        if (AIR_MATERIALS.contains(n)) return null;

        // 1. Explicit irregular mapping
        String hit = IRREGULAR.get(n);
        if (hit != null) return "textures/" + category(n) + "/" + hit;

        // 2. Suffix patterns
        String pattern;
        if ((pattern = trySpawnEgg(n)) != null) return "textures/items/" + pattern;
        if ((pattern = tryMusicDisc(n)) != null) return "textures/items/" + pattern;

        // 3. Fallback: simple toLowerCase
        return "textures/" + category(n) + "/" + n.toLowerCase();
    }

    // ─── Pattern functions ───────────────────────────────────────────

    private static String trySpawnEgg(String n) {
        if (n.endsWith("_SPAWN_EGG"))
            return "spawn_" + n.substring(0, n.length() - 10).toLowerCase();
        return null;
    }

    private static String tryMusicDisc(String n) {
        if (n.startsWith("MUSIC_DISC_"))
            return "record_" + n.substring(11).toLowerCase();
        return null;
    }

    // ─── Category (items vs blocks) ──────────────────────────────────

    private static String category(String n) {
        return isItem(n) ? "items" : "blocks";
    }

    /**
     * Full image resolution chain for form button icons.
     * Handles: bare material names, Java-style paths (textures/items/X),
     * already-resolved Bedrock paths, and player names.
     */
    public static String resolveImage(String image) {
        if (image == null || image.isBlank()) return null;
        String trimmed = image.trim();

        if (trimmed.startsWith("textures/") || trimmed.startsWith("http://") || trimmed.startsWith("https://"))
            return trimmed;

        String resolved = resolve(trimmed);
        if (resolved != null) return resolved;

        if (trimmed.matches("^[A-Za-z0-9_.\\-]+$"))
            return "https://mc-heads.net/head/" + trimmed + "/64";

        return trimmed;
    }

    private static final Set<String> BLOCK_SUFFIXES = Set.of(
            "_BLOCK", "_ORE", "_STONE", "_BRICKS",
            "_LOG", "_WOOD", "_LEAVES", "_PLANKS", "_SAPLING",
            "_STAIRS", "_SLAB", "_FENCE", "_FENCE_GATE", "_DOOR", "_TRAPDOOR",
            "_WALL", "_WALL_SIGN", "_SIGN", "_BUTTON", "_PRESSURE_PLATE",
            "_GLASS", "_GLASS_PANE", "_PANE",
            "_CARPET", "_BED", "_WOOL",
            "_TERRACOTTA", "_GLAZED_TERRACOTTA",
            "_CONCRETE", "_CONCRETE_POWDER",
            "_CANDLE",
            "_PLANT", "_VINE", "_GRASS", "_SOIL", "_SAND", "_GRAVEL",
            "_FUNGUS", "_NYLIUM",
            "_STEM", "_BUD", "_CLUSTER",
            "_PORTAL", "_GATEWAY",
            "_SLIME", "_SPONGE",
            "_FARMLAND",
            "_SPAWNER",
            "_CAULDRON",
            "_FURNACE",
            "_ANVIL",
            "_RAIL",
            "_LADDER",
            "_TORCH",
            "_LANTERN",
            "_SHULKER_BOX",
            "_CHEST",
            "_BARREL",
            "_BREWING_STAND",
            "_ENCHANTING_TABLE",
            "_END_PORTAL",
            "_COMMAND_BLOCK",
            "_STRUCTURE_BLOCK",
            "_OBSERVER",
            "_REPEATER",
            "_COMPARATOR",
            "_PISTON",
            "_HOPPER",
            "_DROPPER",
            "_DISPENSER",
            "_JUKEBOX",
            "_NOTE_BLOCK",
            "_DAYLIGHT_DETECTOR",
            "_LECTERN",
            "_SMITHING_TABLE",
            "_FLETCHING_TABLE",
            "_CARTOGRAPHY_TABLE",
            "_LOOM",
            "_STONECUTTER",
            "_GRINDSTONE",
            "_COMPOSTER",
            "_BELL",
            "_CAMPFIRE",
            "_BEACON",
            "_CONDUIT",
            "_LODESTONE",
            "_SCAFFOLDING",
            "_TARGET",
            "_RESPAWN_ANCHOR",
            "_AMETHYST",
            "_SCULK",
            "_TUFF",
            "_DEEPSLATE",
            "_CALCITE",
            "_DRIPSTONE",
            "_MOSS",
            "_MUD",
            "_MYCELIUM",
            "_PODZOL",
            "_BASALT",
            "_BLACKSTONE",
            "_PRISMARINE",
            "_PURPUR",
            "_MAGMA",
            "_SHROOMLIGHT",
            "_NETHER_WART",
            "_WART",
            "_GLOWSTONE",
            "_SEA_LANTERN",
            "_JACK_O_LANTERN",
            "_DRAGON_EGG",
            "_END_ROD",
            "_BONE_BLOCK",
            "_HONEYCOMB",
            "_HONEY_BLOCK",
            "_SLIME_BLOCK",
            "_HAY_BLOCK",
            "_COAL_BLOCK",
            "_IRON_BLOCK",
            "_GOLD_BLOCK",
            "_DIAMOND_BLOCK",
            "_EMERALD_BLOCK",
            "_LAPIS_BLOCK",
            "_REDSTONE_BLOCK",
            "_QUARTZ_BLOCK",
            "_NETHERITE_BLOCK",
            "_ANCIENT_DEBRIS",
            "_TINTED_GLASS",
            "_FROGLIGHT",
            "_FROGSPAWN",
            "_MUSHROOM",
            "_CORAL",
            "_CORAL_FAN",
            "_FLOWER",
            "_BEDROCK",
            "_OBSIDIAN",
            "_ICE",
            "_SNOW",
            "_CLAY",
            "_DIRT",
            "_PATH",
            "_FIRE",
            "_TRIPWIRE",
            "_WEB",
            "_COBWEB",
            "_EGG",
            "_BEEHIVE",
            "_BEE_NEST",
            "_DRIED_KELP_BLOCK",
            "_RESIN_BRICKS",
            "_COPPER_CHEST",
            "_COPPER_BLOCK",
            "_COPPER_GRATE",
            "_COPPER_DOOR",
            "_COPPER_TRAPDOOR",
            "_COPPER_BULB",
            "_CHISELED_COPPER",
            "_CUT_COPPER",
            "_LIGHTNING_ROD",
            "_CRAFTER",
            "_TRIAL_SPAWNER",
            "_VAULT",
            "_HEAVY_CORE",
            "_CHISELED_BOOKSHELF",
            "_DECORATED_POT",
            "_TURTLE_EGG",
            "_SNIFFER_EGG",
            "_END_CRYSTAL",
            "_AZALEA",
            "_LAPIS_ORE",
            "_NETHER_QUARTZ_ORE",
            "_CHAIN",
            "_POINTED_DRIPSTONE",
            "_BAMBOO_BLOCK",
            "_SHORT_GRASS",
            "_TALL_GRASS",
            "_TALL_SEAGRASS",
            "_SEAGRASS",
            "_SEA_PICKLE",
            "_KELP",
            "_CACTUS",
            "_DEAD_BUSH",
            "_FERN",
            "_LARGE_FERN",
            "_ROSE_BUSH",
            "_LILAC",
            "_SUNFLOWER",
            "_PEONY",
            "_PITCHER_PLANT",
            "_CHORUS_PLANT",
            "_CHORUS_FLOWER"
    );

    private static boolean isItem(String n) {
        if (ITEM_KEYS.contains(n)) return true;
        if (n.startsWith("MUSIC_DISC_")) return true;
        for (String suffix : BLOCK_SUFFIXES) {
            if (n.endsWith(suffix)) return false;
        }
        return true;
    }
}
