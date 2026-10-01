package io.xpipe.app.hub.entry;

import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.core.AppSizeBreakpoints;
import io.xpipe.app.storage.DataStoreEntry;

import lombok.Value;

@Value
public class StoreEntryActionProviderSelectionState {

    DataStoreEntry.Validity validity;
    Class<?> storeClass;
    boolean compact;
    boolean template;

    public static StoreEntryActionProviderSelectionState of(DataStoreEntry entry) {
        return new StoreEntryActionProviderSelectionState(
                entry.getValidity(),
                entry.getStore() != null ? entry.getStore().getClass() : null,
                AppSizeBreakpoints.compactMode().get(),
                entry.isTemplate());
    }
}
