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
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;

import lombok.Value;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

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
                        return wrapper.getExpanded().get()
                                || allChildren.getList().isEmpty();
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
            DerivedObservableList<StoreSection> list, ObservableValue<StoreCategoryWrapper> category,
            ObservableIntegerValue updateObservable) {
        if (category == null) {
            return list;
        }

        var explicitOrderComp = Comparator.<StoreSection>comparingInt(new ToIntFunction<>() {
            @Override
            public int applyAsInt(StoreSection value) {
                var explicit = value.getWrapper().getEntry().getExplicitOrder();
                if (explicit == null) {
                    return 0;
                }

                return switch (explicit) {
                    case TOP -> -1;
                    case BOTTOM -> 1;
                };
            }
        });
        var usableComp = Comparator.<StoreSection>comparingInt(
                value -> value.getWrapper().getEntry().getValidity().isUsable() ? -1 : 1);
        var comp = explicitOrderComp.thenComparing(usableComp);
        var mappedSortMode = BindingsHelper.flatMap(
                category,
                storeCategoryWrapper -> storeCategoryWrapper != null ? storeCategoryWrapper.getSortMode() : null);
        return list.sorted(
                (o1, o2) -> {
                    var current = mappedSortMode.getValue();
                    if (current != null) {
                        return comp.thenComparing(current.comparator())
                                .compare(current.representative(o1), current.representative(o2));
                    } else {
                        return comp.compare(o1, o2);
                    }
                },
                mappedSortMode,
                updateObservable);
    }

    public static StoreSection createTopLevel(
            DerivedObservableList<StoreEntryWrapper> all,
            Predicate<StoreEntryWrapper> entryFilter,
            ObservableStringValue filterString,
            ObservableValue<StoreCategoryWrapper> category,
            ObservableIntegerValue updateObservable
            ) {
        var topLevel = all.filtered(
                section -> {
                    return DataStorage.get().isRootEntry(section.getEntry());
                },
                category,
                updateObservable);
        var cached = topLevel.mapped(
                storeEntryWrapper -> create(List.of(), storeEntryWrapper, 1, all, entryFilter, filterString, category, updateObservable));
        var ordered = sorted(cached, category, updateObservable);
        var shown = ordered.filtered(
                section -> {
                    // matches filter
                    return (filterString == null || section.matchesFilter(filterString.get()))
                            &&
                            // matches selector
                            (section.anyMatches(entryFilter))
                            &&
                            // same category
                            (category == null
                                    || category.getValue() == null
                                    || showInCategory(category.getValue(), section.getWrapper()));
                },
                category,
                filterString);
        return new StoreSection(null, ordered, shown, 0);
    }

    private static StoreSection create(
            List<StoreEntryWrapper> parents,
            StoreEntryWrapper e,
            int depth,
            DerivedObservableList<StoreEntryWrapper> all,
            Predicate<StoreEntryWrapper> entryFilter,
            ObservableStringValue filterString,
            ObservableValue<StoreCategoryWrapper> category,
            ObservableIntegerValue updateObservable) {
        if (e.getEntry().getValidity() == DataStoreEntry.Validity.LOAD_FAILED) {
            return new StoreSection(
                    e,
                    new DerivedObservableList<>(FXCollections.observableArrayList(), true),
                    new DerivedObservableList<>(FXCollections.observableArrayList(), true),
                    depth);
        }

        var allChildren = all.filtered(
                other -> {
                    // Legacy implementation that does not use children caches. Use for testing
                    //            if (true) return DataStorage.get()
                    //                    .getDisplayParent(other.getEntry())
                    //                    .map(found -> found.equals(e.getEntry()))
                    //                    .orElse(false);

                    // is children. This check is fast as the children are cached in the storage
                    return DataStorage.get().getStoreChildren(e.getEntry()).contains(other.getEntry())
                            &&
                            // show provider
                            (!other.getEntry().getValidity().isUsable()
                                    || other.getEntry().getProvider().shouldShow(other));
                },
                e.getPersistentState(),
                e.getCache(),
                updateObservable);
        var l = new ArrayList<>(parents);
        l.add(e);
        var cached = allChildren.mapped(c -> create(l, c, depth + 1, all, entryFilter, filterString, category, updateObservable));
        var ordered = sorted(cached, category, updateObservable);
        var filtered = ordered.filtered(
                section -> {
                    // matches filter
                    return (filterString == null
                                    || section.matchesFilter(filterString.get())
                                    || l.stream().anyMatch(p -> p.matchesFilter(filterString.get())))
                            &&
                            // matches selector
                            section.anyMatches(entryFilter)
                            &&
                            // matches category
                            // Prevent updates for children on category switching by checking depth
                            (category == null
                                    || category.getValue() == null
                                    || showInCategory(category.getValue(), section.getWrapper())
                                    || depth > 0)
                            &&
                            // not root
                            // If this entry is already shown as root due to a different category than parent, don't
                            // show it
                            // again here
                            !DataStorage.get().isRootEntry(section.getWrapper().getEntry());
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
        return anyMatches(storeEntryWrapper -> storeEntryWrapper.matchesFilter(filter));
    }

    public boolean anyMatches(Predicate<StoreEntryWrapper> c) {
        return c == null
                || c.test(wrapper)
                || allChildren.getList().stream().anyMatch(storeEntrySection -> storeEntrySection.anyMatches(c));
    }
}
