package io.xpipe.ext.proc.action;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.store.ShellStore;
import io.xpipe.extension.I18n;
import io.xpipe.extension.util.ActionProvider;
import io.xpipe.extension.util.DesktopShortcuts;
import io.xpipe.extension.util.XPipeDistributionType;
import javafx.beans.value.ObservableValue;
import lombok.Value;

public class LaunchShortcutAction implements ActionProvider {

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry entry;

        @Override
        public boolean requiresPlatform() {
            return false;
        }

        @Override
        public void execute() throws Exception {
            DesktopShortcuts.create("xpipe://launch/" + entry.getUuid().toString(), entry.getName());
        }
    }

    @Override
    public boolean isActive() throws Exception {
        return XPipeDistributionType.get().supportsURLs();
    }


    @Override
    public DataStoreCallSite<?> getDataStoreCallSite() {
        return new DataStoreCallSite<ShellStore>() {

            @Override
            public Action createAction(ShellStore store) {
                return new Action(DataStorage.get().getStore(store));
            }

            @Override
            public Class<ShellStore> getApplicableClass() {
                return ShellStore.class;
            }

            @Override
            public ObservableValue<String> getName(ShellStore store) {
                return I18n.observable("createShortcut");
            }

            @Override
            public String getIcon(ShellStore store) {
                return "mdi2c-code-greater-than";
            }

            @Override
            public boolean isMajor() {
                return false;
            }
        };
    }
}
