package io.xpipe.ext.proc.action;

import io.xpipe.core.impl.LocalProcessControlProvider;
import io.xpipe.ext.proc.CommandStore;
import io.xpipe.extension.DataStoreActionProvider;
import io.xpipe.extension.I18n;
import io.xpipe.extension.util.XPipeDaemon;
import javafx.beans.value.ObservableValue;

public class LaunchCommandAction implements DataStoreActionProvider<CommandStore> {

    @Override
    public Class<CommandStore> getApplicableClass() {
        return CommandStore.class;
    }

    @Override
    public boolean isActive() throws Exception {
        return true;
    }

    @Override
    public ObservableValue<String> getName(CommandStore store) {
        return I18n.observable("openCommand");
    }

    @Override
    public String getIcon(CommandStore store) {
        return "mdmz-play_arrow";
    }

    @Override
    public boolean isMajor() {
        return true;
    }

    @Override
    public void execute(CommandStore store) throws Exception {
        var storeName = XPipeDaemon.getInstance().getStoreName(store).orElse("");
        String command = store.create().prepareTerminalOpen();
        LocalProcessControlProvider.get().openInTerminal(storeName, command);
    }
}
