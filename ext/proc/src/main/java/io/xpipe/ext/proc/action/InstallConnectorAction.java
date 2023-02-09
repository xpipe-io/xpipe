package io.xpipe.ext.proc.action;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.ProxyManagerProvider;
import io.xpipe.extension.I18n;
import io.xpipe.extension.util.ActionProvider;
import javafx.beans.value.ObservableValue;
import lombok.Value;

public class InstallConnectorAction implements ActionProvider {

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry entry;

        @Override
        public boolean requiresPlatform() {
            return true;
        }

        @Override
        public void execute() throws Exception {
            try (ShellProcessControl s =
                    ((ShellStore) entry.getStore()).create().start()) {
                ProxyManagerProvider.get().setup(s);
            }
        }
    }

    @Override
    public DataStoreCallSite<?> getDataStoreCallSite() {
        return new DataStoreCallSite<ShellStore>() {

            @Override
            public LaunchShortcutAction.Action createAction(ShellStore store) {
                return new LaunchShortcutAction.Action(DataStorage.get().getStore(store));
            }

            @Override
            public Class<ShellStore> getApplicableClass() {
                return ShellStore.class;
            }

            @Override
            public boolean isApplicable(ShellStore o) throws Exception {
                return !ShellStore.isLocal(o);
            }

            @Override
            public ObservableValue<String> getName(ShellStore store) {
                return I18n.observable("installConnector");
            }

            @Override
            public String getIcon(ShellStore store) {
                return "mdi2c-code-greater-than";
            }
        };
    }
}
