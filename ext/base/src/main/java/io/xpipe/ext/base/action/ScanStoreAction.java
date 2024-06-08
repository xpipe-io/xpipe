package io.xpipe.ext.base.action;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ScanAlert;
import io.xpipe.core.process.ShellStoreState;
import io.xpipe.core.store.ShellStore;

import javafx.beans.value.ObservableValue;

import lombok.Value;

public class ScanStoreAction implements ActionProvider {

    @Override
    public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
        return new LeafDataStoreCallSite<ShellStore>() {

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<ShellStore> store) {
                return new Action(store.get());
            }

            @Override
            public Class<ShellStore> getApplicableClass() {
                return ShellStore.class;
            }

            @Override
            public boolean isMajor(DataStoreEntryRef<ShellStore> o) {
                return o.get().getProvider().shouldHaveChildren();
            }

            @Override
            public boolean isApplicable(DataStoreEntryRef<ShellStore> o) {
                if (!o.get().getProvider().canHaveSubShells()) {
                    return false;
                }

                var state = o.get().getStorePersistentState();
                if (state instanceof ShellStoreState shellStoreState) {
                    return shellStoreState.getShellDialect() == null
                            || shellStoreState.getShellDialect().getDumbMode().supportsAnyPossibleInteraction();
                } else {
                    return true;
                }
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<ShellStore> store) {
                return AppI18n.observable("scanConnections");
            }

            @Override
            public String getIcon(DataStoreEntryRef<ShellStore> store) {
                return "mdi2m-magnify-scan";
            }
        };
    }

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry entry;

        @Override
        public void execute() {
            ScanAlert.showAsync(entry);
        }
    }
}
