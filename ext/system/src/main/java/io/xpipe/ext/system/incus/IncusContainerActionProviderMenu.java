package io.xpipe.ext.system.incus;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.action.HubBranchProvider;
import io.xpipe.app.hub.action.HubMenuItemProvider;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.ext.base.store.StorePauseActionProvider;
import io.xpipe.ext.base.store.StoreRestartActionProvider;
import io.xpipe.ext.base.store.StoreStartActionProvider;
import io.xpipe.ext.base.store.StoreStopActionProvider;

import javafx.beans.value.ObservableValue;

import java.util.List;

public class IncusContainerActionProviderMenu implements HubBranchProvider<IncusContainerStore> {

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
    public LabelGraphic getIcon(DataStoreEntryRef<IncusContainerStore> store) {
        return new LabelGraphic.IconGraphic("mdi2p-package-variant-closed");
    }

    @Override
    public List<HubMenuItemProvider<?>> getChildren(DataStoreEntryRef<IncusContainerStore> store) {
        return List.of(
                new StoreStartActionProvider(),
                new StoreStopActionProvider(),
                new StorePauseActionProvider(),
                new StoreRestartActionProvider(),
                new IncusContainerConsoleActionProvider(),
                new IncusContainerEditConfigActionProvider(),
                new IncusContainerEditRunConfigActionProvider());
    }
}
