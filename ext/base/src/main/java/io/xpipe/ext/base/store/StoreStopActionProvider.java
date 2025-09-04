package io.xpipe.ext.base.store;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.action.BatchHubProvider;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.hub.action.StoreActionCategory;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.storage.DataStoreEntryRef;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class StoreStopActionProvider implements HubLeafProvider<StoppableStore>, BatchHubProvider<StoppableStore> {

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.CUSTOM;
    }

    @Override
    public boolean isApplicable(DataStoreEntryRef<StoppableStore> o) {
        return true;
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<StoppableStore> store) {
        return AppI18n.observable("stop");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<StoppableStore> store) {
        return new LabelGraphic.IconGraphic("mdi2s-stop");
    }

    @Override
    public Class<?> getApplicableClass() {
        return StoppableStore.class;
    }

    @Override
    public boolean requiresValidStore() {
        return false;
    }

    @Override
    public ObservableValue<String> getName() {
        return AppI18n.observable("stop");
    }

    @Override
    public LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2s-stop");
    }

    @Override
    public Action createBatchAction(DataStoreEntryRef<StoppableStore> ref) {
        return Action.builder().ref(ref).build();
    }

    @Override
    public String getId() {
        return "stopAction";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<StoppableStore> {

        @Override
        public void executeImpl() throws Exception {
            ref.getStore().stop();
        }

        @Override
        public boolean isMutation() {
            return true;
        }
    }
}
