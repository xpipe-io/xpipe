package io.xpipe.ext.base.action;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.TerminalLauncher;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.LaunchableStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.ext.base.script.ScriptStore;

import javafx.beans.value.ObservableValue;

import lombok.Value;

public class LaunchAction implements ActionProvider {

    @Override
    public String getId() {
        return "launch";
    }

    @Override
    public DataStoreCallSite<?> getDataStoreCallSite() {
        return new DataStoreCallSite<DataStore>() {

            @Override
            public boolean canLinkTo() {
                return true;
            }

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<DataStore> store) {
                return new Action(store.get());
            }

            @Override
            public Class<DataStore> getApplicableClass() {
                return DataStore.class;
            }

            @Override
            public boolean isApplicable(DataStoreEntryRef<DataStore> o) {
                return o.get().getValidity().isUsable()
                        && (o.getStore() instanceof LaunchableStore
                                || o.get().getProvider().launchAction(o.get()) != null);
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<DataStore> store) {
                return AppI18n.observable("launch");
            }

            @Override
            public String getIcon(DataStoreEntryRef<DataStore> store) {
                return "mdi2p-play";
            }
        };
    }

    @Override
    public DefaultDataStoreCallSite<?> getDefaultDataStoreCallSite() {
        return new DefaultDataStoreCallSite<DataStore>() {

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<DataStore> store) {
                return new Action(store.get());
            }

            @Override
            public Class<DataStore> getApplicableClass() {
                return DataStore.class;
            }

            @Override
            public boolean isApplicable(DataStoreEntryRef<DataStore> o) {
                return o.get().getValidity().isUsable()
                        && (o.getStore() instanceof LaunchableStore
                                || o.get().getProvider().launchAction(o.get()) != null);
            }
        };
    }

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry entry;

        @Override
        public boolean requiresJavaFXPlatform() {
            return false;
        }

        @Override
        public void execute() throws Exception {
            var storeName = DataStorage.get().getStoreDisplayName(entry);
            if (entry.getStore() instanceof ShellStore s) {
                TerminalLauncher.open(entry, storeName, null, ScriptStore.controlWithDefaultScripts(s.control()));
                return;
            }

            if (entry.getStore() instanceof LaunchableStore s) {
                s.launch();
                return;
            }

            if (entry.getProvider().launchAction(entry) != null) {
                entry.getProvider().launchAction(entry).execute();
            }
        }
    }
}
