package io.xpipe.ext.base.action;

import io.xpipe.app.comp.store.StoreCreationComp;
import io.xpipe.app.comp.store.StoreCreationDialog;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.DataStore;

import javafx.beans.value.ObservableValue;

import lombok.Value;

public class EditStoreAction implements ActionProvider {

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
            public ObservableValue<String> getName(DataStoreEntryRef<DataStore> store) {
                return AppI18n.observable("base.edit");
            }

            @Override
            public String getIcon(DataStoreEntryRef<DataStore> store) {
                return "mdal-edit";
            }

            @Override
            public boolean requiresValidStore() {
                return false;
            }
        };
    }

    @Override
    public DefaultDataStoreCallSite<?> getDefaultDataStoreCallSite() {
        return new DefaultDataStoreCallSite<>() {
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
                if (o.get().getValidity() == DataStoreEntry.Validity.LOAD_FAILED) {
                    return false;
                }

                return o.get().getValidity().equals(DataStoreEntry.Validity.INCOMPLETE)
                        || o.get().getProvider().editByDefault();
            }
        };
    }

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry store;

        @Override
        public void execute() {
            StoreCreationDialog.showEdit(store);
        }
    }
}
