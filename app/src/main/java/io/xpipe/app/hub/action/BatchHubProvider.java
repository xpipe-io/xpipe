package io.xpipe.app.hub.action;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.core.store.DataStore;

import javafx.beans.value.ObservableValue;
import lombok.SneakyThrows;

import java.util.List;

public interface BatchHubProvider<T extends DataStore> extends ActionProvider {

    ObservableValue<String> getName();

    LabelGraphic getIcon();

    Class<?> getApplicableClass();

    default boolean isApplicable(DataStoreEntryRef<T> o) {
        return true;
    }

    default void execute(List<DataStoreEntryRef<T>> refs) throws Exception {
        createBatchAction(refs).executeAsync();
    }

    default AbstractAction createBatchAction(List<DataStoreEntryRef<T>> refs) {
        var individual = refs.stream()
                .map(ref -> {
                    return createBatchAction(ref);
                })
                .filter(action -> action != null)
                .toList();
        return BatchStoreAction.<T>builder().actions(individual).build();
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    default StoreAction<T> createBatchAction(DataStoreEntryRef<T> ref) {
        var c = getActionClass().orElseThrow();
        var bm = c.getDeclaredMethod("builder");
        bm.setAccessible(true);
        var b = bm.invoke(null);

        if (StoreAction.class.isAssignableFrom(c)) {
            var refMethod = b.getClass().getMethod("ref", DataStoreEntryRef.class);
            refMethod.setAccessible(true);
            refMethod.invoke(b, ref);
        }

        var m = b.getClass().getDeclaredMethod("build");
        m.setAccessible(true);
        var defValue = c.cast(m.invoke(b));
        return (StoreAction<T>) defValue;
    }

    default List<? extends ActionProvider> getChildren(List<DataStoreEntryRef<T>> batch) {
        return List.of();
    }
}
