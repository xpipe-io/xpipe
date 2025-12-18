package io.xpipe.app.hub.action.impl;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreActionCategory;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.storage.DataStoreEntryRef;
import javafx.beans.value.ObservableValue;

public class RefreshHubLeafProvider implements HubLeafProvider<ShellStore> {

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.CONFIGURATION;
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<ShellStore> store) {
        return AppI18n.observable("refresh");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<ShellStore> store) {
        return new LabelGraphic.IconGraphic("mdi2r-refresh");
    }

    @Override
    public Class<?> getApplicableClass() {
        return ShellStore.class;
    }

    @Override
    public AbstractAction createAction(DataStoreEntryRef<ShellStore> ref) {
        return RefreshActionProvider.Action.builder().ref(ref.asNeeded()).build();
    }
}
