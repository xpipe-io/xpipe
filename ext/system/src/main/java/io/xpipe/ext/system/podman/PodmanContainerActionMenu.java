package io.xpipe.ext.system.podman;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.ext.base.store.StoreRestartAction;
import io.xpipe.ext.base.store.StoreStartAction;
import io.xpipe.ext.base.store.StoreStopAction;

import javafx.beans.value.ObservableValue;

import java.util.List;

public class PodmanContainerActionMenu implements ActionProvider {

    @Override
    public BranchDataStoreCallSite<?> getBranchDataStoreCallSite() {
        return new BranchDataStoreCallSite<PodmanContainerStore>() {

            @Override
            public Class<PodmanContainerStore> getApplicableClass() {
                return PodmanContainerStore.class;
            }

            @Override
            public boolean isMajor(DataStoreEntryRef<PodmanContainerStore> o) {
                return true;
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<PodmanContainerStore> store) {
                return AppI18n.observable("containerActions");
            }

            @Override
            public String getIcon(DataStoreEntryRef<PodmanContainerStore> store) {
                return "mdi2p-package-variant-closed";
            }

            @Override
            public List<ActionProvider> getChildren(DataStoreEntryRef<PodmanContainerStore> store) {
                return List.of(
                        new StoreStartAction(),
                        new StoreStopAction(),
                        new StoreRestartAction(),
                        new PodmanContainerInspectAction(),
                        new PodmanContainerLogsAction(),
                        new PodmanContainerAttachAction());
            }
        };
    }
}
