package io.xpipe.ext.proc.action;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.TerminalProvider;
import io.xpipe.core.store.LaunchableStore;
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
            if (entry.getStore() instanceof LaunchableStore s) {
                String command = s.prepareLaunchCommand();
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
                var entry = DataStorage.get()
                        .getStoreEntryByUuid(UUID.fromString(args.get(1)))
                        .orElseThrow();
                return new Action(entry);
            }
        };
    }

    @Override
    public DataStoreCallSite<?> getDataStoreCallSite() {
        return new DataStoreCallSite<LaunchableStore>() {

            @Override
            public boolean isDefault() {
                return true;
            }

            @Override
            public boolean showIfDisabled() {
                return false;
            }

            @Override
            public ObservableValue<String> getName(LaunchableStore store) {
                return I18n.observable("openShell");
            }

            @Override
            public String getIcon(LaunchableStore store) {
                return "mdi2c-code-greater-than";
            }

            @Override
            public ActionProvider.Action createAction(LaunchableStore store) {
                return new Action(DataStorage.get().getEntryByStore(store).orElseThrow());
            }

            @Override
            public Class<LaunchableStore> getApplicableClass() {
                return LaunchableStore.class;
            }

            @Override
            public boolean isMajor() {
                return true;
            }
        };
    }
}
