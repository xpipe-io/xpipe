package io.xpipe.app.util;

import io.xpipe.app.core.AppCache;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Supplier;

@Getter
public class CacheableConfiguration<T> {

    private final Class<T> clazz;
    private final String cacheKey;
    private final Supplier<T> def;
    private final ObjectProperty<T> value;

    public CacheableConfiguration(Class<T> clazz, String cacheKey, Supplier<T> def) {
        this.clazz = clazz;
        this.cacheKey = cacheKey;
        this.def = def;
        this.value = new SimpleObjectProperty<>(AppCache.getNonNull(cacheKey, clazz, def));
        value.addListener((observable, oldValue, newValue) -> {
            AppCache.update(cacheKey, newValue);
        });
    }

    public void update(T val) {
        AppCache.update(cacheKey, val);
        value.set(val);
    }

    public T get() {
        return value.get();
    }
}
