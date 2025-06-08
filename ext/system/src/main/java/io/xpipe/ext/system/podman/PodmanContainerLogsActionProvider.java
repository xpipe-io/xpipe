package io.xpipe.ext.system.podman;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.action.LeafStoreActionProvider;
import io.xpipe.app.action.StoreAction;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.terminal.TerminalLauncher;
import io.xpipe.app.util.LabelGraphic;

import javafx.beans.value.ObservableValue;

import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class PodmanContainerLogsActionProvider implements LeafStoreActionProvider<PodmanContainerStore> {

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
                return AppI18n.observable("containerLogs");
            }

            @Override
            public LabelGraphic getIcon(DataStoreEntryRef<PodmanContainerStore> store) {
                return new LabelGraphic.IconGraphic("mdi2v-view-list-outline");
            }

        @Override
    public String getId() {
        return "openPodmanContainerLogs";
    }
@Jacksonized
@SuperBuilder
    static class Action extends StoreAction<PodmanContainerStore> {

        @Override
        public void executeImpl() throws Exception {
            var d = (PodmanContainerStore) ref.getStore();
            var view = d.commandView(d.getCmd().getStore().getHost().getStore().getOrStartSession());
            TerminalLauncher.open(ref.get().getName(), view.logs(d.getContainerName()));
        }
    }
}
