package io.xpipe.app.comp.storage.store;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.StorageListener;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class StoreViewState {

    private static StoreViewState INSTANCE;

    private final StringProperty filter = new SimpleStringProperty();

    @Getter
    private final ObservableList<StoreEntryWrapper> allEntries =
            FXCollections.observableList(new CopyOnWriteArrayList<>());

    @Getter
    private final ObservableList<StoreCategoryWrapper> categories =
            FXCollections.observableList(new CopyOnWriteArrayList<>());

    @Getter
    private final StoreSection topLevelSection;

    @Getter
    private final Property<StoreCategoryWrapper> activeCategory = new SimpleObjectProperty<>();

    private StoreViewState() {
        StoreSection tl;
        try {
            initContent();
            addStorageListeners();
            tl = StoreSection.createTopLevel(allEntries,  storeEntryWrapper -> true, filter, activeCategory);
        } catch (Exception exception) {
            tl = new StoreSection(null, FXCollections.emptyObservableList(), FXCollections.emptyObservableList(), 0);
            categories.setAll(new StoreCategoryWrapper(DataStorage.get().getAllCategory()));
            activeCategory.setValue(getAllCategory());
            ErrorEvent.fromThrowable(exception).handle();
        }
        topLevelSection = tl;
    }

    public ObservableList<StoreCategoryWrapper> getSortedCategories() {
        Comparator<StoreCategoryWrapper> comparator = new Comparator<>() {
            @Override
            public int compare(StoreCategoryWrapper o1, StoreCategoryWrapper o2) {
                if (o1.getParent() == null && o2.getParent() == null) {
                    return 0;
                }

                if (o1.getParent() == null) {
                    return -1;
                }

                if (o2.getParent() == null) {
                    return 1;
                }

                var parent = compare(o1.getParent(), o2.getParent());
                if (parent != 0) {
                    return parent;
                }

                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        };
        return categories.sorted(comparator);
    }

    public StoreCategoryWrapper getAllCategory() {
        return categories.stream()
                .filter(storeCategoryWrapper ->
                        storeCategoryWrapper.getCategory().getUuid().equals(DataStorage.ALL_CATEGORY_UUID))
                .findFirst()
                .orElseThrow();
    }

    public static void init() {
        new StoreViewState();
    }

    public static void reset() {
        AppCache.update(
                "selectedCategory",
                INSTANCE.activeCategory.getValue().getCategory().getUuid());
        INSTANCE = null;
    }

    public static StoreViewState get() {
        return INSTANCE;
    }

    private void initContent() {
        allEntries.setAll(FXCollections.observableArrayList(DataStorage.get().getStoreEntries().stream()
                .map(StoreEntryWrapper::new)
                .toList()));
        categories.setAll(FXCollections.observableArrayList(DataStorage.get().getStoreCategories().stream()
                .map(StoreCategoryWrapper::new)
                .toList()));

        activeCategory.addListener((observable, oldValue, newValue) -> {
            DataStorage.get().setSelectedCategory(newValue.getCategory());
        });
        var selected = AppCache.get("selectedCategory", UUID.class, () -> DataStorage.DEFAULT_CATEGORY_UUID);
        activeCategory.setValue(categories.stream()
                .filter(storeCategoryWrapper ->
                        storeCategoryWrapper.getCategory().getUuid().equals(selected))
                .findFirst()
                .orElse(categories.stream()
                        .filter(storeCategoryWrapper ->
                                storeCategoryWrapper.getCategory().getUuid().equals(DataStorage.DEFAULT_CATEGORY_UUID))
                        .findFirst()
                        .orElseThrow()));

        INSTANCE = this;
        categories.forEach(storeCategoryWrapper -> storeCategoryWrapper.update());
        allEntries.forEach(storeCategoryWrapper -> storeCategoryWrapper.update());
    }

    private void addStorageListeners() {
        DataStorage.get().addListener(new StorageListener() {
            @Override
            public void onStoreAdd(DataStoreEntry... entry) {
                var l = Arrays.stream(entry).map(StoreEntryWrapper::new).toList();
                Platform.runLater(() -> {
                    allEntries.addAll(l);
                    categories.stream()
                            .filter(storeCategoryWrapper -> allEntries.stream()
                                    .anyMatch(storeEntryWrapper -> storeEntryWrapper
                                            .getEntry()
                                            .getCategoryUuid()
                                            .equals(storeCategoryWrapper
                                                            .getCategory()
                                                            .getUuid())))
                            .forEach(storeCategoryWrapper -> storeCategoryWrapper.update());
                });
            }

            @Override
            public void onStoreRemove(DataStoreEntry... entry) {
                var a = Arrays.stream(entry).collect(Collectors.toSet());
                var l = StoreViewState.get().getAllEntries().stream()
                        .filter(storeEntryWrapper -> a.contains(storeEntryWrapper.getEntry()))
                        .toList();
                var cats = categories.stream()
                        .filter(storeCategoryWrapper -> allEntries.stream()
                                .anyMatch(storeEntryWrapper -> storeEntryWrapper
                                        .getEntry()
                                        .getCategoryUuid()
                                        .equals(storeCategoryWrapper
                                                        .getCategory()
                                                        .getUuid()))).toList();
                Platform.runLater(() -> {
                    allEntries.removeAll(l);
                    cats.forEach(storeCategoryWrapper -> storeCategoryWrapper.update());
                });
            }

            @Override
            public void onCategoryAdd(DataStoreCategory category) {
                var l = new StoreCategoryWrapper(category);
                Platform.runLater(() -> {
                    categories.add(l);
                    l.update();
                });
            }

            @Override
            public void onCategoryRemove(DataStoreCategory category) {
                var found = categories.stream()
                        .filter(storeCategoryWrapper ->
                                        storeCategoryWrapper.getCategory().equals(category))
                        .findFirst();
                if (found.isEmpty()) {
                    return;
                }

                Platform.runLater(() -> {
                    categories.remove(found.get());
                    var p = found.get().getParent();
                    if (p != null) {
                        p.update();
                    }
                });
            }
        });
    }

    public Property<String> getFilterString() {
        return filter;
    }

}
