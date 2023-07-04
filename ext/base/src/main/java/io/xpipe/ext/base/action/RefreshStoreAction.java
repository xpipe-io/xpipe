package io.xpipe.ext.base.action;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.store.DataStore;
import javafx.beans.value.ObservableValue;
import lombok.Value;

public class RefreshStoreAction implements ActionProvider  {

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry store;

        @Override
        public boolean requiresJavaFXPlatform() {
            return false;
        }

        @Override
        public void execute() throws Exception {
            store.refresh(true);
        }
    }

    @Override
    public ActionProvider.DataStoreCallSite<?> getDataStoreCallSite() {
        return new ActionProvider.DataStoreCallSite<>() {

            @Override
            public boolean isMajor(DataStore o) {
                return true;
            }

            @Override
            public ActiveType activeType() {
                return ActiveType.ALWAYS_ENABLE;
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
            public ObservableValue<String> getName(DataStore store) {
                return AppI18n.observable("base.refresh");
            }

            @Override
            public String getIcon(DataStore store) {
                return "mdal-edit";
            }
        };
    }
}
