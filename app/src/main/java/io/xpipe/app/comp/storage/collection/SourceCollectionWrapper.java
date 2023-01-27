package io.xpipe.app.comp.storage.collection;

import io.xpipe.app.comp.source.GuiDsCreatorMultiStep;
import io.xpipe.app.comp.storage.StorageFilter;
import io.xpipe.app.comp.storage.source.SourceEntryDisplayMode;
import io.xpipe.app.comp.storage.source.SourceEntryWrapper;
import io.xpipe.app.storage.CollectionListener;
import io.xpipe.app.storage.DataSourceCollection;
import io.xpipe.app.storage.DataSourceEntry;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.impl.FileStore;
import io.xpipe.extension.DataSourceProvider;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SourceCollectionWrapper implements StorageFilter.Filterable {

    private final Property<String> name;
    private final IntegerProperty size;
    private final ListProperty<SourceEntryWrapper> entries;
    private final DataSourceCollection collection;
    private final Property<Instant> lastAccess;
    private final Property<SourceCollectionSortMode> sortMode =
            new SimpleObjectProperty<>(SourceCollectionSortMode.DATE_DESC);
    private final Property<SourceEntryDisplayMode> displayMode =
            new SimpleObjectProperty<>(SourceEntryDisplayMode.LIST);

    public SourceCollectionWrapper(DataSourceCollection collection) {
        this.collection = collection;
        this.entries =
                new SimpleListProperty<SourceEntryWrapper>(FXCollections.observableList(collection.getEntries().stream()
                        .map(SourceEntryWrapper::new)
                        .collect(Collectors.toCollection(ArrayList::new))));
        this.size = new SimpleIntegerProperty(collection.getEntries().size());
        this.name = new SimpleStringProperty(collection.getName());
        this.lastAccess = new SimpleObjectProperty<>(collection.getLastAccess().minus(Duration.ofMillis(500)));

        setupListeners();
    }

    public ReadOnlyBooleanProperty emptyProperty() {
        return entries.emptyProperty();
    }

    public boolean isDeleteable() {
        return !isInternal();
    }

    public boolean isRenameable() {
        return !isInternal();
    }

    public boolean isInternal() {
        return collection.equals(DataStorage.get().getInternalCollection());
    }

    public void dropFile(Path file) {
        var store = FileStore.local(file);
        GuiDsCreatorMultiStep.showForStore(DataSourceProvider.Category.STREAM, store, this.getCollection());
    }

    public void delete() {
        DataStorage.get().deleteCollection(this.collection);
    }

    public void clean() {
        var entries = List.copyOf(collection.getEntries());
        entries.forEach(e -> DataStorage.get().deleteEntry(e));
    }

    private void setupListeners() {
        name.addListener((c, o, n) -> {
            collection.setName(n);
        });

        collection.addListener(new CollectionListener() {
            @Override
            public void onUpdate() {
                lastAccess.setValue(collection.getLastAccess().minus(Duration.ofMillis(500)));
                name.setValue(collection.getName());
            }

            @Override
            public void onEntryAdd(DataSourceEntry entry) {
                var e = new SourceEntryWrapper(entry);
                entries.add(e);
            }

            @Override
            public void onEntryRemove(DataSourceEntry entry) {
                entries.removeIf(e -> e.getEntry().equals(entry));
            }
        });
    }

    public DataSourceCollection getCollection() {
        return collection;
    }

    public String getName() {
        return name.getValue();
    }

    public Property<String> nameProperty() {
        return name;
    }

    public int getSize() {
        return size.get();
    }

    public IntegerProperty sizeProperty() {
        return size;
    }

    public ObservableList<SourceEntryWrapper> getEntries() {
        return entries.get();
    }

    public ListProperty<SourceEntryWrapper> entriesProperty() {
        return entries;
    }

    @Override
    public boolean shouldShow(String filter) {
        if (isInternal()) {
            // return getEntries().stream().anyMatch(e -> e.shouldShow(filter));
        }

        return getName().toLowerCase().contains(filter.toLowerCase())
                || entries.stream().anyMatch(e -> e.shouldShow(filter));
    }

    public Instant getLastAccess() {
        return lastAccess.getValue();
    }

    public Property<Instant> lastAccessProperty() {
        return lastAccess;
    }

    public SourceCollectionSortMode getSortMode() {
        return sortMode.getValue();
    }

    public Property<SourceCollectionSortMode> sortModeProperty() {
        return sortMode;
    }

    public SourceEntryDisplayMode getDisplayMode() {
        return displayMode.getValue();
    }

    public Property<SourceEntryDisplayMode> displayModeProperty() {
        return displayMode;
    }
}
