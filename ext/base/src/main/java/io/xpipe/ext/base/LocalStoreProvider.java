package io.xpipe.ext.base;

import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.StorageElement;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.OsType;
import io.xpipe.core.store.DataStore;

import java.util.List;
import java.util.UUID;

public class LocalStoreProvider implements DataStoreProvider {

    @Override
    public String queryInformationString(DataStore store, int length) throws Exception {
        try (var pc = LocalStore.getShell().start()) {
            return OsType.getLocal().determineOperatingSystemName(pc);
        }
    }

    @Override
    public String toSummaryString(DataStore store, int length) {
        return "localhost";
    }

    @Override
    public boolean shouldShow() {
        return false;
    }

    @Override
    public void storageInit() throws Exception {
        var hasLocal = DataStorage.get().getUsableStores().stream()
                .anyMatch(dataStore -> dataStore instanceof LocalStore);
        if (hasLocal) {
            return;
        }

        var e = DataStoreEntry.createNew(UUID.randomUUID(), "Local Machine", new LocalStore());
        DataStorage.get().addStoreEntry(e);
        e.setConfiguration(StorageElement.Configuration.builder()
                .deletable(false)
                .editable(false)
                .refreshable(true)
                .renameable(false)
                .build());
        e.refresh(true);
    }

    @Override
    public DataStore defaultStore() {
        return new LocalStore();
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("local", "localhost");
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(LocalStore.class);
    }
}
