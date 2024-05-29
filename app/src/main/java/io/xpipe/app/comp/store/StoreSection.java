package io.xpipe.app.comp.store;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.fxcomps.util.DerivedObservableList;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import lombok.Value;

import java.util.Comparator;
import java.util.function.Predicate;

@Value
public class StoreSection {

    StoreEntryWrapper wrapper;
    DerivedObservableList<StoreSection> allChildren;
    DerivedObservableList<StoreSection> shownChildren;
    int depth;
    ObservableBooleanValue showDetails;

    public StoreSection(
            StoreEntryWrapper wrapper,
            DerivedObservableList<StoreSection> allChildren,
            DerivedObservableList<StoreSection> shownChildren,
            int depth) {
        this.wrapper = wrapper;
        this.allChildren = allChildren;
        this.shownChildren = shownChildren;
        this.depth = depth;
        if (wrapper != null) {
            this.showDetails = Bindings.createBooleanBinding(
                    () -> {
                        return wrapper.getExpanded().get() || allChildren.getList().isEmpty();
                    },
                    wrapper.getExpanded(),
                    allChildren.getList());
        } else {
            this.showDetails = new SimpleBooleanProperty(true);
        }
    }

    public static Comp<?> customSection(StoreSection e, boolean topLevel) {
        var prov = e.getWrapper().getEntry().getProvider();
        if (prov != null) {
            return prov.customSectionComp(e, topLevel);
        } else {
            return new StoreSectionComp(e, topLevel);
        }
    }

    private static DerivedObservableList<StoreSection> sorted(
            DerivedObservableList<StoreSection> list, ObservableValue<StoreCategoryWrapper> category) {
        if (category == null) {
            return list;
        }

        var explicitOrderComp = new Comparator<StoreSection>() {
            @Override
            public int compare(StoreSection o1, StoreSection o2) {
                var explicit1 = o1.getWrapper().getEntry().getOrderBefore();
                var explicit2 = o2.getWrapper().getEntry().getOrderBefore();
                if (explicit1 == null && explicit2 == null) {
                    return 0;
                }
                if (explicit1 != null && explicit2 == null) {
                    return -1;
                }
                if (explicit2 != null && explicit1 == null) {
                    return 1;
                }
                if (explicit1.equals(o2.getWrapper().getEntry().getUuid())) {
                    return -1;
                }
                if (explicit2.equals(o1.getWrapper().getEntry().getUuid())) {
                    return -1;
                }
                return 0;
            }
        };
        var usableComp = Comparator.<StoreSection>comparingInt(
                value -> value.getWrapper().getEntry().getValidity().isUsable() ? -1 : 1);
        var comp = explicitOrderComp.thenComparing(usableComp);
        var mappedSortMode = BindingsHelper.flatMap(
                category,
                storeCategoryWrapper -> storeCategoryWrapper != null ? storeCategoryWrapper.getSortMode() : null);
        return list.sorted((o1, o2) -> {
                    var current = mappedSortMode.getValue();
                    if (current != null) {
                        return comp.thenComparing(current.comparator())
                                .compare(current.representative(o1), current.representative(o2));
                    } else {
                        return comp.compare(o1, o2);
                    }
                },
                mappedSortMode);
    }

    public static StoreSection createTopLevel(
            DerivedObservableList<StoreEntryWrapper> all,
            Predicate<StoreEntryWrapper> entryFilter,
            ObservableStringValue filterString,
            ObservableValue<StoreCategoryWrapper> category) {
        var topLevel = all.filtered(section -> {
                    return DataStorage.get().isRootEntry(section.getEntry());
                },
                category);
        var cached = topLevel.mapped(
                storeEntryWrapper -> create(storeEntryWrapper, 1, all, entryFilter, filterString, category));
        var ordered = sorted(cached, category);
        var shown = ordered.filtered(
                section -> {
                    var showFilter = filterString == null || section.matchesFilter(filterString.get());
                    var matchesSelector = section.anyMatches(entryFilter);
                    var sameCategory = category == null
                            || category.getValue() == null
                            || showInCategory(category.getValue(), section.getWrapper());
                    return showFilter && matchesSelector && sameCategory;
                },
                category,
                filterString);
        return new StoreSection(null, ordered, shown, 0);
    }

    private static StoreSection create(
            StoreEntryWrapper e,
            int depth,
            DerivedObservableList<StoreEntryWrapper> all,
            Predicate<StoreEntryWrapper> entryFilter,
            ObservableStringValue filterString,
            ObservableValue<StoreCategoryWrapper> category) {
        if (e.getEntry().getValidity() == DataStoreEntry.Validity.LOAD_FAILED) {
            return new StoreSection(e, new DerivedObservableList<>(
                    FXCollections.observableArrayList(), true), new DerivedObservableList<>(
                            FXCollections.observableArrayList(), true), depth);
        }

        var allChildren = all.filtered(other -> {
            // Legacy implementation that does not use children caches. Use for testing
            //            if (true) return DataStorage.get()
            //                    .getDisplayParent(other.getEntry())
            //                    .map(found -> found.equals(e.getEntry()))
            //                    .orElse(false);

            // This check is fast as the children are cached in the storage
            var isChildren = DataStorage.get().getStoreChildren(e.getEntry()).contains(other.getEntry());
            var showProvider = other.getEntry().getProvider() == null ||
                    other.getEntry().getProvider().shouldShow(other);
            return isChildren && showProvider;
        }, e.getPersistentState(), e.getCache());
        var cached = allChildren.mapped(
                entry1 -> create(entry1, depth + 1, all, entryFilter, filterString, category));
        var ordered = sorted(cached, category);
        var filtered = ordered.filtered(
                section -> {
                    var showFilter = filterString == null || section.matchesFilter(filterString.get());
                    var matchesSelector = section.anyMatches(entryFilter);
                    // Prevent updates for children on category switching by checking depth
                    var showCategory = category == null
                            || category.getValue() == null
                            || showInCategory(category.getValue(), section.getWrapper())
                            || depth > 0;
                    // If this entry is already shown as root due to a different category than parent, don't show it
                    // again here
                    var notRoot =
                            !DataStorage.get().isRootEntry(section.getWrapper().getEntry());
                    var showProvider = section.getWrapper().getEntry().getProvider() == null ||
                            section.getWrapper().getEntry().getProvider().shouldShow(section.getWrapper());
                    return showFilter && matchesSelector && showCategory && notRoot && showProvider;
                },
                category,
                filterString,
                e.getPersistentState(),
                e.getCache());
        return new StoreSection(e, cached, filtered, depth);
    }

    private static boolean showInCategory(StoreCategoryWrapper categoryWrapper, StoreEntryWrapper entryWrapper) {
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

    public boolean matchesFilter(String filter) {
        return anyMatches(storeEntryWrapper -> storeEntryWrapper.shouldShow(filter));
    }

    public boolean anyMatches(Predicate<StoreEntryWrapper> c) {
        return c == null
                || c.test(wrapper)
                || allChildren.getList().stream().anyMatch(storeEntrySection -> storeEntrySection.anyMatches(c));
    }
}
