package io.xpipe.ext.base.action;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.DesktopShortcuts;
import io.xpipe.core.store.LaunchableStore;
import javafx.beans.value.ObservableValue;
import lombok.Value;

public class LaunchShortcutAction implements ActionProvider {

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry entry;

        @Override
        public boolean requiresJavaFXPlatform() {
            return false;
        }

        @Override
        public void execute() throws Exception {
            DesktopShortcuts.create("xpipe://launch/" + entry.getUuid().toString(), entry.getName());
        }
    }

    @Override
    public DataStoreCallSite<?> getDataStoreCallSite() {
        return new DataStoreCallSite<LaunchableStore>() {

            @Override
            public Action createAction(LaunchableStore store) {
                return new Action(DataStorage.get().getStoreEntry(store));
            }

            @Override
            public Class<LaunchableStore> getApplicableClass() {
                return LaunchableStore.class;
            }

            @Override
            public ObservableValue<String> getName(LaunchableStore store) {
                return AppI18n.observable("createShortcut");
            }

            @Override
            public String getIcon(LaunchableStore store) {
                return "mdi2c-code-greater-than";
            }

        };
    }
}
