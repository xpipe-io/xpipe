package io.xpipe.ext.base.action;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
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
            public Action createAction(DataStoreEntryRef<LaunchableStore> store) {
                return new Action(store.get());
            }

            @Override
            public Class<LaunchableStore> getApplicableClass() {
                return LaunchableStore.class;
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<LaunchableStore> store) {
                return AppI18n.observable("createShortcut");
            }

            @Override
            public String getIcon(DataStoreEntryRef<LaunchableStore> store) {
                return "mdi2c-code-greater-than";
            }

        };
    }
}
