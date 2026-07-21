package it.pintux.life.homesteadaddon.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class Formatting {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    private Formatting() {}

    public static String bank(double amount) {
        return String.format("%,.2f", amount);
    }

    public static String date(long epochMillis) {
        if (epochMillis <= 0) {
            return "?";
        }
        return DATE.format(Instant.ofEpochMilli(epochMillis));
    }
}
