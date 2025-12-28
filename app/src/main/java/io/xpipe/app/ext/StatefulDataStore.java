package io.xpipe.app.ext;

import io.xpipe.app.storage.DataStateHandler;

import lombok.SneakyThrows;

import java.util.Arrays;

public interface StatefulDataStore<T extends DataStoreState> extends DataStore {

    @SneakyThrows
    default T createDefaultState() {
        var c = getStateClass().getDeclaredMethod("builder");
        c.setAccessible(true);
        var b = c.invoke(null);
        var m = b.getClass().getDeclaredMethod("build");
        m.setAccessible(true);
        return getStateClass().cast(m.invoke(b));
    }

    default T getState() {
        return DataStateHandler.get().getState(this, this::createDefaultState);
    }

    default void setState(T val) {
        DataStateHandler.get().setState(this, val);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    default Class<T> getStateClass() {
        var found = Arrays.stream(getClass().getDeclaredClasses())
                .filter(aClass -> DataStoreState.class.isAssignableFrom(aClass))
                .findAny();
        if (found.isEmpty()) {
            throw new IllegalArgumentException(
                    "Store class " + getClass().getSimpleName() + " does not have a state class set");
        }

        return (Class<T>) found.get();
    }
}
