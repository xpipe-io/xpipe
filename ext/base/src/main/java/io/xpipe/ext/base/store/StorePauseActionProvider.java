package io.xpipe.ext.base.store;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.action.BatchHubProvider;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.hub.action.StoreActionCategory;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.platform.LabelGraphic;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class StorePauseActionProvider implements HubLeafProvider<PauseableStore>, BatchHubProvider<PauseableStore> {

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.CUSTOM;
    }

    @Override
    public boolean isApplicable(DataStoreEntryRef<PauseableStore> o) {
        return true;
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<PauseableStore> store) {
        return AppI18n.observable("pause");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<PauseableStore> store) {
        return new LabelGraphic.IconGraphic("mdi2p-pause");
    }

    @Override
    public Class<PauseableStore> getApplicableClass() {
        return PauseableStore.class;
    }

    @Override
    public boolean requiresValidStore() {
        return false;
    }

    @Override
    public ObservableValue<String> getName() {
        return AppI18n.observable("pause");
    }

    @Override
    public LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2p-pause");
    }

    @Override
    public String getId() {
        return "pauseStore";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<PauseableStore> {

        @Override
        public void executeImpl() throws Exception {
            ref.getStore().pause();
        }

        @Override
        public boolean isMutation() {
            return true;
        }
    }
}
