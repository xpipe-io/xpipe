package io.xpipe.ext.proc.action;

import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.ProxyManagerProvider;
import io.xpipe.extension.DataStoreActionProvider;
import io.xpipe.extension.I18n;
import javafx.beans.value.ObservableValue;

public class InstallConnectorAction implements DataStoreActionProvider<ShellStore> {
    @Override
    public Class<ShellStore> getApplicableClass() {
        return ShellStore.class;
    }

    @Override
    public boolean isActive() throws Exception {
        return true;
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

    @Override
    public void execute(ShellStore store) throws Exception {
        try (ShellProcessControl s = store.create().start()) {
            ProxyManagerProvider.get().setup(s);
        }
    }
}
