package io.xpipe.ext.base.script;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.FixedHierarchyStore;
import io.xpipe.app.hub.action.BatchHubProvider;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.hub.action.StoreActionCategory;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.ext.base.service.FixedServiceGroupStore;
import javafx.beans.value.ObservableValue;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class ScriptSourceRefreshHubProvider implements HubLeafProvider<ScriptSourceStore> {

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.CUSTOM;
    }

    @Override
    public boolean isMajor() {
        return true;
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<ScriptSourceStore> store) {
        return AppI18n.observable("refreshSource");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<ScriptSourceStore> store) {
        return new LabelGraphic.IconGraphic("mdi2r-refresh");
    }

    @Override
    public Class<?> getApplicableClass() {
        return ScriptSourceStore.class;
    }

    @Override
    public String getId() {
        return "refreshSource";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<ScriptSourceStore> {

        @Override
        public void executeImpl() throws Exception {
            ref.getStore().refresh();
        }
    }
}
