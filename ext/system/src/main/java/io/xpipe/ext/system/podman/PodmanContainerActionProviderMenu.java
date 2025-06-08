package io.xpipe.ext.system.podman;

import io.xpipe.app.action.BranchStoreActionProvider;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.ext.base.store.StoreRestartActionProvider;
import io.xpipe.ext.base.store.StoreStartActionProvider;
import io.xpipe.ext.base.store.StoreStopActionProvider;

import javafx.beans.value.ObservableValue;

import java.util.List;

public class PodmanContainerActionProviderMenu implements BranchStoreActionProvider<PodmanContainerStore> {

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
            public LabelGraphic getIcon(DataStoreEntryRef<PodmanContainerStore> store) {
                return new LabelGraphic.IconGraphic("mdi2p-package-variant-closed");
            }

            @Override
            public List<ActionProvider> getChildren(DataStoreEntryRef<PodmanContainerStore> store) {
                return List.of(
                        new StoreStartActionProvider(),
                        new StoreStopActionProvider(),
                        new StoreRestartActionProvider(),
                        new PodmanContainerInspectActionProvider(),
                        new PodmanContainerLogsActionProvider(),
                        new PodmanContainerAttachActionProvider());
            }
}
