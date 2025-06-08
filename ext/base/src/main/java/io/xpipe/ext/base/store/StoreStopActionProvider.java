package io.xpipe.ext.base.store;

import io.xpipe.app.hub.action.BatchStoreActionProvider;
import io.xpipe.app.action.LeafStoreActionProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LabelGraphic;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class StoreStopActionProvider implements LeafStoreActionProvider<StoppableStore>, BatchStoreActionProvider<StoppableStore> {

    @Override
    public boolean isMutation() {
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
            public Class<?> getApplicableClass() {
                return StoppableStore.class;
            }

    @Override
            public Action createAction(DataStoreEntryRef<StoppableStore> ref) {
                return Action.builder().ref(ref).build();
            }

    @Override
    public boolean isApplicable(DataStoreEntryRef<StoppableStore> o) {
        return true;
    }

        @Override
    public String getId() {
        return "stopAction";
    }
@Jacksonized
@SuperBuilder
    static class Action extends StoreAction<StoppableStore> {

        @Override
        public void executeImpl() throws Exception {
            ref.getStore().stop();
        }
    }
}
