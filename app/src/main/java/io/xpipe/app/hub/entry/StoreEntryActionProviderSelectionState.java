package io.xpipe.app.hub.entry;

import io.xpipe.app.core.AppSizeBreakpoints;
import io.xpipe.app.storage.DataStoreEntry;

import lombok.Value;

import java.util.Map;

@Value
public class StoreEntryActionProviderSelectionState {

    private static int externalChangeCounter;


    DataStoreEntry.Validity validity;
    Map<String, Object> cache;
    Object state;
    Class<?> storeClass;
    boolean compact;
    boolean template;
    int changeCounter;

    public static void externalChange() {
        externalChangeCounter++;
    }

    public static StoreEntryActionProviderSelectionState of(DataStoreEntry entry) {

        return new StoreEntryActionProviderSelectionState(
                entry.getValidity(),
                entry.getStoreCache(),
                entry.getStorePersistentState(),
                entry.getStore() != null ? entry.getStore().getClass() : null,
                AppSizeBreakpoints.compactMode().get(),
                entry.isTemplate(),
                externalChangeCounter);
    }
}
