package io.xpipe.extension;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Supplier;

public interface Cache {

    Cache INSTANCE = ServiceLoader.load(Cache.class).findFirst().orElseThrow();

    public static <T> T get(String key, Class<T> type, Supplier<T> notPresent) {
        return INSTANCE.getValue(key, type, notPresent);
    }

    public static <T> Optional<T> getIfPresent(String key, Class<T> type) {
        return Optional.ofNullable(get(key, type, () -> null));
    }

    public static <T> void update(String key, T val) {
        INSTANCE.updateValue(key, key);
    }

    public <T> T getValue(String key, Class<?> type, Supplier<T> notPresent);

    public <T> void updateValue(String key, T val);
}
