package io.xpipe.ext.base.action;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.FixedHierarchyStore;
import javafx.beans.value.ObservableValue;
import lombok.Value;

public class RefreshStoreAction implements ActionProvider  {

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry store;

        @Override
        public boolean requiresJavaFXPlatform() {
            return false;
        }

        @Override
        public void execute() throws Exception {
            DataStorage.get().refreshChildren(store);
        }
    }

    @Override
    public DefaultDataStoreCallSite<FixedHierarchyStore> getDefaultDataStoreCallSite() {
        return new DefaultDataStoreCallSite<>() {

            @Override
            public boolean isApplicable(FixedHierarchyStore o) {
                return DataStorage.get().getStoreChildren(DataStorage.get().getStoreEntry(o), true).size() == 0;
            }

            @Override
            public ActionProvider.Action createAction(FixedHierarchyStore store) {
                return new Action(DataStorage.get().getStoreEntry(store));
            }

            @Override
            public Class<FixedHierarchyStore> getApplicableClass() {
                return FixedHierarchyStore.class;
            }
        };
    }

    @Override
    public ActionProvider.DataStoreCallSite<?> getDataStoreCallSite() {
        return new ActionProvider.DataStoreCallSite<FixedHierarchyStore>() {

            @Override
            public boolean isMajor(FixedHierarchyStore o) {
                return true;
            }

            @Override
            public ActiveType activeType() {
                return ActiveType.ALWAYS_ENABLE;
            }

            @Override
            public ActionProvider.Action createAction(FixedHierarchyStore store) {
                return new Action(DataStorage.get().getStoreEntry(store));
            }

            @Override
            public Class<FixedHierarchyStore> getApplicableClass() {
                return FixedHierarchyStore.class;
            }

            @Override
            public ObservableValue<String> getName(FixedHierarchyStore store) {
                return AppI18n.observable("base.refresh");
            }

            @Override
            public String getIcon(FixedHierarchyStore store) {
                return "mdi2r-refresh";
            }
        };
    }
}
