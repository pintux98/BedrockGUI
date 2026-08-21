package it.pintux.life.essentialsaddon.provider;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Reads values out of HuskHomes objects without compiling against them.
 *
 * <p>HuskHomes has changed the shape of the same API across releases: methods that returned a
 * {@code CompletableFuture} now return the value directly, and a home's name has lived on
 * {@code getName()}, on {@code getMeta()} and on the public {@code meta} field. Every shape is
 * accepted here so one rename cannot empty a menu.</p>
 */
final class HuskHomesValues {

    private HuskHomesValues() {
    }

    static Object await(Object value, String call, long timeoutSeconds) throws Exception {
        if (!(value instanceof CompletionStage<?> stage)) {
            return unwrapOptional(value);
        }
        try {
            return unwrapOptional(stage.toCompletableFuture().get(timeoutSeconds, TimeUnit.SECONDS));
        } catch (TimeoutException timeout) {
            throw new IllegalStateException("HuskHomes did not answer " + call + " within "
                    + timeoutSeconds + "s", timeout);
        }
    }

    static Object unwrapOptional(Object value) {
        if (value instanceof Optional<?> optional) {
            return optional.orElse(null);
        }
        return value;
    }

    static List<Object> asList(Object value) {
        List<Object> items = new ArrayList<>();
        if (value == null) {
            return items;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item != null) {
                    items.add(item);
                }
            }
            return items;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                Object item = Array.get(value, i);
                if (item != null) {
                    items.add(item);
                }
            }
            return items;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item != null) {
                    items.add(item);
                }
            }
        }
        return items;
    }

    static String homeName(Object home) {
        if (home == null) {
            return null;
        }
        String direct = text(call(home, "getName", "getIdentifier"));
        if (direct != null) {
            return direct;
        }
        Object meta = call(home, "getMeta");
        if (meta == null) {
            meta = field(home, "meta");
        }
        if (meta != null) {
            String metaName = text(call(meta, "getName"));
            return metaName != null ? metaName : text(field(meta, "name"));
        }
        return null;
    }

    static Object call(Object holder, String... methodNames) {
        if (holder == null) {
            return null;
        }
        for (String methodName : methodNames) {
            try {
                Method method = holder.getClass().getMethod(methodName);
                method.setAccessible(true);
                return method.invoke(holder);
            } catch (Exception | LinkageError ignored) {
            }
        }
        return null;
    }

    static Object field(Object holder, String fieldName) {
        if (holder == null) {
            return null;
        }
        Class<?> type = holder.getClass();
        while (type != null && type != Object.class) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(holder);
            } catch (Exception | LinkageError ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    private static String text(Object value) {
        return value instanceof String string && !string.isBlank() ? string : null;
    }
}
