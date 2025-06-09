package io.xpipe.app.hub.action;

import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.core.store.DataStore;

import javafx.beans.value.ObservableValue;

public interface StoreActionProvider<T extends DataStore> extends ActionProvider {

    default StoreActionCategory getCategory() {
        return null;
    }

    default boolean isMajor(DataStoreEntryRef<T> o) {
        return false;
    }

    default boolean isApplicable(DataStoreEntryRef<T> o) {
        return true;
    }

    ObservableValue<String> getName(DataStoreEntryRef<T> store);

    LabelGraphic getIcon(DataStoreEntryRef<T> store);

    Class<?> getApplicableClass();
}
