package io.xpipe.extension;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Supplier;

public interface Cache {

    Cache INSTANCE = ServiceLoader.load(Cache.class).findFirst().orElseThrow();

    @SuppressWarnings("unchecked")
    public static <T, V extends T> V get(String key, Class<T> type, Supplier<T> notPresent) {
        return (V) INSTANCE.getValue(key, type, notPresent);
    }

    public static <T> Optional<T> getIfPresent(String key, Class<T> type) {
        return Optional.ofNullable(get(key, type, () -> null));
    }

    public static <T> void update(String key, T val) {
        INSTANCE.updateValue(key, val);
    }

    public <T> T getValue(String key, Class<?> type, Supplier<T> notPresent);

    public <T> void updateValue(String key, T val);
}
