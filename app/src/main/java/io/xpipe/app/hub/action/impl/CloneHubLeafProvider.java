package io.xpipe.app.hub.action.impl;

import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.hub.action.StoreActionCategory;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.core.store.DataStore;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.time.Duration;

public class CloneHubLeafProvider implements HubLeafProvider<DataStore> {

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.CONFIGURATION;
    }

    @Override
    public Class<DataStore> getApplicableClass() {
        return DataStore.class;
    }

    @Override
    public boolean isApplicable(DataStoreEntryRef<DataStore> o) {
        return o.get().getProvider().canClone();
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<DataStore> store) {
        return AppI18n.observable("base.clone");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<DataStore> store) {
        return new LabelGraphic.IconGraphic("mdi2c-content-copy");
    }

    @Override
    public String getId() {
        return "cloneStore";
    }

    @Jacksonized
    @SuperBuilder
    static class Action extends StoreAction<DataStore> {

        @Override
        public void executeImpl() {
            var entry = DataStoreEntry.createNew(
                    ref.get().getName() + " (" + AppI18n.get("connectionCopy") + ")", ref.getStore());
            var instant = ref.get().getLastAccess().plus(Duration.ofSeconds(1));
            entry.setLastModified(instant);
            entry.setLastUsed(instant);
            DataStorage.get().addStoreEntryIfNotPresent(entry);
        }
    }
}
