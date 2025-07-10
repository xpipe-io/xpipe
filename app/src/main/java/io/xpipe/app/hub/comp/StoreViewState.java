package io.xpipe.app.hub.comp;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.ext.DataStoreUsageCategory;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.StorageListener;
import io.xpipe.app.util.DerivedObservableList;
import io.xpipe.app.util.PlatformThread;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
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
            DerivedObservableList.synchronizedArrayList(true);

    @Getter
    private final DerivedObservableList<StoreCategoryWrapper> categories =
            DerivedObservableList.synchronizedArrayList(true);

    @Getter
    private final IntegerProperty entriesListVisibilityObservable = new SimpleIntegerProperty();

    @Getter
    private final IntegerProperty entriesListUpdateObservable = new SimpleIntegerProperty();

    @Getter
    private final Property<StoreCategoryWrapper> activeCategory = new SimpleObjectProperty<>();

    @Getter
    private final Property<StoreSectionSortMode> globalSortMode = new SimpleObjectProperty<>();

    @Getter
    private final Property<StoreSectionSortMode> tieSortMode = new SimpleObjectProperty<>();

    @Getter
    private final BooleanProperty batchMode = new SimpleBooleanProperty(false);

    @Getter
    private final DerivedObservableList<StoreEntryWrapper> batchModeSelection =
            DerivedObservableList.synchronizedArrayList(true);

    private final Set<StoreEntryWrapper> batchModeSelectionSet = new HashSet<>();

    @Getter
    private boolean initialized = false;

    @Getter
    private final DerivedObservableList<StoreEntryWrapper> effectiveBatchModeSelection = batchModeSelection.filtered(
            storeEntryWrapper -> {
                if (!storeEntryWrapper.getValidity().getValue().isUsable()) {
                    return false;
                }

                if (storeEntryWrapper.getEntry().getProvider().getUsageCategory() == DataStoreUsageCategory.GROUP) {
                    return false;
                }

                return true;
            },
            entriesListVisibilityObservable,
            entriesListUpdateObservable);

    @Getter
    private final ObservableValue<Comparator<StoreSection>> effectiveSortMode = Bindings.createObjectBinding(
            () -> {
                var g = globalSortMode.getValue() != null ? globalSortMode.getValue() : null;
                var t = tieSortMode.getValue() != null ? tieSortMode.getValue() : StoreSectionSortMode.DATE_DESC;
                var incomplete = Comparator.<StoreSection>comparingInt(value -> {
                    if (!value.getWrapper().getValidity().getValue().isUsable()) {
                        return 1;
                    }

                    return 0;
                });
                return g != null
                        ? incomplete.thenComparing(g.comparator().thenComparing(t.comparator()))
                        : incomplete.thenComparing(t.comparator());
            },
            globalSortMode,
            tieSortMode);

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
        INSTANCE.initSortMode();
        INSTANCE.updateContent();
        INSTANCE.initSections();
        INSTANCE.updateContent();
        INSTANCE.initFilterListener();
        INSTANCE.initBatchListeners();
        INSTANCE.initialized = true;
    }

    public static void reset() {
        if (INSTANCE == null) {
            return;
        }

        var active = INSTANCE.activeCategory.getValue().getCategory();
        if (active != null) {
            AppCache.update("selectedCategory", active.getUuid());
        }

        var globalMode = INSTANCE.globalSortMode.getValue();
        if (globalMode != null) {
            AppCache.update("globalSortMode", globalMode.getId());
        }

        var tieMode = INSTANCE.tieSortMode.getValue();
        if (tieMode != null) {
            AppCache.update("tieSortMode", tieMode.getId());
        }

        INSTANCE = null;
    }

    public static StoreViewState get() {
        return INSTANCE;
    }

    public boolean isBatchModeSelected(StoreEntryWrapper entry) {
        return batchModeSelectionSet.contains(entry);
    }

    public void selectBatchMode(StoreSection section) {
        var wrapper = section.getWrapper();
        if (wrapper != null && !batchModeSelectionSet.contains(wrapper)) {
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

    private void initSortMode() {
        String global = AppCache.getNonNull("globalSortMode", String.class, () -> null);
        var globalMode = global != null ? StoreSectionSortMode.fromId(global).orElse(null) : null;
        globalSortMode.setValue(globalMode != null ? globalMode : StoreSectionSortMode.INDEX_ASC);

        String tie = AppCache.getNonNull("tieSortMode", String.class, () -> null);
        var tieMode = global != null ? StoreSectionSortMode.fromId(tie).orElse(null) : null;
        tieSortMode.setValue(tieMode != null ? tieMode : StoreSectionSortMode.DATE_DESC);
    }

    private void updateContent() {
        categories.getList().forEach(c -> c.update());
        allEntries.getList().forEach(e -> e.update());
    }

    private void initSections() {
        try {
            currentTopLevelSection = StoreSection.createTopLevel(
                    allEntries,
                    batchModeSelectionSet,
                    storeEntryWrapper -> true,
                    filter,
                    activeCategory,
                    entriesListVisibilityObservable,
                    entriesListUpdateObservable,
                    new ReadOnlyBooleanWrapper(true));
        } catch (Exception exception) {
            currentTopLevelSection = new StoreSection(
                    null, DerivedObservableList.arrayList(true), DerivedObservableList.arrayList(true), 0);
            ErrorEventFactory.fromThrowable(exception).handle();
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

    private void initBatchListeners() {
        batchModeSelection.getList().addListener((ListChangeListener<? super StoreEntryWrapper>) c -> {
            if (c.getList().isEmpty()) {
                batchModeSelectionSet.clear();
                return;
            }

            while (c.next()) {
                if (c.wasAdded()) {
                    batchModeSelectionSet.addAll(c.getAddedSubList());
                } else if (c.wasRemoved()) {
                    c.getRemoved().forEach(batchModeSelectionSet::remove);
                }
            }
        });

        allEntries.getList().addListener((ListChangeListener<? super StoreEntryWrapper>) c -> {
            batchModeSelection.getList().retainAll(c.getList());
        });

        batchMode.addListener((observable, oldValue, newValue) -> {
            batchModeSelection.getList().clear();
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

        activeCategory.addListener((observable, oldValue, newValue) -> {
            DataStorage.get().setSelectedCategory(newValue.getCategory());
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
                    // Some entries might already be removed again
                    var wrappers = Arrays.stream(entry)
                            .map(StoreEntryWrapper::new)
                            .filter(storeEntryWrapper ->
                                    DataStorage.get().getStoreEntries().contains(storeEntryWrapper.getEntry()))
                            .toList();
                    wrappers.forEach(StoreEntryWrapper::update);

                    // Don't update anything if we have already reset
                    if (INSTANCE == null) {
                        return;
                    }

                    synchronized (this) {
                        allEntries.getList().addAll(wrappers);
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
                    wrappers.forEach(storeEntryWrapper -> storeEntryWrapper.update());
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

                    if (found.get().equals(activeCategory.getValue())) {
                        activeCategory.setValue(found.get().getParent());
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
            public void onEntryCategoryChange() {
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

    public Optional<StoreSection> getParentSectionForWrapper(StoreEntryWrapper wrapper) {
        StoreSection current = getCurrentTopLevelSection();
        while (true) {
            var child = current.getAllChildren().getList().stream()
                    .filter(section -> section.getWrapper().equals(wrapper))
                    .findFirst();
            if (child.isPresent()) {
                return Optional.of(current);
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

    public StoreCategoryWrapper getAllMacrosCategory() {
        return categories.getList().stream()
                .filter(storeCategoryWrapper ->
                        storeCategoryWrapper.getCategory().getUuid().equals(DataStorage.ALL_MACROS_CATEGORY_UUID))
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
