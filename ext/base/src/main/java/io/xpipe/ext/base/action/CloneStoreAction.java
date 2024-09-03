package io.xpipe.ext.base.action;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.DataStore;
import javafx.beans.value.ObservableValue;
import lombok.Value;

import java.time.Duration;

public class CloneStoreAction implements ActionProvider {

    @Override
    public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
        return new LeafDataStoreCallSite<>() {

            @Override
            public boolean isSystemAction() {
                return true;
            }

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<DataStore> store) {
                return new Action(store.get());
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
            public String getIcon(DataStoreEntryRef<DataStore> store) {
                return "mdi2c-content-copy";
            }
        };
    }

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry entry;

        @Override
        public void execute() {
            var entry = DataStoreEntry.createNew(this.entry.getName() + " (Copy)", this.entry.getStore());
            var instant = this.entry.getLastAccess().plus(Duration.ofSeconds(1));
            entry.setLastModified(instant);
            entry.setLastUsed(instant);
            DataStorage.get().addStoreEntryIfNotPresent(entry);
        }
    }
}
