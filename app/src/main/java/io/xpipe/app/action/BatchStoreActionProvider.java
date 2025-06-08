package io.xpipe.app.action;

import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.core.store.DataStore;
import javafx.beans.value.ObservableValue;

import java.util.List;

public interface BatchStoreActionProvider<T extends DataStore> extends ActionProvider {

    ObservableValue<String> getName();

    LabelGraphic getIcon();

    Class<?> getApplicableClass();

    default boolean isApplicable(DataStoreEntryRef<T> o) {
        return true;
    }

    default AbstractAction createBatchAction(List<DataStoreEntryRef<T>> stores) {
        var individual = stores.stream().map(ref -> {
            return createAction(ref);
        }).filter(action -> action != null).toList();
        return BatchStoreAction.<T>builder().actions(individual).build();
    }

    default StoreAction<T> createAction(DataStoreEntryRef<T> store) {
        return null;
    }

    default List<? extends ActionProvider> getChildren(List<DataStoreEntryRef<T>> batch) {
        return List.of();
    }
}
