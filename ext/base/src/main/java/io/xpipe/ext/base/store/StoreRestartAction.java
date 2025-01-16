package io.xpipe.ext.base.store;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.DataStore;

import javafx.beans.value.ObservableValue;

import lombok.Value;

public class StoreRestartAction implements ActionProvider {

    @Override
    public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
        return new LeafDataStoreCallSite<>() {

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<DataStore> store) {
                return new Action(store);
            }

            @Override
            public Class<DataStore> getApplicableClass() {
                return DataStore.class;
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<DataStore> store) {
                return AppI18n.observable("restart");
            }

            @Override
            public String getIcon(DataStoreEntryRef<DataStore> store) {
                return "mdi2r-restart";
            }

            @Override
            public boolean requiresValidStore() {
                return false;
            }

            @Override
            public boolean isApplicable(DataStoreEntryRef<DataStore> o) {
                return o.getStore() instanceof StartableStore && o.getStore() instanceof StoppableStore;
            }
        };
    }

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntryRef<DataStore> entry;

        @Override
        public void execute() throws Exception {
            ((StoppableStore) entry.getStore()).stop();
            ((StartableStore) entry.getStore()).start();
        }
    }
}
