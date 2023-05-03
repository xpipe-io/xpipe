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
public class StoreSection implements StorageFilter.Filterable {

    StoreEntryWrapper wrapper;
    ObservableList<StoreSection> children;

    private static final Comparator<StoreSection> COMPARATOR = Comparator.<StoreSection, Instant>comparing(
                    o -> o.wrapper.getEntry().getState().equals(DataStoreEntry.State.COMPLETE_AND_VALID)
                            ? o.wrapper.getEntry().getLastAccess()
                            : Instant.EPOCH).reversed()
            .thenComparing(
                    storeEntrySection -> storeEntrySection.wrapper.getEntry().getName());

    public static StoreSection createTopLevel() {
        var topLevel = BindingsHelper.mappedContentBinding(StoreViewState.get().getAllEntries(), storeEntryWrapper -> create(storeEntryWrapper));
        var filtered =
                BindingsHelper.filteredContentBinding(topLevel, section -> {
                    if (!section.getWrapper().getEntry().getState().isUsable()) {
                        return true;
                    }

                    var parent = section.getWrapper()
                            .getEntry()
                            .getProvider()
                            .getParent(section.getWrapper().getEntry().getStore());
                    return parent == null
                            || (DataStorage.get().getStoreEntryIfPresent(parent).isEmpty());
                });
        var ordered = BindingsHelper.orderedContentBinding(
                filtered,
                COMPARATOR);
        return new StoreSection(null, ordered);
    }

    private static StoreSection create(StoreEntryWrapper e) {
        if (!e.getEntry().getState().isUsable()) {
            return new StoreSection(e, FXCollections.observableArrayList());
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
        return new StoreSection(e, ordered);
    }

    @Override
    public boolean shouldShow(String filter) {
        return wrapper.shouldShow(filter)
                || children.stream().anyMatch(storeEntrySection -> storeEntrySection.shouldShow(filter));
    }
}
