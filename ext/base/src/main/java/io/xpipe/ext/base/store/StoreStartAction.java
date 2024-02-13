package io.xpipe.ext.base.store;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntryRef;
import javafx.beans.value.ObservableValue;
import lombok.Value;

public class StoreStartAction implements ActionProvider {

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntryRef<StartableStore> entry;

        @Override
        public boolean requiresJavaFXPlatform() {
            return false;
        }

        @Override
        public void execute() throws Exception {
            entry.getStore().start();
        }
    }
    
    @Override
    public DataStoreCallSite<?> getDataStoreCallSite() {
        return new DataStoreCallSite<StartableStore>() {

            @Override
            public boolean isMajor(DataStoreEntryRef<StartableStore> o) {
                return true;
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
            public ActionProvider.Action createAction(DataStoreEntryRef<StartableStore> store) {
                return new Action(store);
            }

            @Override
            public Class<StartableStore> getApplicableClass() {
                return StartableStore.class;
            }
        };
    }
}
