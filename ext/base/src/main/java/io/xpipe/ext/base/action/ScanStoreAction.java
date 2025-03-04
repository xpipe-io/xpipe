package io.xpipe.ext.base.action;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ScanDialog;
import io.xpipe.core.process.ShellTtyState;
import io.xpipe.core.process.SystemState;

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
                return true;
            }

            @Override
            public boolean isApplicable(DataStoreEntryRef<ShellStore> o) {
                var state = o.get().getStorePersistentState();
                if (state instanceof SystemState systemState) {
                    return (systemState.getShellDialect() == null
                                    || systemState
                                            .getShellDialect()
                                            .getDumbMode()
                                            .supportsAnyPossibleInteraction())
                            && (systemState.getTtyState() == null || systemState.getTtyState() == ShellTtyState.NONE);
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
                return "mdi2l-layers-plus";
            }
        };
    }

    @Override
    public BatchDataStoreCallSite<?> getBatchDataStoreCallSite() {
        return new BatchDataStoreCallSite<ShellStore>() {

            @Override
            public ObservableValue<String> getName() {
                return AppI18n.observable("scanConnections");
            }

            @Override
            public String getIcon() {
                return "mdi2l-layers-plus";
            }

            @Override
            public Class<?> getApplicableClass() {
                return ShellStore.class;
            }
        };
    }

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry entry;

        @Override
        public void execute() {
            if (entry == null || entry.getStore() instanceof ShellStore) {
                ScanDialog.showForShellStore(entry);
            }
        }
    }
}
