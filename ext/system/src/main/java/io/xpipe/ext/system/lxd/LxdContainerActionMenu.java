package io.xpipe.ext.system.lxd;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.ext.base.store.StorePauseAction;
import io.xpipe.ext.base.store.StoreRestartAction;
import io.xpipe.ext.base.store.StoreStartAction;
import io.xpipe.ext.base.store.StoreStopAction;
import javafx.beans.value.ObservableValue;

import java.util.List;

public class LxdContainerActionMenu implements ActionProvider {

    @Override
    public BranchDataStoreCallSite<?> getBranchDataStoreCallSite() {
        return new BranchDataStoreCallSite<LxdContainerStore>() {

            @Override
            public Class<LxdContainerStore> getApplicableClass() {
                return LxdContainerStore.class;
            }

            @Override
            public boolean isMajor(DataStoreEntryRef<LxdContainerStore> o) {
                return true;
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<LxdContainerStore> store) {
                return AppI18n.observable("containerActions");
            }

            @Override
            public String getIcon(DataStoreEntryRef<LxdContainerStore> store) {
                return "mdi2p-package-variant-closed";
            }

            @Override
            public List<ActionProvider> getChildren(DataStoreEntryRef<LxdContainerStore> store) {
                return List.of(
                        new StoreStartAction(),
                        new StoreStopAction(),
                        new StorePauseAction(),
                        new StoreRestartAction(),
                        new LxdContainerConsoleAction(),
                        new LxdContainerEditConfigAction(),
                        new LxdContainerEditRunConfigAction());
            }
        };
    }
}
