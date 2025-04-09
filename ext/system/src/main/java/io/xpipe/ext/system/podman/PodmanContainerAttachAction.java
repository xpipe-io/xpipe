package io.xpipe.ext.system.podman;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.terminal.TerminalLauncher;
import io.xpipe.app.util.LabelGraphic;

import javafx.beans.value.ObservableValue;

public class PodmanContainerAttachAction implements ActionProvider {

    @Override
    public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
        return new LeafDataStoreCallSite<PodmanContainerStore>() {

            @Override
            public Action createAction(DataStoreEntryRef<PodmanContainerStore> store) {
                return () -> {
                    var d = store.getStore();
                    var view = d.commandView(
                            d.getCmd().getStore().getHost().getStore().getOrStartSession());
                    TerminalLauncher.open(store.get().getName(), view.attach(d.getContainerName()));
                };
            }

            @Override
            public Class<PodmanContainerStore> getApplicableClass() {
                return PodmanContainerStore.class;
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<PodmanContainerStore> store) {
                return AppI18n.observable("attachContainer");
            }

            @Override
            public LabelGraphic getIcon(DataStoreEntryRef<PodmanContainerStore> store) {
                return new LabelGraphic.IconGraphic("mdi2a-attachment");
            }
        };
    }
}
