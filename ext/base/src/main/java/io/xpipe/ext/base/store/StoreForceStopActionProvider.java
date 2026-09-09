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

public class StoreForceStopActionProvider implements HubLeafProvider<ForceStoppableStore>, BatchHubProvider<ForceStoppableStore> {

    @Override
    public boolean runParallel() {
        return true;
    }

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.CUSTOM;
    }

    @Override
    public boolean isApplicable(DataStoreEntryRef<ForceStoppableStore> o) {
        return true;
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<ForceStoppableStore> store) {
        return AppI18n.observable("forceStop");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<ForceStoppableStore> store) {
        return new LabelGraphic.IconGraphic("mdi2s-stop-circle-outline");
    }

    @Override
    public Class<?> getApplicableClass() {
        return ForceStoppableStore.class;
    }

    @Override
    public boolean requiresValidStore() {
        return false;
    }

    @Override
    public ObservableValue<String> getName() {
        return AppI18n.observable("forceStop");
    }

    @Override
    public LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2s-stop-circle-outline");
    }

    @Override
    public Action createBatchAction(DataStoreEntryRef<ForceStoppableStore> ref) {
        return Action.builder().ref(ref).build();
    }

    @Override
    public String getId() {
        return "forceStopStore";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<ForceStoppableStore> {

        @Override
        public void executeImpl() throws Exception {
            ref.getStore().forceStop();
        }

        @Override
        public boolean isMutation() {
            return true;
        }
    }
}
