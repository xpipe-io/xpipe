package io.xpipe.ext.base.action;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ObservableDataStore;
import javafx.beans.value.ObservableValue;
import lombok.Value;

public class ObserveStoreAction implements ActionProvider {

    @Override
    public DataStoreCallSite<?> getDataStoreCallSite() {
        return new DataStoreCallSite<ObservableDataStore>() {

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<ObservableDataStore> store) {
                return new Action(store);
            }

            @Override
            public Class<ObservableDataStore> getApplicableClass() {
                return ObservableDataStore.class;
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<ObservableDataStore> store) {
                return store.getStore().getObserverState()
                        ? AppI18n.observable("base.stopObserve")
                        : AppI18n.observable("base.observe");
            }

            @Override
            public String getIcon(DataStoreEntryRef<ObservableDataStore> store) {
                return store.getStore().getObserverState() ? "mdi2e-eye-off-outline" : "mdi2e-eye-outline";
            }
        };
    }

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntryRef<ObservableDataStore> store;

        @Override
        public boolean requiresJavaFXPlatform() {
            return true;
        }

        @Override
        public void execute() {
            store.getStore().toggleObserverState(!store.getStore().getObserverState());
        }
    }
}
