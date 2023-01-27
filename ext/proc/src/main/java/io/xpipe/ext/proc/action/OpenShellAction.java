package io.xpipe.ext.proc.action;

import io.xpipe.core.impl.LocalProcessControlProvider;
import io.xpipe.core.store.ShellStore;
import io.xpipe.extension.DataStoreActionProvider;
import io.xpipe.extension.I18n;
import io.xpipe.extension.util.XPipeDaemon;
import javafx.beans.value.ObservableValue;

public class OpenShellAction implements DataStoreActionProvider<ShellStore> {

    @Override
    public Class<ShellStore> getApplicableClass() {
        return ShellStore.class;
    }

    @Override
    public boolean isActive() throws Exception {
        return true;
    }

    @Override
    public ObservableValue<String> getName(ShellStore store) {
        return I18n.observable("openShell");
    }

    @Override
    public String getIcon(ShellStore store) {
        return "mdi2c-code-greater-than";
    }

    @Override
    public boolean isMajor() {
        return true;
    }

    @Override
    public void execute(ShellStore store) throws Exception {
        var storeName = XPipeDaemon.getInstance().getStoreName(store).orElse("");
        String command = store.create().prepareTerminalOpen();
        LocalProcessControlProvider.get().openInTerminal(storeName, command);
    }
}
