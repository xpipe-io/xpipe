package io.xpipe.ext.system.podman;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.util.LabelGraphic;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class PodmanContainerAttachActionProvider implements HubLeafProvider<PodmanContainerStore> {

    @Override
    public Action createAction(DataStoreEntryRef<PodmanContainerStore> ref) {
        return Action.builder().ref(ref).build();
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

    @Override
    public String getId() {
        return "attachPodmanContainer";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<PodmanContainerStore> {

        @Override
        public void executeImpl() throws Exception {
            var d = ref.getStore();
            var view = d.commandView(d.getCmd().getStore().getHost().getStore().getOrStartSession());
            TerminalLaunch.builder()
                    .entry(ref.get())
                    .title("Attach")
                    .command(view.attach(d.getContainerName()))
                    .launch();
        }
    }
}
