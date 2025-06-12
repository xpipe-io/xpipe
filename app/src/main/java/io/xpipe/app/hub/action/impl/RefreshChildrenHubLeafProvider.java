package io.xpipe.app.hub.action.impl;

import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.hub.action.StoreActionCategory;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.FixedHierarchyStore;
import io.xpipe.app.util.LabelGraphic;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class RefreshChildrenHubLeafProvider implements HubLeafProvider<FixedHierarchyStore> {

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.OPEN;
    }

    @Override
    public boolean isDefault(DataStoreEntryRef<FixedHierarchyStore> o) {
        return true;
    }

    @Override
    public boolean isMajor(DataStoreEntryRef<FixedHierarchyStore> o) {
        return true;
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<FixedHierarchyStore> store) {
        return AppI18n.observable("base.refresh");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<FixedHierarchyStore> store) {
        return new LabelGraphic.IconGraphic("mdi2r-refresh");
    }

    @Override
    public boolean isApplicable(DataStoreEntryRef<FixedHierarchyStore> o) {
        return o.getStore().canManuallyRefresh();
    }

    @Override
    public Class<FixedHierarchyStore> getApplicableClass() {
        return FixedHierarchyStore.class;
    }

    @Override
    public String getId() {
        return "refreshStoreChildren";
    }

    @Jacksonized
    @SuperBuilder
    static class Action extends StoreAction<FixedHierarchyStore> {

        @Override
        public void executeImpl() {
            DataStorage.get().refreshChildren(ref.get());
        }
    }
}
