package io.xpipe.app.comp.storage.store;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import lombok.Value;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

@Value
public class StoreSection {

    public static Comp<?> customSection(StoreSection e, boolean topLevel) {
        var prov = e.getWrapper().getEntry().getProvider();
        if (prov != null) {
            return prov.customSectionComp(e, topLevel);
        } else {
            return new StoreSectionComp(e, topLevel);
        }
    }

    StoreEntryWrapper wrapper;
    ObservableList<StoreSection> allChildren;
    ObservableList<StoreSection> shownChildren;
    ObservableList<StoreEntryWrapper> shownEntries;
    int depth;
    ObservableBooleanValue showDetails;

    public StoreSection(
            StoreEntryWrapper wrapper,
            ObservableList<StoreSection> allChildren,
            ObservableList<StoreSection> shownChildren,
            int depth) {
        this.wrapper = wrapper;
        this.allChildren = allChildren;
        this.shownChildren = shownChildren;
        this.depth = depth;
        if (wrapper != null) {
            this.showDetails = Bindings.createBooleanBinding(
                    () -> {
                        return wrapper.getExpanded().get() || allChildren.size() == 0;
                    },
                    wrapper.getExpanded(),
                    allChildren);
        } else {
            this.showDetails = new SimpleBooleanProperty(true);
        }

        this.shownEntries = FXCollections.observableArrayList();
        this.shownChildren.addListener((ListChangeListener<? super StoreSection>) c -> {
            shownEntries.clear();
            addShown(shownEntries);
        });
    }

    private void addShown(List<StoreEntryWrapper> list) {
        getShownChildren().forEach(shown -> {
            list.add(shown.getWrapper());
            shown.addShown(list);
        });
    }

    private static ObservableList<StoreSection> sorted(
            ObservableList<StoreSection> list, ObservableValue<StoreCategoryWrapper> category) {
        var c = Comparator.<StoreSection>comparingInt(
                value -> value.getWrapper().getEntry().getState().isUsable() ? 1 : -1);
        category.getValue().getSortMode().addListener((observable, oldValue, newValue) -> {
            int a = 0;
        });
        var mapped = BindingsHelper.mappedBinding(category, storeCategoryWrapper -> storeCategoryWrapper.getSortMode());
        mapped.addListener((observable, oldValue, newValue) -> {
            int a = 0;
        });
        return BindingsHelper.orderedContentBinding(
                list,
                (o1, o2) -> {
                    var current = category.getValue();
                    if (current != null) {
                        return c.thenComparing(current.getSortMode().getValue().comparator())
                                .compare(o1, o2);
                    } else {
                        return c.compare(o1, o2);
                    }
                },
                category,
                mapped);
    }

    public static StoreSection createTopLevel(
            ObservableList<StoreEntryWrapper> all,
            Predicate<StoreEntryWrapper> entryFilter,
            ObservableStringValue filterString,
            ObservableValue<StoreCategoryWrapper> category) {
        var cached = BindingsHelper.cachedMappedContentBinding(
                all, storeEntryWrapper -> create(storeEntryWrapper, 1, all, entryFilter, filterString, category));
        var ordered = sorted(cached, category);
        var topLevel = BindingsHelper.filteredContentBinding(
                ordered,
                section -> {
                    var noParent = DataStorage.get()
                            .getParent(section.getWrapper().getEntry(), true)
                            .isEmpty();
                    var sameCategory =
                            category.getValue().contains(section.getWrapper().getEntry());
                    var diffParentCategory = DataStorage.get()
                            .getParent(section.getWrapper().getEntry(), true)
                            .map(entry -> !category.getValue().contains(entry))
                            .orElse(false);
                    var showFilter = section.shouldShow(filterString.get());
                    var matchesSelector = section.anyMatches(entryFilter);
                    return (noParent || diffParentCategory) && showFilter && sameCategory && matchesSelector;
                },
                category,
                filterString);
        return new StoreSection(null, cached, topLevel, 0);
    }

    private static StoreSection create(
            StoreEntryWrapper e,
            int depth,
            ObservableList<StoreEntryWrapper> all,
            Predicate<StoreEntryWrapper> entryFilter,
            ObservableStringValue filterString,
            ObservableValue<StoreCategoryWrapper> category) {
        if (e.getEntry().getState() == DataStoreEntry.State.LOAD_FAILED) {
            return new StoreSection(e, FXCollections.observableArrayList(), FXCollections.observableArrayList(), depth);
        }

        var allChildren = BindingsHelper.filteredContentBinding(all, other -> {
            return DataStorage.get()
                    .getParent(other.getEntry(), true)
                    .map(found -> found.equals(e.getEntry()))
                    .orElse(false);
        });
        var cached = BindingsHelper.cachedMappedContentBinding(
                allChildren, entry1 -> create(entry1, depth + 1, all, entryFilter, filterString, category));
        var ordered = sorted(cached, category);
        var filtered = BindingsHelper.filteredContentBinding(
                ordered,
                section -> {
                    return category.getValue().contains(section.getWrapper().getEntry())
                            && section.shouldShow(filterString.get())
                            && section.anyMatches(entryFilter);
                },
                category,
                filterString);
        return new StoreSection(e, cached, filtered, depth);
    }

    public boolean shouldShow(String filter) {
        return anyMatches(storeEntryWrapper -> storeEntryWrapper.shouldShow(filter));
    }

    public boolean anyMatches(Predicate<StoreEntryWrapper> c) {
        return c == null
                || c.test(wrapper)
                || allChildren.stream().anyMatch(storeEntrySection -> storeEntrySection.anyMatches(c));
    }
}
