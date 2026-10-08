package io.xpipe.app.hub.entry;

import io.xpipe.app.core.AppSizeBreakpoints;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.store.DataStore;

import lombok.Value;

import java.util.HashMap;
import java.util.Map;

@Value
public class StoreEntryActionProviderSelectionState {

    private static int externalChangeCounter;


    DataStoreEntry.Validity validity;
    Map<String, Object> cache;
    Object state;
    DataStore store;
    boolean compact;
    boolean template;
    int changeCounter;

    public static void externalChange() {
        externalChangeCounter++;
    }

    public static StoreEntryActionProviderSelectionState of(DataStoreEntry entry) {
        // The store cache is mutated in place, so copy it
        Map<String, Object> cache;
        synchronized (entry.getStoreCache()) {
            cache = new HashMap<>(entry.getStoreCache());
        }

        return new StoreEntryActionProviderSelectionState(
                entry.getValidity(),
                cache,
                entry.getStorePersistentState(),
                entry.getStore(),
                AppSizeBreakpoints.compactMode().get(),
                entry.isTemplate(),
                externalChangeCounter);
    }
}
