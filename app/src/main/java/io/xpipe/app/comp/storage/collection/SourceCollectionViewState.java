package io.xpipe.app.comp.storage.collection;

import io.xpipe.app.comp.storage.StorageFilter;
import io.xpipe.app.comp.storage.source.SourceEntryWrapper;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataSourceCollection;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.StorageListener;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.util.BindingsHelper;
import io.xpipe.extension.fxcomps.util.PlatformThread;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.time.Instant;
import java.util.Comparator;
import java.util.concurrent.CopyOnWriteArrayList;

public class SourceCollectionViewState {

    private static SourceCollectionViewState INSTANCE;

    private final StorageFilter filter = new StorageFilter();

    private final ObservableList<SourceCollectionWrapper> allGroups =
            FXCollections.observableList(new CopyOnWriteArrayList<>());
    private final ObservableList<SourceCollectionWrapper> shownGroups =
            FXCollections.observableList(new CopyOnWriteArrayList<>());
    private final SimpleObjectProperty<SourceCollectionWrapper> selectedGroup = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<SourceCollectionSortMode> sortMode = new SimpleObjectProperty<>();

    private final ObservableList<SourceEntryWrapper> allEntries =
            FXCollections.observableList(new CopyOnWriteArrayList<>());
    private final ObservableList<SourceEntryWrapper> shownEntries =
            FXCollections.observableList(new CopyOnWriteArrayList<>());

    private final ObservableBooleanValue storageEmpty =
            BindingsHelper.persist(Bindings.size(allGroups).isEqualTo(0));

    private SourceCollectionViewState() {
        addCollectionListChangeListeners();
        addEntryListListeners();
        addSortModeListeners();
    }

    public static void init() {
        INSTANCE = new SourceCollectionViewState();
    }

    public static void reset() {
        INSTANCE = null;
    }

    public static SourceCollectionViewState get() {
        return INSTANCE;
    }

    private void addSortModeListeners() {
        ChangeListener<SourceCollectionSortMode> listener = (observable1, oldValue1, newValue1) -> {
            sortMode.set(newValue1);
        };

        selectedGroup.addListener((observable, oldValue, newValue) -> {
            sortMode.set(newValue != null ? newValue.getSortMode() : null);
            if (newValue != null) {
                newValue.sortModeProperty().addListener(listener);
            }

            if (oldValue != null) {
                oldValue.sortModeProperty().removeListener(listener);
            }
        });
    }

    public void addNewCollection() {
        PlatformThread.runLaterIfNeeded(() -> {
            var col = DataSourceCollection.createNew(I18n.get("newCollection"));
            DataStorage.get().addCollection(col);
            allGroups.stream()
                    .filter(g -> g.getCollection().equals(col))
                    .findAny()
                    .ifPresent(selectedGroup::set);
        });
    }

    public ObservableList<SourceEntryWrapper> getAllEntries() {
        return allEntries;
    }

    public ObservableList<SourceEntryWrapper> getShownEntries() {
        return shownEntries;
    }

    public ObservableBooleanValue getStorageEmpty() {
        return storageEmpty;
    }

    public SourceCollectionWrapper getSelectedGroup() {
        return selectedGroup.get();
    }

    public SimpleObjectProperty<SourceCollectionWrapper> selectedGroupProperty() {
        return selectedGroup;
    }

    private void addCollectionListChangeListeners() {
        allGroups.setAll(filter(FXCollections.observableList(DataStorage.get().getSourceCollections().stream()
                .map(SourceCollectionWrapper::new)
                .toList())));
        filter.createFilterBinding(
                filter(allGroups),
                shownGroups,
                new SimpleObjectProperty<>(
                        Comparator.<SourceCollectionWrapper, Instant>comparing(e -> e.getLastAccess())
                                .reversed()));

        DataStorage.get().addListener(new StorageListener() {
            @Override
            public void onStoreAdd(DataStoreEntry entry) {}

            @Override
            public void onStoreRemove(DataStoreEntry entry) {}

            @Override
            public void onCollectionAdd(DataSourceCollection collection) {
                PlatformThread.runLaterIfNeeded(() -> {
                    var sg = new SourceCollectionWrapper(collection);
                    allGroups.add(sg);
                });
            }

            @Override
            public void onCollectionRemove(DataSourceCollection collection) {
                PlatformThread.runLaterIfNeeded(() -> {
                    allGroups.removeIf(g -> g.getCollection().equals(collection));
                });
            }
        });

        shownGroups.addListener((ListChangeListener<? super SourceCollectionWrapper>) (c) -> {
            if (selectedGroup.get() != null && !shownGroups.contains(selectedGroup.get())) {
                selectedGroup.set(null);
            }
        });

        shownGroups.addListener((ListChangeListener<? super SourceCollectionWrapper>) c -> {
            if (c.getList().size() == 1) {
                selectedGroup.set(c.getList().get(0));
            }
        });
    }

    private ObservableList<SourceCollectionWrapper> filter(ObservableList<SourceCollectionWrapper> list) {
        return list.filtered(storeEntryWrapper -> {
            if (AppPrefs.get().developerMode().getValue() && AppPrefs.get().developerShowHiddenEntries().get()) {
                return true;
            } else {
                return !storeEntryWrapper.isInternal();
            }
        });
    }

    public SourceCollectionWrapper getGroup(SourceEntryWrapper e) {
        return allGroups.stream()
                .filter(g -> g.getEntries().contains(e))
                .findFirst()
                .orElseThrow();
    }

    public ObservableList<SourceEntryWrapper> getFilteredEntries(SourceCollectionWrapper g) {
        var filtered = FXCollections.<SourceEntryWrapper>observableArrayList();
        filter.createFilterBinding(
                g.entriesProperty(),
                filtered,
                new SimpleObjectProperty<>(Comparator.<SourceEntryWrapper, Instant>comparing(
                                e -> e.getEntry().getLastAccess())
                        .reversed()));
        return filtered;
    }

    private void addEntryListListeners() {
        filter.createFilterBinding(
                allEntries,
                shownEntries,
                Bindings.createObjectBinding(
                        () -> {
                            return sortMode.getValue() != null
                                    ? sortMode.getValue().comparator()
                                    : Comparator.<SourceEntryWrapper>comparingInt(o -> o.hashCode());
                        },
                        sortMode));

        selectedGroup.addListener((c, o, n) -> {
            if (o != null) {
                Bindings.unbindContent(allEntries, o.getEntries());
            }

            if (n != null) {
                Bindings.bindContent(allEntries, n.getEntries());
            }
        });
    }

    public ObservableList<SourceCollectionWrapper> getShownGroups() {
        return shownGroups;
    }

    public ObservableList<SourceCollectionWrapper> getAllGroups() {
        return allGroups;
    }

    public StorageFilter getFilter() {
        return filter;
    }
}
