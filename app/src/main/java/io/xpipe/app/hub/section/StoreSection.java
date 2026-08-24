package io.xpipe.app.hub.section;

import io.xpipe.app.hub.entry.StoreEntryWrapper;
import io.xpipe.app.hub.list.StoreViewState;
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

    public void apply(boolean alwaysUpdateAll) {
        // Apply changes to newly added ones before they are added to reduce updates while active
        for (StoreSection child : shownChildrenToApply.getList()) {
            child.apply(alwaysUpdateAll);
        }

        //        var delayedUpdates = new HashSet<StoreSection>();
        //        delayedUpdates.addAll(allChildrenToApply.getList());
        //        delayedUpdates.addAll(allChildren.getList());
        //        shownChildrenToApply.getList().forEach(delayedUpdates::remove);

        var allChanged = allChildren.setContent(allChildrenToApply.getList());
        var shownChanged = shownChildren.setContent(shownChildrenToApply.getList());

        if (alwaysUpdateAll || allChanged || shownChanged) {
            for (StoreSection s : shownChildren.getList()) {
                s.getWrapper().update();
            }
        }

        //        // Apply changes to other ones after they are removed to reduce updates while active
        //        for (StoreSection child : delayedUpdates) {
        //            child.apply();
        //        }
    }

    public void refreshAll(
            ObservableList<StoreEntryWrapper> all, StoreSectionConfig config, int depth, int orderUpdateIndex) {
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

        var withParentConfig = config.withParent(wrapper);
        var applicable = all.stream()
                .filter(other -> {
                    return wrapper != null ? withParentConfig.isChild(this, other) : withParentConfig.isTop(other);
                })
                .toList();

        var newAll = applicable.stream()
                .map(wrapper -> {
                    var found = allChildren.getList().stream()
                            .filter(child -> child.getEntry().equals(wrapper.getEntry()))
                            .findFirst();
                    var sec = found.isPresent() ? found.get() : new StoreSection(wrapper, depth + 1);
                    return sec;
                })
                .toList();

        allChildrenToApply.setContent(newAll);
        sort(allChildrenToApply.getList(), orderUpdateIndex);

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

        var withParentConfig = config.withParent(wrapper);
        shownChildrenToApply.setContent(allChildrenToApply.getList().stream()
                .filter(other -> {
                    return wrapper != null ? withParentConfig.showChild(other) : withParentConfig.showTop(other);
                })
                .toList());

        for (StoreSection child : allChildrenToApply.getList()) {
            child.refreshShown(withParentConfig);
        }
    }

    private void sort(List<StoreSection> list, int orderUpdateIndex) {
        var customComparator = wrapper != null && wrapper.getEntry().getProvider() != null
                ? wrapper.getEntry().getProvider().getComparator()
                : null;
        var sortMode = StoreViewState.get().createEffectiveSortMode(customComparator, orderUpdateIndex);
        list.sort((o1, o2) -> {
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

    public void insertSections(List<StoreEntryWrapper> selection, int index, boolean after, boolean topLevel) {
        List<StoreSection> l = shownChildren.getList();
        if (l.isEmpty()) {
            for (StoreEntryWrapper section : selection) {
                DataStorage.get().setPinToTop(section.getEntry(), false);
            }
            return;
        }

        // Insert at top or bottom
        var top = index == 0 && !after;
        var bottom = index == l.size() - 1 && after;
        if (top || bottom) {
            var ref = top ? l.getFirst() : l.getLast();
            var orderIndex = ref.getWrapper().getOrderIndex().get();
            if (StoreSectionSortMode.INDEX_DESC.equals(
                            StoreViewState.get().getSortMode().getValue())
                    ^ bottom) {
                var off = Math.min(0.5, Math.ceil(orderIndex) - orderIndex);
                if (off == 0.0) {
                    off = 0.5;
                }
                orderSelection(selection, orderIndex, off, topLevel);
            } else {
                var off = Math.max(-0.5, Math.floor(orderIndex) - orderIndex);
                if (off == 0.0) {
                    off = -0.5;
                }
                orderSelection(selection, orderIndex, off, topLevel);
            }
            return;
        }

        var min = l.get(index - (after ? 0 : 1)).getWrapper().getOrderIndex().get();
        var max = l.get(index + (after ? 1 : 0)).getWrapper().getOrderIndex().get();
        var off = Math.clamp(max - min, -0.5, 0.5);
        orderSelection(selection, min, off, topLevel);
    }

    private void orderSelection(List<StoreEntryWrapper> selection, double start, double off, boolean topLevel) {
        var inc = off / (selection.size() + 1);
        for (int i = 0; i < selection.size(); i++) {
            var w = selection.get(i);
            w.orderWithIndex(start + (i + 1) * inc);

            var isChildInSelection = selection.stream()
                    .anyMatch(other ->
                            StoreViewState.get()
                                    .getSectionForWrapper(other)
                                    .orElseThrow()
                                    .getAllChildren()
                                    .getList()
                                    .stream()
                                    .anyMatch(child -> child.getWrapper().equals(w)));
            if (!isChildInSelection) {
                DataStorage.get().setPinToTop(w.getEntry(), topLevel);
            }
        }
    }
}
