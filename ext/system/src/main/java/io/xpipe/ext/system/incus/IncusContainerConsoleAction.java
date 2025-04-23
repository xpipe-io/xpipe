package io.xpipe.ext.system.incus;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.terminal.TerminalLauncher;
import io.xpipe.app.util.LabelGraphic;

import javafx.beans.value.ObservableValue;

import lombok.Value;

public class IncusContainerConsoleAction implements ActionProvider {

    @Override
    public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
        return new LeafDataStoreCallSite<IncusContainerStore>() {

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<IncusContainerStore> store) {
                return new Action(store.get());
            }

            @Override
            public Class<IncusContainerStore> getApplicableClass() {
                return IncusContainerStore.class;
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<IncusContainerStore> store) {
                return AppI18n.observable("serialConsole");
            }

            @Override
            public LabelGraphic getIcon(DataStoreEntryRef<IncusContainerStore> store) {
                return new LabelGraphic.IconGraphic("mdi2c-console");
            }

            @Override
            public boolean requiresValidStore() {
                return false;
            }
        };
    }

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry store;

        @Override
        public void execute() throws Exception {
            var d = (IncusContainerStore) store.getStore();
            var view = new IncusCommandView(
                    d.getInstall().getStore().getHost().getStore().getOrStartSession());
            TerminalLauncher.open(store.getName(), view.console(d.getContainerName()));
        }
    }
}
