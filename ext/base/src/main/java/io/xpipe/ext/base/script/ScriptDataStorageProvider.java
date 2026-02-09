package io.xpipe.ext.base.script;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.ext.DataStorageExtensionProvider;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;

public class ScriptDataStorageProvider extends DataStorageExtensionProvider {

    @Override
    public void storageInit() {
        if (AppProperties.get().isNewBuildSession()) {
            var legacyLeftovers = DataStorage.get().getStoreEntries().stream()
                    .filter(entry -> {
                        return entry.getValidity() == DataStoreEntry.Validity.LOAD_FAILED
                                && ("My scripts".equals(entry.getName())
                                        || "Files".equals(entry.getName())
                                        || "Management".equals(entry.getName()));
                    })
                    .toList();
            DataStorage.get().deleteWithChildren(legacyLeftovers.toArray(DataStoreEntry[]::new));
        }

        // Don't regenerate if the user deleted anything
        if (!AppProperties.get().isInitialLaunch()) {
            return;
        }

        if (AppProperties.get().isTest()) {
            return;
        }

        for (PredefinedScriptStore value : PredefinedScriptStore.values()) {
            var previous = DataStorage.get().getStoreEntryIfPresent(value.getUuid());
            var store = value.getScriptStore().get();
            if (previous.isPresent()) {
                DataStorage.get().updateEntryStore(previous.get(), store);
                value.setEntry(previous.get().ref());
            } else {
                var e = DataStoreEntry.createNew(
                        value.getUuid(), DataStorage.PREDEFINED_SCRIPTS_CATEGORY_UUID, value.getName(), store);
                DataStorage.get().addStoreEntryIfNotPresent(e);
                value.setEntry(e.ref());
            }
        }
    }
}
