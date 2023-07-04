package io.xpipe.ext.base.action;

import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.TerminalHelper;
import io.xpipe.core.store.LaunchableStore;
import lombok.Value;

import java.util.List;
import java.util.UUID;

public class LaunchAction implements ActionProvider {

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry entry;

        @Override
        public boolean requiresJavaFXPlatform() {
            return false;
        }

        @Override
        public void execute() throws Exception {
            var storeName = entry.getName();
            if (entry.getStore() instanceof LaunchableStore s) {
                String command = s.prepareLaunchCommand(storeName);
                if (command == null) {
                    return;
                }

                TerminalHelper.open(storeName, command);
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
                        .getStoreEntry(UUID.fromString(args.get(0)))
                        .orElseThrow();
                return new Action(entry);
            }
        };
    }

    @Override
    public DefaultDataStoreCallSite<?> getDefaultDataStoreCallSite() {
        return new DefaultDataStoreCallSite<LaunchableStore>() {
            @Override
            public boolean isApplicable(LaunchableStore o) {
                return DataStorage.get()
                        .getStoreEntryIfPresent(o)
                        .orElseThrow()
                        .getState()
                        .isUsable();
            }

            @Override
            public ActionProvider.Action createAction(LaunchableStore store) {
                return new Action(
                        DataStorage.get().getStoreEntryIfPresent(store).orElseThrow());
            }

            @Override
            public Class<LaunchableStore> getApplicableClass() {
                return LaunchableStore.class;
            }
        };
    }

    //    @Override
    //    public DataStoreCallSite<?> getDataStoreCallSite() {
    //        return new DataStoreCallSite<LaunchableStore>() {
    //
    //            @Override
    //            public boolean isApplicable(LaunchableStore o) throws Exception {
    //                return DataStorage.get().getStoreEntryIfPresent(o).orElseThrow().getState().isUsable();
    //            }
    //
    //            @Override
    //            public ObservableValue<String> getName(LaunchableStore store) {
    //                return AppI18n.observable("openShell");
    //            }
    //
    //            @Override
    //            public String getIcon(LaunchableStore store) {
    //                return "mdi2c-code-greater-than";
    //            }
    //
    //            @Override
    //            public ActionProvider.Action createAction(LaunchableStore store) {
    //                return new Action(DataStorage.get().getStoreEntryIfPresent(store).orElseThrow());
    //            }
    //
    //            @Override
    //            public Class<LaunchableStore> getApplicableClass() {
    //                return LaunchableStore.class;
    //            }
    //
    //            @Override
    //            public boolean isMajor() {
    //                return true;
    //            }
    //        };
    //    }
}
