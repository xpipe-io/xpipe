package io.xpipe.app.comp.storage.store;

import io.xpipe.app.comp.storage.StorageFilter;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Value;

import java.time.Instant;
import java.util.Comparator;

@Value
public class StoreViewSection implements StorageFilter.Filterable {

    StoreEntryWrapper wrapper;
    ObservableList<StoreViewSection> children;

    private static final Comparator<StoreViewSection> COMPARATOR = Comparator.<StoreViewSection, Instant>comparing(
                    o -> o.wrapper.getEntry().getState().equals(DataStoreEntry.State.COMPLETE_AND_VALID)
                            ? o.wrapper.getEntry().getLastAccess()
                            : Instant.EPOCH).reversed()
            .thenComparing(
                    storeEntrySection -> storeEntrySection.wrapper.getEntry().getName());

    public static ObservableList<StoreViewSection> createTopLevels() {
        var filtered =
                BindingsHelper.filteredContentBinding(StoreViewState.get().getAllEntries(), storeEntryWrapper -> {
                    if (!storeEntryWrapper.getEntry().getState().isUsable()) {
                        return true;
                    }

                    var parent = storeEntryWrapper
                            .getEntry()
                            .getProvider()
                            .getParent(storeEntryWrapper.getEntry().getStore());
                    return parent == null
                            || (DataStorage.get().getStoreEntryIfPresent(parent).isEmpty());
                });
        var topLevel = BindingsHelper.mappedContentBinding(filtered, storeEntryWrapper -> create(storeEntryWrapper));
        var ordered = BindingsHelper.orderedContentBinding(
                topLevel,
                COMPARATOR);
        return ordered;
    }

    public static StoreViewSection create(StoreEntryWrapper e) {
        if (!e.getEntry().getState().isUsable()) {
            return new StoreViewSection(e, FXCollections.observableArrayList());
        }

        var filtered = BindingsHelper.filteredContentBinding(
                StoreViewState.get().getAllEntries(),
                other -> other.getEntry().getState().isUsable()
                        && e.getEntry()
                        .getStore()
                        .equals(other.getEntry()
                                        .getProvider()
                                        .getParent(other.getEntry().getStore())));
        var children = BindingsHelper.mappedContentBinding(filtered, entry1 -> create(entry1));
        var ordered = BindingsHelper.orderedContentBinding(
                children,
                COMPARATOR);
        return new StoreViewSection(e, ordered);
    }

    @Override
    public boolean shouldShow(String filter) {
        return wrapper.shouldShow(filter)
                || children.stream().anyMatch(storeEntrySection -> storeEntrySection.shouldShow(filter));
    }
}
