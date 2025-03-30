package io.xpipe.app.comp.store;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.ext.DataStoreUsageCategory;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.StorageListener;
import io.xpipe.app.util.DerivedObservableList;
import io.xpipe.app.util.PlatformThread;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;

import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

public class StoreViewState {

    private static StoreViewState INSTANCE;
    private final StringProperty filter = new SimpleStringProperty();

    @Getter
    private final DerivedObservableList<StoreEntryWrapper> allEntries =
            new DerivedObservableList<>(FXCollections.synchronizedObservableList(FXCollections.observableArrayList()), true);

    @Getter
    private final DerivedObservableList<StoreCategoryWrapper> categories =
            new DerivedObservableList<>(FXCollections.synchronizedObservableList(FXCollections.observableArrayList()), true);

    @Getter
    private final IntegerProperty entriesListVisibilityObservable = new SimpleIntegerProperty();

    @Getter
    private final IntegerProperty entriesListUpdateObservable = new SimpleIntegerProperty();

    @Getter
    private final Property<StoreCategoryWrapper> activeCategory = new SimpleObjectProperty<>();

    @Getter
    private final Property<StoreSortMode> sortMode = new SimpleObjectProperty<>();

    @Getter
    private final BooleanProperty batchMode = new SimpleBooleanProperty(true);

    @Getter
    private final DerivedObservableList<StoreEntryWrapper> batchModeSelection =
            new DerivedObservableList<>(FXCollections.observableArrayList(), true);

    @Getter
    private boolean initialized = false;

    @Getter
    private final DerivedObservableList<StoreEntryWrapper> effectiveBatchModeSelection =
            batchModeSelection.filtered(storeEntryWrapper -> {
                if (!storeEntryWrapper.getValidity().getValue().isUsable()) {
                    return false;
                }

                if (storeEntryWrapper.getEntry().getProvider().getUsageCategory() == DataStoreUsageCategory.GROUP) {
                    return false;
                }

                return true;
            });

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
        INSTANCE.updateContent();
        INSTANCE.initFilterListener();
        INSTANCE.initBatchListener();
        INSTANCE.initialized = true;
    }

    public static void reset() {
        if (INSTANCE == null) {
            return;
        }

        var active = INSTANCE.activeCategory.getValue().getCategory();
        if (active != null) {
            AppCache.update("selectedCategory", active.getUuid());
            return;
        }

        INSTANCE = null;
    }

    public static StoreViewState get() {
        return INSTANCE;
    }

    public void selectBatchMode(StoreSection section) {
        var wrapper = section.getWrapper();
        if (wrapper != null && !batchModeSelection.getList().contains(wrapper)) {
            batchModeSelection.getList().add(wrapper);
        }
        if (wrapper == null
                || (wrapper.getValidity().getValue().isUsable()
                        && wrapper.getEntry().getProvider().getUsageCategory() == DataStoreUsageCategory.GROUP)) {
            section.getShownChildren().getList().forEach(c -> selectBatchMode(c));
        }
    }

    public void unselectBatchMode(StoreSection section) {
        var wrapper = section.getWrapper();
        if (wrapper != null) {
            batchModeSelection.getList().remove(wrapper);
        }
        if (wrapper == null
                || (wrapper.getValidity().getValue().isUsable()
                        && wrapper.getEntry().getProvider().getUsageCategory() == DataStoreUsageCategory.GROUP)) {
            section.getShownChildren().getList().forEach(c -> unselectBatchMode(c));
        }
    }

    public boolean isSectionSelected(StoreSection section) {
        if (section.getWrapper() == null) {
            var batchSet = new HashSet<>(batchModeSelection.getList());
            var childSet = section.getShownChildren().getList().stream()
                    .map(s -> s.getWrapper())
                    .toList();
            return batchSet.containsAll(childSet);
        }

        return getBatchModeSelection().getList().contains(section.getWrapper());
    }

    private void updateContent() {
        categories.getList().forEach(c -> c.update());
        allEntries.getList().forEach(e -> e.update());
    }

    private void initSections() {
        try {
            currentTopLevelSection = StoreSection.createTopLevel(
                    allEntries, storeEntryWrapper -> true, filter, activeCategory, entriesListVisibilityObservable, entriesListUpdateObservable);
        } catch (Exception exception) {
            currentTopLevelSection = new StoreSection(
                    null,
                    new DerivedObservableList<>(FXCollections.observableArrayList(), true),
                    new DerivedObservableList<>(FXCollections.observableArrayList(), true),
                    0);
            ErrorEvent.fromThrowable(exception).handle();
        }
    }

    private void initFilterListener() {
        var all = getAllConnectionsCategory();
        filter.addListener((observable, oldValue, newValue) -> {
            categories.getList().forEach(e -> e.update());
            var matchingCats = categories.getList().stream()
                    .filter(storeCategoryWrapper ->
                            storeCategoryWrapper.getRoot().equals(all))
                    .filter(storeCategoryWrapper -> storeCategoryWrapper.getDirectContainedEntries().getList().stream()
                            .anyMatch(wrapper -> wrapper.matchesFilter(newValue)))
                    .toList();
            if (matchingCats.size() == 1) {
                activeCategory.setValue(matchingCats.getFirst());
            }
        });
    }

    private void initBatchListener() {
        allEntries.getList().addListener((ListChangeListener<? super StoreEntryWrapper>) c -> {
            batchModeSelection.getList().removeIf(storeEntryWrapper -> {
                return allEntries.getList().contains(storeEntryWrapper);
            });
        });
    }

    private void initContent() {
        allEntries
                .getList()
                .setAll(FXCollections.observableArrayList(DataStorage.get().getStoreEntries().stream()
                        .map(StoreEntryWrapper::new)
                        .toList()));
        categories
                .getList()
                .setAll(FXCollections.observableArrayList(DataStorage.get().getStoreCategories().stream()
                        .map(StoreCategoryWrapper::new)
                        .toList()));

        sortMode.addListener((observable, oldValue, newValue) -> {
            var cat = getActiveCategory().getValue();
            if (cat == null) {
                return;
            }

            cat.getSortMode().setValue(newValue);
        });
        activeCategory.addListener((observable, oldValue, newValue) -> {
            DataStorage.get().setSelectedCategory(newValue.getCategory());
            sortMode.setValue(newValue.getSortMode().getValue());
        });
        var selected = AppCache.getNonNull("selectedCategory", UUID.class, () -> DataStorage.DEFAULT_CATEGORY_UUID);
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

    public void triggerStoreListVisibilityUpdate() {
        PlatformThread.runLaterIfNeeded(() -> {
            entriesListVisibilityObservable.set(entriesListVisibilityObservable.get() + 1);
        });
    }

    public void triggerStoreListUpdate() {
        PlatformThread.runLaterIfNeeded(() -> {
            entriesListUpdateObservable.set(entriesListUpdateObservable.get() + 1);
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
            public void onStoreListUpdate() {
                Platform.runLater(() -> {
                    triggerStoreListUpdate();
                });
            }

            @Override
            public void onStoreAdd(DataStoreEntry... entry) {
                Platform.runLater(() -> {
                    var l = Arrays.stream(entry)
                            .map(StoreEntryWrapper::new)
                            .peek(storeEntryWrapper -> storeEntryWrapper.update())
                            .toList();

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
                    l.forEach(storeEntryWrapper -> storeEntryWrapper.update());
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
                Platform.runLater(() -> {
                    // Don't update anything if we have already reset
                    if (INSTANCE == null) {
                        return;
                    }

                    l.update();
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

            @Override
            public void onEntryCategoryChange(DataStoreCategory from, DataStoreCategory to) {
                Platform.runLater(() -> {
                    synchronized (this) {
                        categories.getList().forEach(storeCategoryWrapper -> storeCategoryWrapper.update());
                    }
                });
            }
        });
    }

    public Optional<StoreSection> getSectionForWrapper(StoreEntryWrapper wrapper) {
        if (currentTopLevelSection == null) {
            return Optional.empty();
        }

        StoreSection current = getCurrentTopLevelSection();
        while (true) {
            var child = current.getAllChildren().getList().stream()
                    .filter(section -> section.getWrapper().equals(wrapper))
                    .findFirst();
            if (child.isPresent()) {
                return child;
            }

            var traverse = current.getAllChildren().getList().stream()
                    .filter(section -> section.anyMatches(w -> w.equals(wrapper)))
                    .findFirst();
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

                var p1 = o1.getParent();
                var p2 = o2.getParent();
                if (p1 == null && p2 == null) {
                    return 0;
                }

                if (p1 == null) {
                    return -1;
                }

                if (p2 == null) {
                    return 1;
                }

                if (o1.getDepth() > o2.getDepth()) {
                    if (p1 == o2) {
                        return 1;
                    }

                    return compare(p1, o2);
                }

                if (o1.getDepth() < o2.getDepth()) {
                    if (p2 == o1) {
                        return -1;
                    }

                    return compare(o1, p2);
                }

                var parent = compare(p1, p2);
                if (parent != 0) {
                    return parent;
                }

                return o1.nameProperty()
                        .getValue()
                        .compareToIgnoreCase(o2.nameProperty().getValue());
            }
        };
        return categories
                .filtered(cat -> root == null || cat.getRoot().equals(root))
                .sorted(comparator);
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

    public StoreCategoryWrapper getAllIdentitiesCategory() {
        return categories.getList().stream()
                .filter(storeCategoryWrapper ->
                        storeCategoryWrapper.getCategory().getUuid().equals(DataStorage.ALL_IDENTITIES_CATEGORY_UUID))
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
