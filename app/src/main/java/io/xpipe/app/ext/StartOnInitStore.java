package io.xpipe.app.ext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.icon.SystemIconSource;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ThreadHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface StartOnInitStore extends SelfReferentialStore, DataStore {

    static void init() {
        ThreadHelper.runFailableAsync(() -> {
            var enabled = getEnabledStores();
            for (DataStoreEntry e : DataStorage.get().getStoreEntries()) {
                if (e.getStore() instanceof StartOnInitStore i && e.getValidity().isUsable() && enabled.contains(i.getSelfEntry().ref())) {
                    i.startOnInit();
                }
            }
        });
    }

    static Set<DataStoreEntryRef<?>> getEnabledStores() {
        synchronized (StartOnInitStore.class) {
            var type = TypeFactory.defaultInstance().constructType(new TypeReference<Set<DataStoreEntryRef<?>>>() {});
            Set<DataStoreEntryRef<?>> cached = AppCache.getNonNull("startOnInitStores", type, () -> Set.of());
            return cached;
        }
    }

    static void setEnabledStores(Set<DataStoreEntryRef<?>> stores) {
        synchronized (StartOnInitStore.class) {
            AppCache.update("startOnInitStores", stores);
        }
    }

    default boolean isEnabled() {
        synchronized (StartOnInitStore.class) {
            return getEnabledStores().contains(getSelfEntry().ref());
        }
    }

    default void enable() {
        synchronized (StartOnInitStore.class) {
            var enabled = new HashSet<>(getEnabledStores());
            enabled.add(getSelfEntry().ref());
            setEnabledStores(enabled);
        }
    }

    default void disable() {
        synchronized (StartOnInitStore.class) {
            var enabled = new HashSet<>(getEnabledStores());
            enabled.remove(getSelfEntry().ref());
            setEnabledStores(enabled);
        }
    }

    void startOnInit() throws Exception;
}
