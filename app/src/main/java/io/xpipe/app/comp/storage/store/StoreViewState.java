package io.xpipe.app.comp.storage.store;

import io.xpipe.app.comp.storage.StorageFilter;
import io.xpipe.app.storage.DataSourceCollection;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.StorageListener;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.fxcomps.util.BindingsHelper;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.Instant;
import java.util.Comparator;
import java.util.concurrent.CopyOnWriteArrayList;

public class StoreViewState {

    private static StoreViewState INSTANCE;

    private final StorageFilter filter = new StorageFilter();

    private final ObservableList<StoreEntryWrapper> allEntries =
            FXCollections.observableList(new CopyOnWriteArrayList<>());
    private final ObservableList<StoreEntryWrapper> shownEntries =
            FXCollections.observableList(new CopyOnWriteArrayList<>());

    private final ObservableBooleanValue empty =
            BindingsHelper.persist(Bindings.equal(Bindings.size(allEntries), 1));

    private StoreViewState() {
        try {
            addStorageGroupListeners();
            addShownContentChangeListeners();
        } catch (Exception exception) {
            ErrorEvent.fromThrowable(exception).handle();
        }
    }

    public static void init() {
        INSTANCE = new StoreViewState();
    }

    public static void reset() {
        INSTANCE = null;
    }

    public static StoreViewState get() {
        return INSTANCE;
    }

    private void addStorageGroupListeners() {
        allEntries.setAll(FXCollections.observableArrayList(DataStorage.get().getStores().stream()
                .map(StoreEntryWrapper::new)
                .toList()));

        DataStorage.get().addListener(new StorageListener() {
            @Override
            public void onStoreAdd(DataStoreEntry entry) {
                Platform.runLater(() -> {
                    var sg = new StoreEntryWrapper(entry);
                    allEntries.add(sg);
                });
            }

            @Override
            public void onStoreRemove(DataStoreEntry entry) {
                Platform.runLater(() -> {
                    allEntries.removeIf(e -> e.getEntry().equals(entry));
                });
            }

            @Override
            public void onCollectionAdd(DataSourceCollection collection) {}

            @Override
            public void onCollectionRemove(DataSourceCollection collection) {}
        });
    }

    private void addShownContentChangeListeners() {
        filter.createFilterBinding(
                allEntries,
                shownEntries,
                new SimpleObjectProperty<>(Comparator.<StoreEntryWrapper, Instant>comparing(
                                storeEntryWrapper -> storeEntryWrapper.getLastAccess())
                        .reversed()));
    }

    public StorageFilter getFilter() {
        return filter;
    }

    public ObservableValue<String> getFilterString() {
        return filter.filterProperty();
    }

    public ObservableList<StoreEntryWrapper> getAllEntries() {
        return allEntries;
    }

    public ObservableList<StoreEntryWrapper> getShownEntries() {
        return shownEntries;
    }

    public boolean isEmpty() {
        return empty.get();
    }

    public ObservableBooleanValue emptyProperty() {
        return empty;
    }
}
