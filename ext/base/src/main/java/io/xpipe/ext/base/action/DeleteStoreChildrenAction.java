package io.xpipe.ext.base.action;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.FixedHierarchyStore;
import io.xpipe.core.store.DataStore;

import javafx.beans.value.ObservableValue;

import lombok.Value;

public class DeleteStoreChildrenAction implements ActionProvider {

    @Override
    public DataStoreCallSite<?> getDataStoreCallSite() {
        return new DataStoreCallSite<>() {

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
                return !(o.getStore() instanceof FixedHierarchyStore)
                        && DataStorage.get().getStoreChildren(o.get()).size() > 1;
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<DataStore> store) {
                return AppI18n.observable("base.deleteChildren");
            }

            @Override
            public String getIcon(DataStoreEntryRef<DataStore> store) {
                return "mdal-delete_outline";
            }
        };
    }

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry store;

        @Override
        public boolean requiresJavaFXPlatform() {
            return false;
        }

        @Override
        public void execute() {
            DataStorage.get().deleteChildren(store);
        }
    }
}
