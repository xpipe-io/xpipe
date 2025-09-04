package io.xpipe.ext.base.store;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.hub.action.BatchHubProvider;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.hub.action.StoreActionCategory;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.platform.LabelGraphic;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class StoreRestartActionProvider implements HubLeafProvider<DataStore>, BatchHubProvider<DataStore> {

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.CUSTOM;
    }

    @Override
    public boolean isApplicable(DataStoreEntryRef<DataStore> o) {
        return o.getStore() instanceof StartableStore && o.getStore() instanceof StoppableStore;
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<DataStore> store) {
        return AppI18n.observable("restart");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<DataStore> store) {
        return new LabelGraphic.IconGraphic("mdi2r-restart");
    }

    @Override
    public Class<?> getApplicableClass() {
        return DataStore.class;
    }

    @Override
    public boolean requiresValidStore() {
        return false;
    }

    @Override
    public ObservableValue<String> getName() {
        return AppI18n.observable("restart");
    }

    @Override
    public LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2r-restart");
    }

    @Override
    public String getId() {
        return "restartStore";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<DataStore> {

        @Override
        public void executeImpl() throws Exception {
            ((StoppableStore) ref.getStore()).stop();
            ((StartableStore) ref.getStore()).start();
        }

        @Override
        public boolean isMutation() {
            return true;
        }
    }
}
