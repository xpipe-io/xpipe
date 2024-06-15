package io.xpipe.ext.base.script;

import io.xpipe.app.ext.DataStorageExtensionProvider;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ScriptDataStorageProvider extends DataStorageExtensionProvider {

    @Override
    public void storageInit() {
        DataStorage.get()
                .addStoreEntryIfNotPresent(DataStoreEntry.createNew(
                        UUID.fromString("a9945ad2-db61-4304-97d7-5dc4330691a7"),
                        DataStorage.CUSTOM_SCRIPTS_CATEGORY_UUID,
                        "My scripts",
                        ScriptGroupStore.builder().build()));

        for (PredefinedScriptGroup value : PredefinedScriptGroup.values()) {
            ScriptGroupStore store = ScriptGroupStore.builder()
                    .description(value.getDescription())
                    .build();
            var e = DataStorage.get()
                    .addStoreEntryIfNotPresent(DataStoreEntry.createNew(
                            UUID.nameUUIDFromBytes(("a " + value.getName()).getBytes(StandardCharsets.UTF_8)),
                            DataStorage.PREDEFINED_SCRIPTS_CATEGORY_UUID,
                            value.getName(),
                            store));
            e.setStoreInternal(store, false);
            e.setExpanded(value.isExpanded());
            value.setEntry(e.ref());
        }

        for (PredefinedScriptStore value : PredefinedScriptStore.values()) {
            var previous = DataStorage.get().getStoreEntryIfPresent(value.getUuid());
            var store = value.getScriptStore().get();
            if (previous.isPresent()) {
                previous.get().setStoreInternal(store, false);
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
