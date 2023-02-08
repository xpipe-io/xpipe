package io.xpipe.ext.proc.action;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.TerminalProvider;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.extension.I18n;
import io.xpipe.extension.util.ActionProvider;
import javafx.beans.value.ObservableValue;
import lombok.Value;

import java.util.List;
import java.util.UUID;

public class LaunchAction implements ActionProvider {

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry entry;


        @Override
        public boolean requiresPlatform() {
            return false;
        }
        @Override
        public void execute() throws Exception {
            var storeName = entry.getName();
            if (entry.getStore() instanceof ShellStore s) {
                String command = s.create().prepareTerminalOpen();
                TerminalProvider.open(storeName, command);
            }
        }
    }

    @Override
    public LauncherCallSite getLauncherCallSite() {
        return new LauncherCallSite() {
            @Override
            public String getId() {
                return "launch";
            }

            @Override
            public ActionProvider.Action createAction(List<String> args) {
                var entry = DataStorage.get().getStoreEntryByUuid(UUID.fromString(args.get(1))).orElseThrow();
                return new Action(entry);
            }
        };
    }

    @Override
    public DataStoreCallSite<?> getDataStoreCallSite() {
        return new DataStoreCallSite<DataStore>() {

            @Override
            public boolean isApplicable(DataStore o) throws Exception {
                return o instanceof ShellStore;
            }

            @Override
            public ObservableValue<String> getName(DataStore store) {
                return I18n.observable("openShell");
            }

            @Override
            public String getIcon(DataStore store) {
                return "mdi2c-code-greater-than";
            }

            @Override
            public ActionProvider.Action createAction(DataStore store) {
                return new Action(DataStorage.get().getEntryByStore(store).orElseThrow());
            }

            @Override
            public Class<DataStore> getApplicableClass() {
                return DataStore.class;
            }

            @Override
            public boolean isMajor() {
                return true;
            }
        };
    }
}
