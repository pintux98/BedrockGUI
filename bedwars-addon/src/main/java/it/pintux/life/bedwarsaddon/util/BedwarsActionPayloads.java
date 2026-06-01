package it.pintux.life.bedwarsaddon.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Encodes provider ids so they survive the "type:value" action-string format. */
public final class BedwarsActionPayloads {
    private BedwarsActionPayloads() {}

    public static String encode(String raw) {
        if (raw == null) return "";
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static String decode(String encoded) {
        if (encoded == null || encoded.isBlank()) return "";
        try {
            return new String(Base64.getUrlDecoder().decode(encoded.trim()), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }
}
