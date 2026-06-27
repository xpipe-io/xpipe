package io.xpipe.app.hub.comp;

import io.xpipe.app.platform.DerivedObservableList;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;

import javafx.collections.ObservableList;
import lombok.Getter;

import java.util.*;
import java.util.function.Predicate;

@Getter
public class StoreSection {

    private final StoreEntryWrapper wrapper;
    private final int depth;

    private final DerivedObservableList<StoreSection> allChildren;
    private final DerivedObservableList<StoreSection> shownChildren;

    private final DerivedObservableList<StoreSection> allChildrenToApply;
    private final DerivedObservableList<StoreSection> shownChildrenToApply;

    public StoreSection(StoreEntryWrapper wrapper, int depth) {
        this.wrapper = wrapper;
        this.depth = depth;
        this.allChildren = DerivedObservableList.arrayList(true);
        this.shownChildren = DerivedObservableList.arrayList(true);
        this.allChildrenToApply = DerivedObservableList.arrayList(true);
        this.shownChildrenToApply = DerivedObservableList.arrayList(true);
    }

    public DataStoreEntry getEntry() {
        return wrapper.getEntry();
    }

    public void apply() {
        // Apply changes to newly added ones before they are added to reduce updates while active
        for (StoreSection child : shownChildrenToApply.getList()) {
            child.apply();
        }

//        var delayedUpdates = new HashSet<StoreSection>();
//        delayedUpdates.addAll(allChildrenToApply.getList());
//        delayedUpdates.addAll(allChildren.getList());
//        shownChildrenToApply.getList().forEach(delayedUpdates::remove);

        allChildren.setContent(allChildrenToApply.getList());
        shownChildren.setContent(shownChildrenToApply.getList());

//        // Apply changes to other ones after they are removed to reduce updates while active
//        for (StoreSection child : delayedUpdates) {
//            child.apply();
//        }
    }

    public void refreshAll(ObservableList<StoreEntryWrapper> all, StoreSectionConfig config, int depth, int orderUpdateIndex) {
        if (wrapper != null) {
            if (wrapper.getEntry().getValidity() == DataStoreEntry.Validity.LOAD_FAILED) {
                allChildrenToApply.setContent(List.of());
                shownChildrenToApply.setContent(List.of());
                return;
            }

            if (!DataStorage.get().getStoreEntries().contains(wrapper.getEntry())) {
                allChildrenToApply.setContent(List.of());
                shownChildrenToApply.setContent(List.of());
                return;
            }
        }

        var applicable = all.stream().filter(other -> {
            return wrapper != null ? config.isChild(this, other) : config.isTop(other);
        }).toList();

        var newAll = applicable.stream().map(wrapper -> {
            var found = allChildren.getList().stream().filter(child -> child.getEntry().equals(wrapper.getEntry())).findFirst();
            var sec = found.isPresent() ? found.get() : new StoreSection(wrapper, depth + 1);
            return sec;
        }).toList();

        allChildrenToApply.setContent(newAll);
        sort(allChildrenToApply.getList(), orderUpdateIndex);

        var withParentConfig = config.withParent(wrapper);
        for (StoreSection child : allChildrenToApply.getList()) {
            child.refreshAll(all, withParentConfig, depth + 1, orderUpdateIndex);
        }
    }

    public void refreshShown(StoreSectionConfig config) {
        if (wrapper != null) {
            if (wrapper.getEntry().getValidity() == DataStoreEntry.Validity.LOAD_FAILED) {
                allChildrenToApply.setContent(List.of());
                shownChildrenToApply.setContent(List.of());
                return;
            }

            if (!DataStorage.get().getStoreEntries().contains(wrapper.getEntry())) {
                allChildrenToApply.setContent(List.of());
                shownChildrenToApply.setContent(List.of());
                return;
            }
        }

        shownChildrenToApply.setContent(allChildrenToApply.getList().stream().filter(other -> {
            return wrapper != null ? config.showChild(other) : config.showTop(other);
        }).toList());

        var withParentConfig = config.withParent(wrapper);
        for (StoreSection child : allChildrenToApply.getList()) {
            child.refreshShown(withParentConfig);
        }
    }

    private void sort(List<StoreSection> list, int orderUpdateIndex) {
        var customComparator =
                wrapper != null && wrapper.getEntry().getProvider() != null ? wrapper.getEntry().getProvider().getComparator() : null;
        var sortMode = StoreViewState.get()
                .createEffectiveSortMode(customComparator, orderUpdateIndex);
        list.sort(
                (o1, o2) -> {
                    var r = sortMode.compare(o1, o2);
                    if (r != 0) {
                        return r;
                    }

                    return sortMode.compare(o1, o2);
                });
    }

    public boolean anyMatches(Predicate<StoreEntryWrapper> c) {
        return c == null
                || (wrapper != null && c.test(wrapper))
                || allChildren.getList().stream().anyMatch(storeEntrySection -> storeEntrySection.anyMatches(c));
    }
}
