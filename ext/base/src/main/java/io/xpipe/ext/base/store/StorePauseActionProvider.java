package io.xpipe.ext.base.store;

import io.xpipe.app.action.LeafStoreActionProvider;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.action.BatchStoreActionProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LabelGraphic;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class StorePauseActionProvider
        implements LeafStoreActionProvider<PauseableStore>, BatchStoreActionProvider<PauseableStore> {

    @Override
    public boolean isMutation() {
        return true;
    }

    @Override
    public Action createAction(DataStoreEntryRef<PauseableStore> ref) {
        return Action.builder().ref(ref).build();
    }

    @Override
    public Class<PauseableStore> getApplicableClass() {
        return PauseableStore.class;
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
    static class Action extends StoreAction<PauseableStore> {

        @Override
        public void executeImpl() throws Exception {
            ref.getStore().pause();
        }
    }
}
