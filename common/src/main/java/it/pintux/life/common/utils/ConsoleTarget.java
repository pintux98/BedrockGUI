package it.pintux.life.common.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public record ConsoleTarget(String player, String[] args) {

    public static ConsoleTarget find(String[] args, Predicate<String> isOnline) {
        for (int i = 0; i < args.length; i++) {
            if (isOnline.test(args[i])) {
                List<String> rest = new ArrayList<>(Arrays.asList(args));
                rest.remove(i);
                return new ConsoleTarget(args[i], rest.toArray(new String[0]));
            }
        }
        return null;
    }
}
