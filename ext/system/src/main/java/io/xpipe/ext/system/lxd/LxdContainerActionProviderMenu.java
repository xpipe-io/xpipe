package io.xpipe.ext.system.lxd;

import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.action.BranchStoreActionProvider;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.ext.base.store.StorePauseActionProvider;
import io.xpipe.ext.base.store.StoreRestartActionProvider;
import io.xpipe.ext.base.store.StoreStartActionProvider;
import io.xpipe.ext.base.store.StoreStopActionProvider;

import javafx.beans.value.ObservableValue;

import java.util.List;

public class LxdContainerActionProviderMenu implements BranchStoreActionProvider<LxdContainerStore> {

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
    public LabelGraphic getIcon(DataStoreEntryRef<LxdContainerStore> store) {
        return new LabelGraphic.IconGraphic("mdi2p-package-variant-closed");
    }

    @Override
    public List<ActionProvider> getChildren(DataStoreEntryRef<LxdContainerStore> store) {
        return List.of(
                new StoreStartActionProvider(),
                new StoreStopActionProvider(),
                new StorePauseActionProvider(),
                new StoreRestartActionProvider(),
                new LxdContainerConsoleActionProvider(),
                new LxdContainerEditConfigActionProvider(),
                new LxdContainerEditRunConfigActionProvider());
    }
}
