package it.pintux.life.common.utils;

@FunctionalInterface
public interface PlaceholderResolver {

    String resolve(FormPlayer player, String params);
}
