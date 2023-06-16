package io.xpipe.app.browser;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.store.FileSystemStore;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.UUID;

@Getter
public class BrowserSavedState {

    static BrowserSavedState load() {
        BrowserSavedState state = AppCache.get("browser-state", BrowserSavedState.class, () -> {
            return null;
        });
        return state;
    }

    @Value
    @Jacksonized
    @Builder
    public static class RecentEntry {

        String directory;
        Instant time;
    }

    @NonNull
    private final LinkedHashMap<UUID, String> lastSystems;

    public BrowserSavedState() {
        lastSystems = new LinkedHashMap<>();
    }

    public BrowserSavedState(@NonNull LinkedHashMap<UUID, String> lastSystems) {
        this.lastSystems = lastSystems;
    }

    public void save() {
        AppCache.update("browser-state", this);
    }

    public void open(FileSystemStore store) {
        var storageEntry = DataStorage.get().getStoreEntryIfPresent(store);
        storageEntry.ifPresent(entry -> lastSystems.put(entry.getUuid(), null));
    }
}
