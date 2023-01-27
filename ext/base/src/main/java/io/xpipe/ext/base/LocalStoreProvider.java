package io.xpipe.ext.base;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.StorageElement;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.OsType;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.extension.DataStoreProvider;
import io.xpipe.extension.util.XPipeDaemon;

import java.util.List;
import java.util.UUID;

public class LocalStoreProvider implements DataStoreProvider {

    @Override
    public String queryInformationString(DataStore store, int length) throws Exception {
        try (var pc = ShellStore.local().create().start()) {
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
        var hasLocal = XPipeDaemon.getInstance().getNamedStores().stream()
                .anyMatch(dataStore -> dataStore instanceof LocalStore);
        if (hasLocal) {
            return;
        }

        var e = DataStoreEntry.createNew(UUID.randomUUID(), "Local Machine", new LocalStore());
        DataStorage.get().addStore(e);
        e.setConfiguration(StorageElement.Configuration.builder()
                .deletable(false)
                .editable(false)
                .refreshable(false)
                .renameable(false)
                .build());
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
