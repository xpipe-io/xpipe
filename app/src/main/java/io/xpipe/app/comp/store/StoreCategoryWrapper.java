package io.xpipe.app.comp.store;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataColor;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.util.PlatformThread;

import javafx.beans.property.*;
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
    private final Property<Boolean> sync;
    private final ObservableList<StoreCategoryWrapper> children;
    private final ObservableList<StoreEntryWrapper> directContainedEntries;
    private final ObservableList<StoreEntryWrapper> allContainedEntries;
    private final BooleanProperty expanded = new SimpleBooleanProperty();
    private final Property<DataColor> color = new SimpleObjectProperty<>();

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
        this.sync = new SimpleObjectProperty<>(category.isSync());
        this.children = FXCollections.observableArrayList();
        this.allContainedEntries = FXCollections.observableArrayList();
        this.directContainedEntries = FXCollections.observableArrayList();
        this.color.setValue(category.getColor());
        setupListeners();
    }

    public StoreCategoryWrapper getRoot() {
        return StoreViewState.get().getCategoryWrapper(root);
    }

    public StoreCategoryWrapper getParent() {
        return StoreViewState.get().getCategories().getList().stream()
                .filter(storeCategoryWrapper ->
                        storeCategoryWrapper.getCategory().getUuid().equals(category.getParentCategory()))
                .findAny()
                .orElse(null);
    }

    public boolean contains(StoreEntryWrapper entry) {
        return entry.getEntry().getCategoryUuid().equals(category.getUuid()) || allContainedEntries.contains(entry);
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
            if (n.equals(translatedName(category.getName()))) {
                return;
            }

            category.setName(n);
        });

        category.addListener(() -> PlatformThread.runLaterIfNeeded(() -> {
            update();
        }));

        AppPrefs.get().showChildCategoriesInParentCategory().addListener((observable, oldValue, newValue) -> {
            update();
        });

        AppPrefs.get().language().addListener((observable, oldValue, newValue) -> {
            update();
        });

        sortMode.addListener((observable, oldValue, newValue) -> {
            category.setSortMode(newValue);
        });

        sync.addListener((observable, oldValue, newValue) -> {
            DataStorage.get().syncCategory(category, newValue);
        });
    }

    public void toggleExpanded() {
        this.expanded.set(!expanded.getValue());
    }

    public void update() {
        // We are probably in shutdown then
        if (StoreViewState.get() == null) {
            return;
        }

        // Avoid reupdating name when changed from the name property!
        var catName = translatedName(category.getName());
        if (!catName.equals(name.getValue())) {
            name.setValue(catName);
        }

        lastAccess.setValue(category.getLastAccess().minus(Duration.ofMillis(500)));
        sortMode.setValue(category.getSortMode());
        sync.setValue(category.isSync());
        expanded.setValue(category.isExpanded());
        color.setValue(category.getColor());

        directContainedEntries.setAll(StoreViewState.get().getAllEntries().getList().stream()
                .filter(entry -> {
                    return entry.getEntry().getCategoryUuid().equals(category.getUuid());
                })
                .toList());
        allContainedEntries.setAll(StoreViewState.get().getAllEntries().getList().stream()
                .filter(entry -> {
                    return entry.getEntry().getCategoryUuid().equals(category.getUuid())
                            || (AppPrefs.get()
                                            .showChildCategoriesInParentCategory()
                                            .get()
                                    && children.stream()
                                            .anyMatch(storeCategoryWrapper -> storeCategoryWrapper.contains(entry)));
                })
                .toList());
        children.setAll(StoreViewState.get().getCategories().getList().stream()
                .filter(storeCategoryWrapper -> getCategory()
                        .getUuid()
                        .equals(storeCategoryWrapper.getCategory().getParentCategory()))
                .toList());
        Optional.ofNullable(getParent()).ifPresent(storeCategoryWrapper -> {
            storeCategoryWrapper.update();
        });
    }

    private String translatedName(String original) {
        if (original.equals("All connections")) {
            return AppI18n.get("allConnections");
        }
        if (original.equals("All scripts")) {
            return AppI18n.get("allScripts");
        }
        if (original.equals("Predefined")) {
            return AppI18n.get("predefined");
        }
        if (original.equals("Custom")) {
            return AppI18n.get("custom");
        }
        if (original.equals("Default")) {
            return AppI18n.get("default");
        }

        return original;
    }

    public Property<String> nameProperty() {
        return name;
    }

    public Property<Instant> lastAccessProperty() {
        return lastAccess;
    }
}
