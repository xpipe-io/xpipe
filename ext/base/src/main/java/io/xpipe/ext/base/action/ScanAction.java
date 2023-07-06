package io.xpipe.ext.base.action;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.ScanAlert;
import io.xpipe.core.store.ShellStore;
import javafx.beans.value.ObservableValue;
import lombok.Value;

public class ScanAction implements ActionProvider {

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry entry;

        @Override
        public boolean requiresJavaFXPlatform() {
            return true;
        }

        @Override
        public void execute() {
            ScanAlert.showAsync(entry);
        }
    }

    @Override
    public DataStoreCallSite<?> getDataStoreCallSite() {
        return new DataStoreCallSite<ShellStore>() {

            @Override
            public boolean isMajor(ShellStore o) {
                return DataStoreProviders.byStore(o).shouldHaveChildren();
            }

            @Override
            public ObservableValue<String> getName(ShellStore store) {
                return AppI18n.observable("scanConnections");
            }

            @Override
            public String getIcon(ShellStore store) {
                return "mdi2p-playlist-plus";
            }

            @Override
            public ActionProvider.Action createAction(ShellStore store) {
                return new Action(DataStorage.get().getStoreEntry(store));
            }

            @Override
            public Class<ShellStore> getApplicableClass() {
                return ShellStore.class;
            }
        };
    }
}
