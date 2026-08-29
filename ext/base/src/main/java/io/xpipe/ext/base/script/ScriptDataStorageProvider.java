package io.xpipe.ext.base.script;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.ext.DataStorageExtensionProvider;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;

public class ScriptDataStorageProvider extends DataStorageExtensionProvider {

    @Override
    public void storageInit() {
        // Don't regenerate if the user deleted anything
        if (!AppProperties.get().isInitialLaunch()) {
            return;
        }

        if (AppProperties.get().isTest()) {
            return;
        }

        if (DataStorage.get()
                .getStoreCategoryIfPresent(DataStorage.PREDEFINED_SCRIPTS_CATEGORY_UUID)
                .isPresent()) {
            return;
        }

        var cat = DataStoreCategory.createNew(
                DataStorage.ALL_SCRIPTS_CATEGORY_UUID, DataStorage.PREDEFINED_SCRIPTS_CATEGORY_UUID, "Samples");
        DataStorage.get().addStoreCategory(cat);

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
