package io.xpipe.app.action;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.core.store.DataStore;
import lombok.Data;
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
        protected void executeImpl() throws Exception {
            var category = DataStorage.get().getStoreCategory(ref.get());
            var wrapper = StoreViewState.get().getCategoryWrapper(category);
            wrapper.select();
        }
    }
}
