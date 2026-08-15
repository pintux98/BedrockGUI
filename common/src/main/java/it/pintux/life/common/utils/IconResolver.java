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
 * 2. Generated atlas table (materials whose lowercase guess is not a real Bedrock texture)
 * 3. Explicit irregular map (only Java→Bedrock ID differences)
 * 4. Suffix patterns: spawn eggs, music discs
 * 5. Fallback: lowercase the Java material name
 *
 * Values that are already a texture path go through {@link #remapTexturePath(String)} instead,
 * which only touches flat vanilla item/block paths.
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

        String known = resolveKnown(n);
        if (known != null) return known;

        // 4. Fallback: simple toLowerCase
        return "textures/" + category(n) + "/" + n.toLowerCase();
    }

    /**
     * Resolve a material name using only the explicit knowledge in this class - the generated atlas
     * table, the irregular map and the suffix patterns. Unlike {@link #resolve(String)} this returns
     * null instead of guessing, so callers can tell "Bedrock renamed this" apart from "no idea".
     *
     * @param materialName Bukkit Material name
     * @return a Bedrock texture path, or null when only the lowercase fallback would apply
     */
    public static String resolveKnown(String materialName) {
        if (materialName == null || materialName.isBlank()) return null;
        String n = materialName.toUpperCase().trim();
        if (AIR_MATERIALS.contains(n)) return null;

        // 1. Generated atlas table — only holds materials whose plain guess is not a real texture,
        //    so this can replace a broken icon but never one that already renders.
        String atlas = BedrockTextureMap.get(n);
        if (atlas != null) return atlas;

        // 2. Explicit irregular mapping
        String hit = IRREGULAR.get(n);
        if (hit != null) return "textures/" + category(n) + "/" + hit;

        // 3. Suffix patterns
        String pattern;
        if ((pattern = trySpawnEgg(n)) != null) return "textures/items/" + pattern;
        if ((pattern = tryMusicDisc(n)) != null) return "textures/items/" + pattern;

        return null;
    }

    /** Vanilla item/block folders in both the Bedrock (plural) and Java (singular) conventions. */
    private static final Set<String> VANILLA_ITEM_FOLDERS = Set.of("items", "item", "blocks", "block");

    /**
     * Normalise an explicit {@code textures/...} path written in a menu config.
     *
     * <p>The path is taken as written - {@code textures/ui/...}, {@code textures/entity/...} and any
     * custom resource-pack folder are returned untouched, because only the pack author knows what
     * lives there. The single exception is a flat vanilla item or block path whose file name is a
     * Java material that Bedrock renamed ({@code textures/items/cooked_chicken}), which is rewritten
     * to the real atlas entry. Anything the atlas has no opinion about stays as typed, so a custom
     * texture dropped into {@code textures/items/} still renders.
     *
     * @param path an image value already known to start with {@code textures/}
     * @return the path to hand to the client
     */
    public static String remapTexturePath(String path) {
        if (path == null) return null;
        String trimmed = path.trim();
        if (!trimmed.startsWith(TEXTURES_PREFIX)) return trimmed;

        int slash = trimmed.indexOf('/', TEXTURES_PREFIX.length());
        if (slash < 0) return trimmed;
        if (!VANILLA_ITEM_FOLDERS.contains(trimmed.substring(TEXTURES_PREFIX.length(), slash))) return trimmed;

        String name = trimmed.substring(slash + 1);
        if (name.isEmpty() || name.indexOf('/') >= 0) return trimmed;

        String known = resolveKnown(name);
        return known != null ? known : trimmed;
    }

    private static final String TEXTURES_PREFIX = "textures/";

    // ─── Potions and tipped arrows ───────────────────────────────────

    /**
     * Java potion-type aliases → the canonical effect name used by {@link PotionTextureMap}.
     * Bukkit has renamed several of these across versions (JUMP → LEAPING, INSTANT_HEAL → HEALING),
     * so both spellings are accepted.
     */
    private static final Map<String, String> POTION_ALIASES = Map.ofEntries(
            Map.entry("SWIFTNESS", "SPEED"),
            Map.entry("JUMP", "LEAPING"),
            Map.entry("INSTANT_HEAL", "HEALING"),
            Map.entry("INSTANT_DAMAGE", "HARMING"),
            Map.entry("REGEN", "REGENERATION"),
            Map.entry("CONFUSION", "NAUSEA"),
            Map.entry("SLOW_FALL", "SLOW_FALLING"),
            Map.entry("DAMAGE_BOOST", "STRENGTH"),
            Map.entry("MOVE_SPEED", "SPEED"),
            Map.entry("MOVE_SLOWDOWN", "SLOWNESS"),
            Map.entry("DIG_SPEED", "HASTE"),
            Map.entry("DIG_SLOWDOWN", "MINING_FATIGUE"),
            Map.entry("FIRE_RES", "FIRE_RESISTANCE"),
            Map.entry("HEAL", "HEALING"),
            Map.entry("HARM", "HARMING")
    );

    /**
     * Resolve the texture for a potion, splash/lingering potion or tipped arrow.
     *
     * <p>Bedrock ships a separate texture per effect and names it differently per container -
     * a speed potion is {@code potion_bottle_moveSpeed}, a speed arrow is {@code tipped_arrow_swift}.
     * Effects Bedrock has no artwork for fall back to the plain container texture.
     *
     * @param materialName POTION, SPLASH_POTION, LINGERING_POTION or TIPPED_ARROW
     * @param potionType   Bukkit PotionType name (LONG_/STRONG_ prefixes are ignored), may be null
     * @return a Bedrock texture path, or null when the material is not a potion container
     */
    public static String resolvePotion(String materialName, String potionType) {
        if (materialName == null) return null;
        String material = materialName.toUpperCase().trim();
        String base = PotionTextureMap.base(material);
        if (base == null) return null;
        String effect = canonicalPotionEffect(potionType);
        if (effect == null) return base;
        String variant = PotionTextureMap.variant(material, effect);
        return variant != null ? variant : base;
    }

    /** @return the canonical effect name, or null for water/mundane/thick/awkward and unknown input */
    private static String canonicalPotionEffect(String potionType) {
        if (potionType == null || potionType.isBlank()) return null;
        String type = potionType.toUpperCase().trim();
        int colon = type.indexOf(':');
        if (colon >= 0) type = type.substring(colon + 1);          // accept "minecraft:strong_healing"
        if (type.startsWith("LONG_")) type = type.substring(5);
        if (type.startsWith("STRONG_")) type = type.substring(7);
        if (type.isEmpty() || BASE_POTION_TYPES.contains(type)) return null;
        return POTION_ALIASES.getOrDefault(type, type);
    }

    private static final Set<String> BASE_POTION_TYPES = Set.of(
            "WATER", "MUNDANE", "THICK", "AWKWARD", "UNCRAFTABLE", "EMPTY", "LUCK"
    );

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
     * already-resolved Bedrock paths, potion containers written as {@code POTION:HEALING}
     * or {@code TIPPED_ARROW:LONG_POISON}, and player names.
     */
    public static String resolveImage(String image) {
        if (image == null || image.isBlank()) return null;
        String trimmed = image.trim();

        if (trimmed.startsWith(TEXTURES_PREFIX)) return remapTexturePath(trimmed);
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed;

        String icon = resolveIcon(trimmed);
        if (icon != null) return icon;

        if (isLocalImageFile(trimmed)) return trimmed;

        if (trimmed.matches("^[A-Za-z0-9_.\\-]+$"))
            return "https://mc-heads.net/head/" + trimmed + "/64";

        return trimmed;
    }

    /** A Bukkit material name, optionally suffixed with a potion type - never a path or a file name. */
    private static final java.util.regex.Pattern MATERIAL_NAME = java.util.regex.Pattern.compile("[A-Za-z0-9_]+");

    private static final java.util.regex.Pattern LOCAL_IMAGE_FILE =
            java.util.regex.Pattern.compile("[A-Za-z0-9_./\\-]+\\.(png|jpg|jpeg|gif|webp)", java.util.regex.Pattern.CASE_INSENSITIVE);

    /**
     * Resolve a value that is meant to name an item rather than point at a file.
     *
     * <p>Only bare material names ({@code DIAMOND_SWORD}) and potion containers written as
     * {@code POTION:HEALING} are answered. Anything holding a path separator, a file extension or a
     * namespace is rejected with null, so the caller can pass it through untouched instead of having
     * {@link #resolve(String)} - a total function - turn it into a bogus {@code textures/items/...}.
     *
     * @return a Bedrock texture path, or null when the value does not name a material
     */
    public static String resolveIcon(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();

        String head = resolveHead(trimmed);
        if (head != null) return head;

        int separator = trimmed.indexOf(':');
        if (separator > 0) {
            String potion = resolvePotion(trimmed.substring(0, separator), trimmed.substring(separator + 1));
            if (potion != null) return potion;
        }

        if (!MATERIAL_NAME.matcher(trimmed).matches()) return null;
        return resolve(trimmed);
    }

    /**
     * Decide how a resolved image value must be sent to the client.
     *
     * <p>Everything this class produces is either an http(s) URL or a path inside the player's
     * resource pack, and the pack root is not always {@code textures/} - a pack is free to keep its
     * artwork under any folder.
     *
     * @param resolved an image value that has already been through {@link #resolveImage(String)}
     * @return true when the value is a URL, false when it is a resource-pack path
     */
    public static boolean isUrl(String resolved) {
        return resolved != null && (resolved.startsWith("http://") || resolved.startsWith("https://"));
    }

    /** @return true when the value names a local image file rather than a material or a head owner */
    public static boolean isLocalImageFile(String value) {
        return value != null && LOCAL_IMAGE_FILE.matcher(value.trim()).matches();
    }

    private static final String HEAD_PREFIX = "head:";

    /** Player name, MHF name, UUID or skin hash - the part mc-heads.net renders a head for. */
    private static final java.util.regex.Pattern HEAD_OWNER = java.util.regex.Pattern.compile("[A-Za-z0-9_.\\-]+");

    /**
     * Resolve an explicit {@code head:<owner>} value to a rendered player head.
     *
     * <p>A bare word cannot be used for this: {@code DIAMOND_SWORD} and {@code Steve} have the same
     * shape, and material lookup wins so existing configs keep their icons. The prefix is how a
     * config asks for a head instead - {@code head:%player%} once the placeholder is replaced.
     *
     * @param value an image value, may be any syntax
     * @return an mc-heads.net URL, or null when the value is not a {@code head:} reference
     */
    public static String resolveHead(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (!trimmed.regionMatches(true, 0, HEAD_PREFIX, 0, HEAD_PREFIX.length())) return null;

        String owner = trimmed.substring(HEAD_PREFIX.length()).trim();
        if (!HEAD_OWNER.matcher(owner).matches()) return null;
        return "https://mc-heads.net/head/" + owner + "/64";
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
