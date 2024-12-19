package io.xpipe.ext.system.incus;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.ext.base.store.StorePauseAction;
import io.xpipe.ext.base.store.StoreRestartAction;
import io.xpipe.ext.base.store.StoreStartAction;
import io.xpipe.ext.base.store.StoreStopAction;

import javafx.beans.value.ObservableValue;

import java.util.List;

public class IncusContainerActionMenu implements ActionProvider {

    @Override
    public BranchDataStoreCallSite<?> getBranchDataStoreCallSite() {
        return new BranchDataStoreCallSite<IncusContainerStore>() {

            @Override
            public Class<IncusContainerStore> getApplicableClass() {
                return IncusContainerStore.class;
            }

            @Override
            public boolean isMajor(DataStoreEntryRef<IncusContainerStore> o) {
                return true;
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<IncusContainerStore> store) {
                return AppI18n.observable("containerActions");
            }

            @Override
            public String getIcon(DataStoreEntryRef<IncusContainerStore> store) {
                return "mdi2p-package-variant-closed";
            }

            @Override
            public List<ActionProvider> getChildren(DataStoreEntryRef<IncusContainerStore> store) {
                return List.of(
                        new StoreStartAction(),
                        new StoreStopAction(),
                        new StorePauseAction(),
                        new StoreRestartAction(),
                        new IncusContainerConsoleAction(),
                        new IncusContainerEditConfigAction(),
                        new IncusContainerEditRunConfigAction());
            }
        };
    }
}
