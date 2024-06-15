package io.xpipe.app.comp.store;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.fxcomps.util.DerivedObservableList;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.StorageListener;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class StoreViewState {

    private static StoreViewState INSTANCE;
    private final StringProperty filter = new SimpleStringProperty();

    @Getter
    private final DerivedObservableList<StoreEntryWrapper> allEntries =
            new DerivedObservableList<>(FXCollections.observableList(new CopyOnWriteArrayList<>()), true);

    @Getter
    private final DerivedObservableList<StoreCategoryWrapper> categories =
            new DerivedObservableList<>(FXCollections.observableList(new CopyOnWriteArrayList<>()), true);

    @Getter
    private final IntegerProperty entriesOrderChangeObservable = new SimpleIntegerProperty();

    @Getter
    private final IntegerProperty entriesListChangeObservable = new SimpleIntegerProperty();

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
        categories.getList().forEach(c -> c.update());
        allEntries.getList().forEach(e -> e.update());
    }

    private void initSections() {
        try {
            currentTopLevelSection =
                    StoreSection.createTopLevel(allEntries, storeEntryWrapper -> true, filter, activeCategory);
        } catch (Exception exception) {
            currentTopLevelSection =
                    new StoreSection(null,
                            new DerivedObservableList<>(FXCollections.observableArrayList(), true),
                            new DerivedObservableList<>(FXCollections.observableArrayList(), true),
                            0);
            ErrorEvent.fromThrowable(exception).handle();
        }
    }

    private void initContent() {
        allEntries.getList().setAll(FXCollections.observableArrayList(DataStorage.get().getStoreEntries().stream()
                .map(StoreEntryWrapper::new)
                .toList()));
        categories.getList().setAll(FXCollections.observableArrayList(DataStorage.get().getStoreCategories().stream()
                .map(StoreCategoryWrapper::new)
                .toList()));

        activeCategory.addListener((observable, oldValue, newValue) -> {
            DataStorage.get().setSelectedCategory(newValue.getCategory());
        });
        var selected = AppCache.get("selectedCategory", UUID.class, () -> DataStorage.DEFAULT_CATEGORY_UUID);
        activeCategory.setValue(categories.getList().stream()
                .filter(storeCategoryWrapper ->
                        storeCategoryWrapper.getCategory().getUuid().equals(selected))
                .findFirst()
                .orElse(categories.getList().stream()
                        .filter(storeCategoryWrapper ->
                                storeCategoryWrapper.getCategory().getUuid().equals(DataStorage.DEFAULT_CATEGORY_UUID))
                        .findFirst()
                        .orElseThrow()));
    }

    public void toggleStoreOrderUpdate() {
        PlatformThread.runLaterIfNeeded(() -> {
            entriesOrderChangeObservable.set(entriesOrderChangeObservable.get() + 1);
        });
    }

    public void toggleStoreListUpdate() {
        PlatformThread.runLaterIfNeeded(() -> {
            entriesListChangeObservable.set(entriesListChangeObservable.get() + 1);
        });
    }

    private void addListeners() {
        if (AppPrefs.get() != null) {
            AppPrefs.get().condenseConnectionDisplay().addListener((observable, oldValue, newValue) -> {
                Platform.runLater(() -> {
                    synchronized (this) {
                        var l = new ArrayList<>(allEntries.getList());
                        allEntries.getList().clear();
                        allEntries.getList().setAll(l);
                    }
                });
            });
        }

        // Watch out for synchronizing all calls to the entries and categories list!
        DataStorage.get().addListener(new StorageListener() {

            @Override
            public void onStoreOrderUpdate() {
                Platform.runLater(() -> {
                    toggleStoreOrderUpdate();
                });
            }

            @Override
            public void onStoreListUpdate() {
                Platform.runLater(() -> {
                    toggleStoreListUpdate();
                });
            }

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
                        allEntries.getList().addAll(l);
                    }
                    synchronized (this) {
                        categories.getList().stream()
                                .filter(storeCategoryWrapper -> allEntries.getList().stream()
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
                    l = allEntries.getList().stream()
                            .filter(storeEntryWrapper -> a.contains(storeEntryWrapper.getEntry()))
                            .toList();
                }
                List<StoreCategoryWrapper> cats;
                synchronized (this) {
                    cats = categories.getList().stream()
                            .filter(storeCategoryWrapper -> allEntries.getList().stream()
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
                        allEntries.getList().removeAll(l);
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
                        categories.getList().add(l);
                    }
                    l.update();
                });
            }

            @Override
            public void onCategoryRemove(DataStoreCategory category) {
                Optional<StoreCategoryWrapper> found;
                synchronized (this) {
                    found = categories.getList().stream()
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
                        categories.getList().remove(found.get());
                    }
                    var p = found.get().getParent();
                    if (p != null) {
                        p.update();
                    }
                });
            }
        });
    }

    public Optional<StoreSection> getParentSectionForWrapper(StoreEntryWrapper wrapper) {
        StoreSection current = getCurrentTopLevelSection();
        while (true) {
            var child = current.getAllChildren().getList().stream().filter(section -> section.getWrapper().equals(wrapper)).findFirst();
            if (child.isPresent()) {
                return Optional.of(current);
            }

            var traverse = current.getAllChildren().getList().stream().filter(section -> section.anyMatches(w -> w.equals(wrapper))).findFirst();
            if (traverse.isPresent()) {
                current = traverse.get();
            } else {
                return Optional.empty();
            }
        }
    }

    public DerivedObservableList<StoreCategoryWrapper> getSortedCategories(StoreCategoryWrapper root) {
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

                if (o1.getDepth() > o2.getDepth()) {
                    if (o1.getParent() == o2) {
                        return 1;
                    }

                    return compare(o1.getParent(), o2);
                }

                if (o1.getDepth() < o2.getDepth()) {
                    if (o2.getParent() == o1) {
                        return -1;
                    }

                    return compare(o1, o2.getParent());
                }

                var parent = compare(o1.getParent(), o2.getParent());
                if (parent != 0) {
                    return parent;
                }

                return o1.nameProperty()
                        .getValue()
                        .compareToIgnoreCase(o2.nameProperty().getValue());
            }
        };
        return categories.filtered(cat -> root == null || cat.getRoot().equals(root)).sorted(comparator);
    }

    public StoreCategoryWrapper getAllConnectionsCategory() {
        return categories.getList().stream()
                .filter(storeCategoryWrapper ->
                        storeCategoryWrapper.getCategory().getUuid().equals(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID))
                .findFirst()
                .orElseThrow();
    }

    public StoreCategoryWrapper getAllScriptsCategory() {
        return categories.getList().stream()
                .filter(storeCategoryWrapper ->
                        storeCategoryWrapper.getCategory().getUuid().equals(DataStorage.ALL_SCRIPTS_CATEGORY_UUID))
                .findFirst()
                .orElseThrow();
    }

    public StoreEntryWrapper getEntryWrapper(DataStoreEntry entry) {
        return allEntries.getList().stream()
                .filter(storeCategoryWrapper -> storeCategoryWrapper.getEntry().equals(entry))
                .findFirst()
                .orElseThrow();
    }

    public StoreCategoryWrapper getCategoryWrapper(DataStoreCategory entry) {
        return categories.getList().stream()
                .filter(storeCategoryWrapper ->
                        storeCategoryWrapper.getCategory().equals(entry))
                .findFirst()
                .orElseThrow();
    }

    public Property<String> getFilterString() {
        return filter;
    }
}
