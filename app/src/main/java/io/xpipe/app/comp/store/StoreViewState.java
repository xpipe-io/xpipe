package io.xpipe.app.comp.store;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.fxcomps.util.ListBindingsHelper;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
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

import java.util.*;
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
    private final Property<StoreCategoryWrapper> activeCategory = new SimpleObjectProperty<>();

    @Getter
    private StoreSection currentTopLevelSection;

    private StoreViewState() {
        initContent();
        addListeners();
    }

    public static void init() {
        if (INSTANCE != null) {
            return;
        }

        INSTANCE = new StoreViewState();
        INSTANCE.updateContent();
        INSTANCE.initSections();
    }

    public static void reset() {
        if (INSTANCE == null) {
            return;
        }

        var active = INSTANCE.activeCategory.getValue().getCategory();
        if (active == null) {
            return;
        }

        AppCache.update("selectedCategory", active.getUuid());
        INSTANCE = null;
    }

    public static StoreViewState get() {
        return INSTANCE;
    }

    private void updateContent() {
        categories.forEach(c -> c.update());
        allEntries.forEach(e -> e.update());
    }

    private void initSections() {
        try {
            currentTopLevelSection =
                    StoreSection.createTopLevel(allEntries, storeEntryWrapper -> true, filter, activeCategory);
        } catch (Exception exception) {
            currentTopLevelSection =
                    new StoreSection(null, FXCollections.emptyObservableList(), FXCollections.emptyObservableList(), 0);
            ErrorEvent.fromThrowable(exception).handle();
        }
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
    }

    private void addListeners() {
        if (AppPrefs.get() != null) {
            AppPrefs.get().condenseConnectionDisplay().addListener((observable, oldValue, newValue) -> {
                Platform.runLater(() -> {
                    synchronized (this) {
                        var l = new ArrayList<>(allEntries);
                        allEntries.clear();
                        allEntries.setAll(l);
                    }
                });
            });
        }

        // Watch out for synchronizing all calls to the entries and categories list!
        DataStorage.get().addListener(new StorageListener() {
            @Override
            public void onStoreAdd(DataStoreEntry... entry) {
                var l = Arrays.stream(entry)
                        .map(StoreEntryWrapper::new)
                        .peek(storeEntryWrapper -> storeEntryWrapper.update())
                        .toList();
                Platform.runLater(() -> {
                    // Don't update anything if we have already reset
                    if (INSTANCE == null) {
                        return;
                    }

                    synchronized (this) {
                        allEntries.addAll(l);
                    }
                    synchronized (this) {
                        categories.stream()
                                .filter(storeCategoryWrapper -> allEntries.stream()
                                        .anyMatch(storeEntryWrapper -> storeEntryWrapper
                                                .getEntry()
                                                .getCategoryUuid()
                                                .equals(storeCategoryWrapper
                                                        .getCategory()
                                                        .getUuid())))
                                .forEach(storeCategoryWrapper -> storeCategoryWrapper.update());
                    }
                });
            }

            @Override
            public void onStoreRemove(DataStoreEntry... entry) {
                var a = Arrays.stream(entry).collect(Collectors.toSet());
                List<StoreEntryWrapper> l;
                synchronized (this) {
                    l = allEntries.stream()
                            .filter(storeEntryWrapper -> a.contains(storeEntryWrapper.getEntry()))
                            .toList();
                }
                List<StoreCategoryWrapper> cats;
                synchronized (this) {
                    cats = categories.stream()
                            .filter(storeCategoryWrapper -> allEntries.stream()
                                    .anyMatch(storeEntryWrapper -> storeEntryWrapper
                                            .getEntry()
                                            .getCategoryUuid()
                                            .equals(storeCategoryWrapper
                                                    .getCategory()
                                                    .getUuid())))
                            .toList();
                }
                Platform.runLater(() -> {
                    // Don't update anything if we have already reset
                    if (INSTANCE == null) {
                        return;
                    }

                    synchronized (this) {
                        allEntries.removeAll(l);
                    }
                    cats.forEach(storeCategoryWrapper -> storeCategoryWrapper.update());
                });
            }

            @Override
            public void onCategoryAdd(DataStoreCategory category) {
                var l = new StoreCategoryWrapper(category);
                l.update();
                Platform.runLater(() -> {
                    // Don't update anything if we have already reset
                    if (INSTANCE == null) {
                        return;
                    }

                    synchronized (this) {
                        categories.add(l);
                    }
                    l.update();
                });
            }

            @Override
            public void onCategoryRemove(DataStoreCategory category) {
                Optional<StoreCategoryWrapper> found;
                synchronized (this) {
                    found = categories.stream()
                            .filter(storeCategoryWrapper ->
                                    storeCategoryWrapper.getCategory().equals(category))
                            .findFirst();
                }
                if (found.isEmpty()) {
                    return;
                }

                Platform.runLater(() -> {
                    // Don't update anything if we have already reset
                    if (INSTANCE == null) {
                        return;
                    }

                    synchronized (this) {
                        categories.remove(found.get());
                    }
                    var p = found.get().getParent();
                    if (p != null) {
                        p.update();
                    }
                });
            }
        });
    }

    public ObservableList<StoreCategoryWrapper> getSortedCategories(StoreCategoryWrapper root) {
        Comparator<StoreCategoryWrapper> comparator = new Comparator<>() {
            @Override
            public int compare(StoreCategoryWrapper o1, StoreCategoryWrapper o2) {
                var o1Root = o1.getRoot();
                var o2Root = o2.getRoot();

                if (o1Root.equals(getAllConnectionsCategory()) && !o1Root.equals(o2Root)) {
                    return -1;
                }

                if (o2Root.equals(getAllConnectionsCategory()) && !o1Root.equals(o2Root)) {
                    return 1;
                }

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
        return ListBindingsHelper.filteredContentBinding(
                        categories, cat -> root == null || cat.getRoot().equals(root))
                .sorted(comparator);
    }

    public StoreCategoryWrapper getAllConnectionsCategory() {
        return categories.stream()
                .filter(storeCategoryWrapper ->
                        storeCategoryWrapper.getCategory().getUuid().equals(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID))
                .findFirst()
                .orElseThrow();
    }

    public StoreCategoryWrapper getAllScriptsCategory() {
        return categories.stream()
                .filter(storeCategoryWrapper ->
                        storeCategoryWrapper.getCategory().getUuid().equals(DataStorage.ALL_SCRIPTS_CATEGORY_UUID))
                .findFirst()
                .orElseThrow();
    }

    public StoreEntryWrapper getEntryWrapper(DataStoreEntry entry) {
        return allEntries.stream()
                .filter(storeCategoryWrapper -> storeCategoryWrapper.getEntry().equals(entry))
                .findFirst()
                .orElseThrow();
    }

    public StoreCategoryWrapper getCategoryWrapper(DataStoreCategory entry) {
        return categories.stream()
                .filter(storeCategoryWrapper ->
                        storeCategoryWrapper.getCategory().equals(entry))
                .findFirst()
                .orElseThrow();
    }

    public Property<String> getFilterString() {
        return filter;
    }
}
