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

public class StoreStartActionProvider
        implements LeafStoreActionProvider<StartableStore>, BatchStoreActionProvider<StartableStore> {

    @Override
    public boolean isMutation() {
        return true;
    }

    @Override
    public Action createAction(DataStoreEntryRef<StartableStore> ref) {
        return Action.builder().ref(ref).build();
    }

    @Override
    public Class<StartableStore> getApplicableClass() {
        return StartableStore.class;
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
    public String getId() {
        return "startStore";
    }

    @Jacksonized
    @SuperBuilder
    static class Action extends StoreAction<StartableStore> {

        @Override
        public void executeImpl() throws Exception {
            ref.getStore().start();
        }
    }
}
