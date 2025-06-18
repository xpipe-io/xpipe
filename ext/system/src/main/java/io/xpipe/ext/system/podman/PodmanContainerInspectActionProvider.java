package io.xpipe.ext.system.podman;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.FileOpener;
import io.xpipe.app.util.LabelGraphic;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class PodmanContainerInspectActionProvider implements HubLeafProvider<PodmanContainerStore> {

    @Override
    public AbstractAction createAction(DataStoreEntryRef<PodmanContainerStore> ref) {
        return Action.builder().ref(ref).build();
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

    @Override
    public String getId() {
        return "inspectPodmanContainer";
    }

    @Jacksonized
    @SuperBuilder
    static class Action extends StoreAction<PodmanContainerStore> {

        @Override
        public void executeImpl() throws Exception {
            var d = (PodmanContainerStore) ref.getStore();
            var view = d.commandView(d.getCmd().getStore().getHost().getStore().getOrStartSession());
            var output = view.inspect(d.getContainerName());
            FileOpener.openReadOnlyString(output);
        }
    }
}
