package io.xpipe.app.hub.action.impl;

import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.store.DataStore;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class CategorySelectActionProvider implements ActionProvider {

    @Override
    public String getId() {
        return "selectCategory";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<DataStore> {

        @Override
        public void executeImpl() throws Exception {
            var category = DataStorage.get().getStoreCategory(ref.get());
            var wrapper = StoreViewState.get().getCategoryWrapper(category);
            wrapper.select();
        }
    }
}
