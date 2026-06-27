package io.xpipe.app.hub.comp;

import io.xpipe.app.platform.BindingsHelper;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import lombok.Getter;

import java.util.HashSet;
import java.util.function.Predicate;

public class StoreSectionState {

    private final ObservableList<StoreEntryWrapper> all;
    private final ObservableValue<StoreFilter> filter;
    private final Predicate<StoreEntryWrapper> entryFilter;
    private final ObservableValue<StoreCategoryWrapper> category;
    private final ObservableList<StoreEntryWrapper> selected;
    private final ObservableBooleanValue enabled;
    @Getter
    private final StoreSection rootSection;

    public StoreSectionState(
            ObservableList<StoreEntryWrapper> all, ObservableValue<StoreFilter> filter, Predicate<StoreEntryWrapper> entryFilter,
            ObservableValue<StoreCategoryWrapper> category, ObservableList<StoreEntryWrapper> selected, ObservableBooleanValue enabled) {
        this.all = all;
        this.filter = filter;
        this.entryFilter = entryFilter;
        this.category = category;
        this.selected = selected;
        this.enabled = enabled;
        this.rootSection = new StoreSection(null, 0);

        addListeners();
    }

    private void updateAll() {
        var parents = new HashSet<StoreEntryWrapper>();
        var config = new StoreSectionConfig(parents, entryFilter, filter.getValue(), category.getValue(), new HashSet<>(selected));
        rootSection.refreshAll(all, config);
        rootSection.refreshShown(config);
        rootSection.apply();
    }

    private void updateShown() {
        var parents = new HashSet<StoreEntryWrapper>();
        var config = new StoreSectionConfig(parents, entryFilter, filter.getValue(), category.getValue(), new HashSet<>(selected));
        rootSection.refreshShown(config);
        rootSection.apply();
    }

    private void addListeners() {
        BindingsHelper.attach(enabled, all, () -> {
            updateAll();
        });

        BindingsHelper.attach(enabled, filter, () -> {
            updateShown();
        });

        BindingsHelper.attach(enabled, category, () -> {
            updateShown();
        });
    }
}
