package io.xpipe.app.hub.section;

import io.xpipe.app.hub.category.StoreCategoryWrapper;
import io.xpipe.app.hub.entry.StoreEntryWrapper;
import io.xpipe.app.hub.list.StoreFilter;
import io.xpipe.app.hub.list.StoreSectionDrag;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;

import lombok.Value;

import java.util.HashSet;
import java.util.Set;

@Value
public class StoreSectionConfig {

    Set<StoreEntryWrapper> parents;
    StoreSectionSelector selector;
    StoreFilter filter;
    StoreCategoryWrapper category;
    Set<StoreEntryWrapper> selected;
    StoreSectionDrag dragOperation;
    Set<StoreEntryWrapper> added;

    public boolean isTop(StoreEntryWrapper wrapper) {
        if (wrapper.getEntry().getValidity() == DataStoreEntry.Validity.LOAD_FAILED) {
            return true;
        }

        return DataStorage.get().isPotentialRootEntry(wrapper.getEntry());
    }

    public boolean isChild(StoreSection section, StoreEntryWrapper other) {
        if (section.getEntry().getValidity() == DataStoreEntry.Validity.LOAD_FAILED) {
            return false;
        }

        // Legacy implementation that does not use children caches. Use for testing
        //                                if (true) return DataStorage.get()
        //                                        .getDefaultDisplayParent(other.getEntry())
        //                                        .map(found -> found.equals(e.getEntry()))
        //                                        .orElse(false);

        // This check is fast as the children are cached in the storage
        if (!DataStorage.get().getStoreChildren(section.getEntry()).contains(other.getEntry())) {
            return false;
        }

        // If this entry is already shown as root due to a different category than parent, don't
        // show it
        // again here
        var root = DataStorage.get().isRootEntry(other.getEntry(), category.getCategory());
        if (root) {
            return false;
        }

        return true;
    }

    public boolean showTop(StoreSection section) {
        return show(section, true);
    }

    public boolean showChild(StoreSection section) {
        return show(section, false);
    }

    private boolean show(StoreSection section, boolean topLevel) {
        if (dragOperation != null) {
            if (dragOperation.getSelection().contains(section.getWrapper())) {
                return false;
            }
        }

        var isBatchSelected = selected.contains(section.getWrapper());
        var wasAddedAfterFilter = added.contains(section.getWrapper());

        var matchesFilterThis = filter != null && matchesFilter(section, filter);
        var matchesFilterParents = filter != null && parents.stream().anyMatch(p -> p.matchesFilter(filter));
        var matchesFilter = filter == null || matchesFilterThis || matchesFilterParents;
        if (!isBatchSelected && !matchesFilter && !wasAddedAfterFilter) {
            return false;
        }

        var matchesSelector = matchesSelector(section, selector);

        if (parents.size() > 0
                && section.getEntry().getProvider() != null
                && !isBatchSelected
                && (filter == null || matchesFilterParents)
                && (selector.excludeNonShown() || !matchesSelector)) {
            var showProvider = true;
            try {
                showProvider = section.getEntry().getProvider().shouldShow(section.getWrapper());
            } catch (Exception ignored) {
            }
            if (!showProvider) {
                return false;
            }
        }

        if (!isBatchSelected && !matchesSelector) {
            return false;
        }

        var showCategory = showInCategory(category, section.getWrapper());
        if (!showCategory) {
            return false;
        }

        if (!topLevel) {
            // If this entry is already shown as root due to a different category than parent, don't
            // show it
            // again here
            var root = DataStorage.get().isRootEntry(section.getEntry(), category.getCategory());
            if (root) {
                return false;
            }
        } else {
            var root = DataStorage.get().isRootEntry(section.getEntry(), category.getCategory());
            if (!root) {
                return false;
            }
        }

        return true;
    }

    private boolean matchesFilter(StoreSection section, StoreFilter filter) {
        return filter == null
                || (section.getWrapper() != null && section.getWrapper().matchesFilter(filter))
                || section.getAllChildrenToApply().getList().stream().anyMatch(c -> matchesFilter(c, filter));
    }

    private boolean matchesSelector(StoreSection section, StoreSectionSelector selector) {
        return selector == null
                || (section.getWrapper() != null && selector.matches(section.getWrapper()))
                || section.getAllChildrenToApply().getList().stream().anyMatch(c -> matchesSelector(c, selector));
    }

    private boolean showInCategory(StoreCategoryWrapper categoryWrapper, StoreEntryWrapper entryWrapper) {
        var current = entryWrapper.getCategory().getValue();
        while (current != null) {
            if (categoryWrapper
                    .getCategory()
                    .getUuid()
                    .equals(current.getCategory().getUuid())) {
                return true;
            }

            // Show everything in top level category
            if (categoryWrapper.getParent() != null
                    && !AppPrefs.get().showChildCategoriesInParentCategory().get()) {
                break;
            }

            current = current.getParent();
        }
        return false;
    }

    public StoreSectionConfig withParent(StoreEntryWrapper parent) {
        var l = new HashSet<>(parents);
        if (parent != null) {
            l.add(parent);
        }
        return new StoreSectionConfig(l, selector, filter, category, selected, dragOperation, added);
    }
}
