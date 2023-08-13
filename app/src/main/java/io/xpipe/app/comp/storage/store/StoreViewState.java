package io.xpipe.app.comp.storage.store;

import io.xpipe.app.comp.storage.StorageFilter;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.StorageListener;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class StoreViewState {

    private static StoreViewState INSTANCE;

    private final StorageFilter filter = new StorageFilter();

    private final ObservableList<StoreEntryWrapper> allEntries =
            FXCollections.observableList(new CopyOnWriteArrayList<>());
    private final ObservableList<StoreEntryWrapper> shownEntries =
            FXCollections.observableList(new CopyOnWriteArrayList<>());

    @Getter
    private final Property<StoreSortMode> sortMode;

    private StoreViewState() {
        var val = AppCache.getIfPresent("sortMode", String.class)
                .flatMap(StoreSortMode::fromId)
                .orElse(StoreSortMode.DATE_ASC);
        this.sortMode = new SimpleObjectProperty<>(val);
        this.sortMode.addListener((observable, oldValue, newValue) -> {
            AppCache.update("sortMode", newValue.getId());
        });

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
        allEntries.setAll(FXCollections.observableArrayList(DataStorage.get().getStoreEntries().stream()
                .map(StoreEntryWrapper::new)
                .toList()));

        DataStorage.get().addListener(new StorageListener() {
            @Override
            public void onStoreAdd(DataStoreEntry... entry) {
                var l = Arrays.stream(entry).map(StoreEntryWrapper::new).toList();
                Platform.runLater(() -> {
                    allEntries.addAll(l);
                });
            }

            @Override
            public void onStoreRemove(DataStoreEntry... entry) {
                var a = Arrays.stream(entry).collect(Collectors.toSet());
                var l = StoreViewState.get().getAllEntries().stream()
                        .filter(storeEntryWrapper -> a.contains(storeEntryWrapper.getEntry()))
                        .toList();
                Platform.runLater(() -> {
                    allEntries.removeAll(l);
                });
            }
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
}
