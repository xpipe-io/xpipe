package io.xpipe.app.comp.store;

import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Getter
public class StoreCategoryWrapper {

    private final DataStoreCategory root;
    private final int depth;
    private final Property<String> name;
    private final DataStoreCategory category;
    private final Property<Instant> lastAccess;
    private final Property<StoreSortMode> sortMode;
    private final Property<Boolean> share;
    private final ObservableList<StoreCategoryWrapper> children;
    private final ObservableList<StoreEntryWrapper> containedEntries;

    public StoreCategoryWrapper(DataStoreCategory category) {
        var d = 0;
        DataStoreCategory last = category;
        DataStoreCategory p = category;
        while ((p = DataStorage.get()
                        .getStoreCategoryIfPresent(p.getParentCategory())
                        .orElse(null))
                != null) {
            d++;
            last = p;
        }
        depth = d;

        this.root = last;
        this.category = category;
        this.name = new SimpleStringProperty(category.getName());
        this.lastAccess = new SimpleObjectProperty<>(category.getLastAccess());
        this.sortMode = new SimpleObjectProperty<>(category.getSortMode());
        this.share = new SimpleObjectProperty<>(category.isShare());
        this.children = FXCollections.observableArrayList();
        this.containedEntries = FXCollections.observableArrayList();
        setupListeners();
    }

    public StoreCategoryWrapper getRoot() {
        return StoreViewState.get().getCategoryWrapper(root);
    }

    public StoreCategoryWrapper getParent() {
        return StoreViewState.get().getCategories().stream()
                .filter(storeCategoryWrapper ->
                                storeCategoryWrapper.getCategory().getUuid().equals(category.getParentCategory()))
                .findAny().orElse(null);
    }

    public boolean contains(DataStoreEntry entry) {
        return entry.getCategoryUuid().equals(category.getUuid())
                || children.stream().anyMatch(storeCategoryWrapper -> storeCategoryWrapper.contains(entry));
    }

    public void select() {
        PlatformThread.runLaterIfNeeded(() -> {
            StoreViewState.get().getActiveCategory().setValue(this);
        });
    }

    public void delete() {
        DataStorage.get().deleteStoreCategory(category);
    }

    private void setupListeners() {
        name.addListener((c, o, n) -> {
            category.setName(n);
        });

        category.addListener(() -> PlatformThread.runLaterIfNeeded(() -> {
            update();
        }));

        sortMode.addListener((observable, oldValue, newValue) -> {
            category.setSortMode(newValue);
        });

        share.addListener((observable, oldValue, newValue) -> {
            category.setShare(newValue);

            DataStoreCategory p = category;
            if (newValue) {
                while ((p = DataStorage.get()
                        .getStoreCategoryIfPresent(p.getParentCategory())
                        .orElse(null))
                        != null) {
                    p.setShare(true);
                }
            }
        });
    }

    public void update() {
        // Avoid reupdating name when changed from the name property!
        if (!category.getName().equals(name.getValue())) {
            name.setValue(category.getName());
        }

        lastAccess.setValue(category.getLastAccess().minus(Duration.ofMillis(500)));
        sortMode.setValue(category.getSortMode());
        share.setValue(category.isShare());

        containedEntries.setAll(StoreViewState.get().getAllEntries().stream()
                .filter(entry -> contains(entry.getEntry()))
                .toList());
        children.setAll(StoreViewState.get().getCategories().stream()
                .filter(storeCategoryWrapper -> getCategory()
                        .getUuid()
                        .equals(storeCategoryWrapper.getCategory().getParentCategory()))
                .toList());
        Optional.ofNullable(getParent())
                .ifPresent(storeCategoryWrapper -> {
                    storeCategoryWrapper.update();
                });
    }

    public String getName() {
        return name.getValue();
    }

    public Property<String> nameProperty() {
        return name;
    }

    public Instant getLastAccess() {
        return lastAccess.getValue();
    }

    public Property<Instant> lastAccessProperty() {
        return lastAccess;
    }
}
