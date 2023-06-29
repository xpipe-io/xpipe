package io.xpipe.ext.base.actions;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.store.DataStore;
import javafx.beans.value.ObservableValue;
import lombok.Value;

public class DeleteStoreChildrenAction implements ActionProvider {

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry store;

        @Override
        public boolean requiresJavaFXPlatform() {
            return false;
        }

        @Override
        public void execute() {
            DataStorage.get().deleteChildren(store, true);
        }
    }

    @Override
    public DataStoreCallSite<?> getDataStoreCallSite() {
        return new DataStoreCallSite<>() {

            @Override
            public boolean isMajor() {
                return false;
            }

            @Override
            public ActionProvider.Action createAction(DataStore store) {
                return new Action(DataStorage.get().getStoreEntry(store));
            }

            @Override
            public Class<DataStore> getApplicableClass() {
                return DataStore.class;
            }

            @Override
            public boolean isApplicable(DataStore o) {
                return DataStorage.get()
                                .getLogicalStoreChildren(DataStorage.get().getStoreEntry(o), true)
                                .size()
                        > 1;
            }

            @Override
            public ObservableValue<String> getName(DataStore store) {
                return AppI18n.observable("base.deleteChildren");
            }

            @Override
            public String getIcon(DataStore store) {
                return "mdal-delete_outline";
            }
        };
    }
}
