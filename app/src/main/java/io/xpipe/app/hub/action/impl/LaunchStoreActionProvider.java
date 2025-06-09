package io.xpipe.app.hub.action.impl;

import io.xpipe.app.action.LeafStoreActionProvider;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.action.BatchStoreActionProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.core.store.DataStore;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class LaunchStoreActionProvider
        implements LeafStoreActionProvider<DataStore>, BatchStoreActionProvider<DataStore> {

    @Override
    public String getId() {
        return "launch";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<DataStore> {

        @Override
        public void executeImpl() throws Exception {
            var r = ref.get().getProvider().launch(ref.get());
            r.run();
        }
    }

    @Override
    public boolean isDefault(DataStoreEntryRef<DataStore> o) {
        return true;
    }

    @Override
    public Action createAction(DataStoreEntryRef<DataStore> ref) {
        return Action.builder().ref(ref).build();
    }

    @Override
    public ObservableValue<String> getName() {
        return AppI18n.observable("launch");
    }

    @Override
    public LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2p-play");
    }

    @Override
    public Class<DataStore> getApplicableClass() {
        return DataStore.class;
    }

    @Override
    public boolean isApplicable(DataStoreEntryRef<DataStore> o) {
        return o.get().getValidity().isUsable() && (o.get().getProvider().launch(o.get()) != null);
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<DataStore> store) {
        return AppI18n.observable("launch");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<DataStore> store) {
        return new LabelGraphic.IconGraphic("mdi2p-play");
    }
}
