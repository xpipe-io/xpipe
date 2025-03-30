package io.xpipe.ext.system.podman;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.FileOpener;

import io.xpipe.app.util.LabelGraphic;
import javafx.beans.value.ObservableValue;

import lombok.Value;

public class PodmanContainerInspectAction implements ActionProvider {

    @Override
    public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
        return new LeafDataStoreCallSite<PodmanContainerStore>() {

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<PodmanContainerStore> store) {
                return new Action(store.get());
            }

            @Override
            public Class<PodmanContainerStore> getApplicableClass() {
                return PodmanContainerStore.class;
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<PodmanContainerStore> store) {
                return AppI18n.observable("inspectContainer");
            }

            @Override
            public LabelGraphic getIcon(DataStoreEntryRef<PodmanContainerStore> store) {
                return new LabelGraphic.IconGraphic("mdi2i-information-outline");
            }
        };
    }

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry store;

        @Override
        public void execute() throws Exception {
            var d = (PodmanContainerStore) store.getStore();
            var view = d.commandView(d.getCmd().getStore().getHost().getStore().getOrStartSession());
            var output = view.inspect(d.getContainerName());
            FileOpener.openReadOnlyString(output);
        }
    }
}
