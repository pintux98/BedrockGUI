package it.pintux.life.common.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Potion / tipped-arrow effect -> Bedrock texture path.
 *
 * <p>Generated from Mojang's vanilla item atlas. Bedrock ships one texture per effect, but names
 * them differently per container: a speed potion is {@code potion_bottle_moveSpeed} while a speed
 * arrow is {@code tipped_arrow_swift}. Effects Bedrock has no artwork for (haste, luck, ...) are
 * absent here and fall back to the plain bottle or arrow.
 */
final class PotionTextureMap {

    private PotionTextureMap() {}

    private static final Map<String, String> VARIANTS = new HashMap<>();
    private static final Map<String, String> BASES = new HashMap<>();

    static {
        BASES.put("LINGERING_POTION", "textures/items/potion_bottle_lingering");
        BASES.put("POTION", "textures/items/potion_bottle_drinkable");
        BASES.put("SPLASH_POTION", "textures/items/potion_bottle_splash");
        BASES.put("TIPPED_ARROW", "textures/items/tipped_arrow");

        VARIANTS.put("POTION|SPEED", "textures/items/potion_bottle_moveSpeed");
        VARIANTS.put("POTION|SLOWNESS", "textures/items/potion_bottle_moveSlowdown");
        VARIANTS.put("POTION|HASTE", "textures/items/potion_bottle_digSpeed");
        VARIANTS.put("POTION|MINING_FATIGUE", "textures/items/potion_bottle_digSlowdown");
        VARIANTS.put("POTION|STRENGTH", "textures/items/potion_bottle_damageBoost");
        VARIANTS.put("POTION|HEALING", "textures/items/potion_bottle_heal");
        VARIANTS.put("POTION|HARMING", "textures/items/potion_bottle_harm");
        VARIANTS.put("POTION|LEAPING", "textures/items/potion_bottle_jump");
        VARIANTS.put("POTION|NAUSEA", "textures/items/potion_bottle_confusion");
        VARIANTS.put("POTION|REGENERATION", "textures/items/potion_bottle_regeneration");
        VARIANTS.put("POTION|RESISTANCE", "textures/items/potion_bottle_resistance");
        VARIANTS.put("POTION|FIRE_RESISTANCE", "textures/items/potion_bottle_fireResistance");
        VARIANTS.put("POTION|WATER_BREATHING", "textures/items/potion_bottle_waterBreathing");
        VARIANTS.put("POTION|INVISIBILITY", "textures/items/potion_bottle_invisibility");
        VARIANTS.put("POTION|BLINDNESS", "textures/items/potion_bottle_blindness");
        VARIANTS.put("POTION|NIGHT_VISION", "textures/items/potion_bottle_nightVision");
        VARIANTS.put("POTION|HUNGER", "textures/items/potion_bottle_hunger");
        VARIANTS.put("POTION|WEAKNESS", "textures/items/potion_bottle_weakness");
        VARIANTS.put("POTION|POISON", "textures/items/potion_bottle_poison");
        VARIANTS.put("POTION|WITHER", "textures/items/potion_bottle_wither");
        VARIANTS.put("POTION|HEALTH_BOOST", "textures/items/potion_bottle_healthBoost");
        VARIANTS.put("POTION|ABSORPTION", "textures/items/potion_bottle_absorption");
        VARIANTS.put("POTION|SATURATION", "textures/items/potion_bottle_saturation");
        VARIANTS.put("POTION|LEVITATION", "textures/items/potion_bottle_levitation");
        VARIANTS.put("POTION|TURTLE_MASTER", "textures/items/potion_bottle_turtleMaster");
        VARIANTS.put("POTION|SLOW_FALLING", "textures/items/potion_bottle_slowFall");
        VARIANTS.put("POTION|WIND_CHARGED", "textures/items/potion_bottle_windCharged");
        VARIANTS.put("POTION|WEAVING", "textures/items/potion_bottle_weaving");
        VARIANTS.put("POTION|OOZING", "textures/items/potion_bottle_oozing");
        VARIANTS.put("POTION|INFESTED", "textures/items/potion_bottle_infested");
        VARIANTS.put("SPLASH_POTION|SPEED", "textures/items/potion_bottle_splash_moveSpeed");
        VARIANTS.put("SPLASH_POTION|SLOWNESS", "textures/items/potion_bottle_splash_moveSlowdown");
        VARIANTS.put("SPLASH_POTION|HASTE", "textures/items/potion_bottle_splash_digSpeed");
        VARIANTS.put("SPLASH_POTION|MINING_FATIGUE", "textures/items/potion_bottle_splash_digSlowdown");
        VARIANTS.put("SPLASH_POTION|STRENGTH", "textures/items/potion_bottle_splash_damageBoost");
        VARIANTS.put("SPLASH_POTION|HEALING", "textures/items/potion_bottle_splash_heal");
        VARIANTS.put("SPLASH_POTION|HARMING", "textures/items/potion_bottle_splash_harm");
        VARIANTS.put("SPLASH_POTION|LEAPING", "textures/items/potion_bottle_splash_jump");
        VARIANTS.put("SPLASH_POTION|NAUSEA", "textures/items/potion_bottle_splash_confusion");
        VARIANTS.put("SPLASH_POTION|REGENERATION", "textures/items/potion_bottle_splash_regeneration");
        VARIANTS.put("SPLASH_POTION|RESISTANCE", "textures/items/potion_bottle_splash_resistance");
        VARIANTS.put("SPLASH_POTION|FIRE_RESISTANCE", "textures/items/potion_bottle_splash_fireResistance");
        VARIANTS.put("SPLASH_POTION|WATER_BREATHING", "textures/items/potion_bottle_splash_waterBreathing");
        VARIANTS.put("SPLASH_POTION|INVISIBILITY", "textures/items/potion_bottle_splash_invisibility");
        VARIANTS.put("SPLASH_POTION|BLINDNESS", "textures/items/potion_bottle_splash_blindness");
        VARIANTS.put("SPLASH_POTION|NIGHT_VISION", "textures/items/potion_bottle_splash_nightVision");
        VARIANTS.put("SPLASH_POTION|HUNGER", "textures/items/potion_bottle_splash_hunger");
        VARIANTS.put("SPLASH_POTION|WEAKNESS", "textures/items/potion_bottle_splash_weakness");
        VARIANTS.put("SPLASH_POTION|POISON", "textures/items/potion_bottle_splash_poison");
        VARIANTS.put("SPLASH_POTION|WITHER", "textures/items/potion_bottle_splash_wither");
        VARIANTS.put("SPLASH_POTION|HEALTH_BOOST", "textures/items/potion_bottle_splash_healthBoost");
        VARIANTS.put("SPLASH_POTION|ABSORPTION", "textures/items/potion_bottle_splash_absorption");
        VARIANTS.put("SPLASH_POTION|SATURATION", "textures/items/potion_bottle_splash_saturation");
        VARIANTS.put("SPLASH_POTION|TURTLE_MASTER", "textures/items/potion_bottle_splash_turtleMaster");
        VARIANTS.put("SPLASH_POTION|SLOW_FALLING", "textures/items/potion_bottle_splash_slowFall");
        VARIANTS.put("SPLASH_POTION|WIND_CHARGED", "textures/items/potion_bottle_splash_windCharged");
        VARIANTS.put("SPLASH_POTION|WEAVING", "textures/items/potion_bottle_splash_weaving");
        VARIANTS.put("SPLASH_POTION|OOZING", "textures/items/potion_bottle_splash_oozing");
        VARIANTS.put("SPLASH_POTION|INFESTED", "textures/items/potion_bottle_splash_infested");
        VARIANTS.put("LINGERING_POTION|SPEED", "textures/items/potion_bottle_lingering_moveSpeed");
        VARIANTS.put("LINGERING_POTION|SLOWNESS", "textures/items/potion_bottle_lingering_moveSlowdown");
        VARIANTS.put("LINGERING_POTION|STRENGTH", "textures/items/potion_bottle_lingering_damageBoost");
        VARIANTS.put("LINGERING_POTION|HEALING", "textures/items/potion_bottle_lingering_heal");
        VARIANTS.put("LINGERING_POTION|HARMING", "textures/items/potion_bottle_lingering_harm");
        VARIANTS.put("LINGERING_POTION|LEAPING", "textures/items/potion_bottle_lingering_jump");
        VARIANTS.put("LINGERING_POTION|REGENERATION", "textures/items/potion_bottle_lingering_regeneration");
        VARIANTS.put("LINGERING_POTION|FIRE_RESISTANCE", "textures/items/potion_bottle_lingering_fireResistance");
        VARIANTS.put("LINGERING_POTION|WATER_BREATHING", "textures/items/potion_bottle_lingering_waterBreathing");
        VARIANTS.put("LINGERING_POTION|INVISIBILITY", "textures/items/potion_bottle_lingering_invisibility");
        VARIANTS.put("LINGERING_POTION|NIGHT_VISION", "textures/items/potion_bottle_lingering_nightVision");
        VARIANTS.put("LINGERING_POTION|WEAKNESS", "textures/items/potion_bottle_lingering_weakness");
        VARIANTS.put("LINGERING_POTION|POISON", "textures/items/potion_bottle_lingering_poison");
        VARIANTS.put("LINGERING_POTION|WITHER", "textures/items/potion_bottle_lingering_wither");
        VARIANTS.put("LINGERING_POTION|TURTLE_MASTER", "textures/items/potion_bottle_lingering_turtleMaster");
        VARIANTS.put("LINGERING_POTION|SLOW_FALLING", "textures/items/potion_bottle_lingering_slowFall");
        VARIANTS.put("LINGERING_POTION|WIND_CHARGED", "textures/items/potion_bottle_lingering_windCharged");
        VARIANTS.put("LINGERING_POTION|WEAVING", "textures/items/potion_bottle_lingering_weaving");
        VARIANTS.put("LINGERING_POTION|OOZING", "textures/items/potion_bottle_lingering_oozing");
        VARIANTS.put("LINGERING_POTION|INFESTED", "textures/items/potion_bottle_lingering_infested");
        VARIANTS.put("TIPPED_ARROW|SPEED", "textures/items/tipped_arrow_swift");
        VARIANTS.put("TIPPED_ARROW|SLOWNESS", "textures/items/tipped_arrow_slow");
        VARIANTS.put("TIPPED_ARROW|STRENGTH", "textures/items/tipped_arrow_strength");
        VARIANTS.put("TIPPED_ARROW|HEALING", "textures/items/tipped_arrow_healing");
        VARIANTS.put("TIPPED_ARROW|HARMING", "textures/items/tipped_arrow_harm");
        VARIANTS.put("TIPPED_ARROW|LEAPING", "textures/items/tipped_arrow_leaping");
        VARIANTS.put("TIPPED_ARROW|REGENERATION", "textures/items/tipped_arrow_regen");
        VARIANTS.put("TIPPED_ARROW|FIRE_RESISTANCE", "textures/items/tipped_arrow_fireres");
        VARIANTS.put("TIPPED_ARROW|WATER_BREATHING", "textures/items/tipped_arrow_waterbreathing");
        VARIANTS.put("TIPPED_ARROW|INVISIBILITY", "textures/items/tipped_arrow_invisibility");
        VARIANTS.put("TIPPED_ARROW|NIGHT_VISION", "textures/items/tipped_arrow_nightvision");
        VARIANTS.put("TIPPED_ARROW|WEAKNESS", "textures/items/tipped_arrow_weakness");
        VARIANTS.put("TIPPED_ARROW|POISON", "textures/items/tipped_arrow_poison");
        VARIANTS.put("TIPPED_ARROW|WITHER", "textures/items/tipped_arrow_wither");
        VARIANTS.put("TIPPED_ARROW|TURTLE_MASTER", "textures/items/tipped_arrow_turtlemaster");
        VARIANTS.put("TIPPED_ARROW|SLOW_FALLING", "textures/items/tipped_arrow_slowfalling");
        VARIANTS.put("TIPPED_ARROW|WIND_CHARGED", "textures/items/tipped_arrow_windCharged");
        VARIANTS.put("TIPPED_ARROW|WEAVING", "textures/items/tipped_arrow_weaving");
        VARIANTS.put("TIPPED_ARROW|OOZING", "textures/items/tipped_arrow_oozing");
        VARIANTS.put("TIPPED_ARROW|INFESTED", "textures/items/tipped_arrow_infested");
    }

    /** @return the plain container texture, or null when the material is not a potion container */
    static String base(String materialName) {
        return BASES.get(materialName);
    }

    static String variant(String materialName, String canonicalEffect) {
        return VARIANTS.get(materialName + "|" + canonicalEffect);
    }
}
