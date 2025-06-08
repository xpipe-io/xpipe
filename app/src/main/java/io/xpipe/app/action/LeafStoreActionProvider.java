package io.xpipe.app.action;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.FailableConsumer;
import javafx.beans.value.ObservableValue;

public interface LeafStoreActionProvider<T extends DataStore> extends StoreActionProvider<T> {

    default boolean isDefault(DataStoreEntryRef<T> o) {
        return false;
    }

    AbstractAction createAction(DataStoreEntryRef<T> ref);

    default boolean requiresValidStore() {
        return true;
    }

    default boolean isSystemAction() {
        return false;
    }
}
