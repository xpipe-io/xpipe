package io.xpipe.app.hub.comp;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import lombok.Value;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

@Value
public class StoreSectionConfig {

    Set<StoreEntryWrapper> parents;
    Predicate<StoreEntryWrapper> entryFilter;
    StoreFilter filter;
    StoreCategoryWrapper category;
    Set<StoreEntryWrapper> selected;

    public boolean isTop(StoreEntryWrapper wrapper) {
        if (wrapper.getEntry().getValidity() == DataStoreEntry.Validity.LOAD_FAILED) {
            return true;
        }

        return DataStorage.get().isRootEntry(wrapper.getEntry(), category.getCategory());
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
        var isBatchSelected = selected.contains(section.getWrapper());

        var matchesFilter = filter == null
                || section.matchesFilter(filter)
                || parents.stream().anyMatch(p -> p.matchesFilter(filter));
        if (!isBatchSelected && !matchesFilter) {
            return false;
        }

        var hasFilter = filter != null;
        if (section.getEntry().getProvider() != null && !isBatchSelected && !hasFilter) {
            var showProvider = true;
            try {
                showProvider = section.getEntry().getProvider().shouldShow(section.getWrapper());
            } catch (Exception ignored) {
            }
            if (!showProvider) {
                return false;
            }
        }

        var matchesSelector = section.anyMatches(entryFilter);
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
        }

        return true;
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

            if (!AppPrefs.get().showChildCategoriesInParentCategory().get()) {
                break;
            }

            current = current.getParent();
        }
        return false;
    }

    public StoreSectionConfig withParent(StoreEntryWrapper parent) {
        var l = new HashSet<>(parents);
        l.add(parent);
        return new StoreSectionConfig(l, entryFilter, filter, category, selected);
    }
}
