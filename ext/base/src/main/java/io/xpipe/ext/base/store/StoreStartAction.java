package io.xpipe.ext.base.store;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntryRef;

import javafx.beans.value.ObservableValue;

import lombok.Value;

public class StoreStartAction implements ActionProvider {

    @Override
    public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
        return new LeafDataStoreCallSite<StartableStore>() {

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<StartableStore> store) {
                return new Action(store);
            }

            @Override
            public Class<StartableStore> getApplicableClass() {
                return StartableStore.class;
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<StartableStore> store) {
                return AppI18n.observable("start");
            }

            @Override
            public String getIcon(DataStoreEntryRef<StartableStore> store) {
                return "mdi2p-play";
            }

            @Override
            public boolean requiresValidStore() {
                return false;
            }
        };
    }

    @Override
    public BatchDataStoreCallSite<?> getBatchDataStoreCallSite() {
        return new BatchDataStoreCallSite<StartableStore>() {

            @Override
            public ObservableValue<String> getName() {
                return AppI18n.observable("start");
            }

            @Override
            public String getIcon() {
                return "mdi2p-play";
            }

            @Override
            public Class<?> getApplicableClass() {
                return StartableStore.class;
            }

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<StartableStore> store) {
                return new Action(store);
            }
        };
    }

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntryRef<StartableStore> entry;

        @Override
        public void execute() throws Exception {
            entry.getStore().start();
        }
    }
}
