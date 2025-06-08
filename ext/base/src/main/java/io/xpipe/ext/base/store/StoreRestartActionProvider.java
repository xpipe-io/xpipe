package io.xpipe.ext.base.store;

import io.xpipe.app.hub.action.BatchStoreActionProvider;
import io.xpipe.app.action.LeafStoreActionProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.core.store.DataStore;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class StoreRestartActionProvider implements LeafStoreActionProvider<DataStore>, BatchStoreActionProvider<DataStore> {

    @Override
    public boolean isMutation() {
        return true;
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
            public Class<?> getApplicableClass() {
                return DataStore.class;
            }

            @Override
            public Action createAction(DataStoreEntryRef<DataStore> ref) {
                return Action.builder().ref(ref).build();
            }

            @Override
            public boolean isApplicable(DataStoreEntryRef<DataStore> o) {
                return o.getStore() instanceof StartableStore && o.getStore() instanceof StoppableStore;
            }

        @Override
    public String getId() {
        return "restartStore";
    }
@Jacksonized
@SuperBuilder
    static class Action extends StoreAction<DataStore> {

        @Override
        public void executeImpl() throws Exception {
            ((StoppableStore) ref.getStore()).stop();
            ((StartableStore) ref.getStore()).start();
        }
    }
}
