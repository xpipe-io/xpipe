package io.xpipe.ext.base.store;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntryRef;

import javafx.beans.value.ObservableValue;

import lombok.Value;

public class StoreStopAction implements ActionProvider {

    @Override
    public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
        return new LeafDataStoreCallSite<StoppableStore>() {

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<StoppableStore> store) {
                return new Action(store);
            }

            @Override
            public Class<StoppableStore> getApplicableClass() {
                return StoppableStore.class;
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<StoppableStore> store) {
                return AppI18n.observable("stop");
            }

            @Override
            public String getIcon(DataStoreEntryRef<StoppableStore> store) {
                return "mdi2s-stop";
            }
        };
    }

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntryRef<StoppableStore> entry;

        @Override
        public void execute() throws Exception {
            entry.getStore().stop();
        }
    }
}
