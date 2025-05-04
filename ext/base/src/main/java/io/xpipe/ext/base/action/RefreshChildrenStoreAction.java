package io.xpipe.ext.base.action;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.FixedHierarchyStore;
import io.xpipe.app.util.LabelGraphic;

import javafx.beans.value.ObservableValue;

import lombok.Value;

public class RefreshChildrenStoreAction implements ActionProvider {

    @Override
    public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
        return new LeafDataStoreCallSite<FixedHierarchyStore>() {

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<FixedHierarchyStore> store) {
                return new Action(store.get());
            }

            @Override
            public Class<FixedHierarchyStore> getApplicableClass() {
                return FixedHierarchyStore.class;
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
        };
    }

    @Override
    public DefaultDataStoreCallSite<FixedHierarchyStore> getDefaultDataStoreCallSite() {
        return new DefaultDataStoreCallSite<>() {

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<FixedHierarchyStore> store) {
                return new Action(store.get());
            }

            @Override
            public Class<FixedHierarchyStore> getApplicableClass() {
                return FixedHierarchyStore.class;
            }

            @Override
            public boolean isApplicable(DataStoreEntryRef<FixedHierarchyStore> o) {
                return o.getStore().canManuallyRefresh() && DataStorage.get().getStoreChildren(o.get()).size() == 0;
            }
        };
    }

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry store;

        @Override
        public void execute() {
            DataStorage.get().refreshChildren(store);
        }
    }
}
