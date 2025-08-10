package io.xpipe.ext.base.store;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.action.BatchHubProvider;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.hub.action.StoreActionCategory;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LabelGraphic;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class StoreStartActionProvider implements HubLeafProvider<StartableStore>, BatchHubProvider<StartableStore> {

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.CUSTOM;
    }

    @Override
    public boolean isApplicable(DataStoreEntryRef<StartableStore> o) {
        return true;
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<StartableStore> store) {
        return AppI18n.observable("start");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<StartableStore> store) {
        return new LabelGraphic.IconGraphic("mdi2p-play");
    }

    @Override
    public Class<StartableStore> getApplicableClass() {
        return StartableStore.class;
    }

    @Override
    public boolean requiresValidStore() {
        return false;
    }

    @Override
    public ObservableValue<String> getName() {
        return AppI18n.observable("start");
    }

    @Override
    public LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2p-play");
    }

    @Override
    public Action createBatchAction(DataStoreEntryRef<StartableStore> ref) {
        return Action.builder().ref(ref).build();
    }

    @Override
    public String getId() {
        return "startStore";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<StartableStore> {

        @Override
        public void executeImpl() throws Exception {
            ref.getStore().start();
        }

        @Override
        public boolean isMutation() {
            return true;
        }
    }
}
