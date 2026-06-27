package io.xpipe.app.hub.comp;

import io.xpipe.app.platform.BindingsHelper;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import lombok.Getter;
import org.int4.fx.values.util.Trigger;

import java.util.HashSet;
import java.util.function.Predicate;

public class StoreSectionState {

    private final ObservableList<StoreEntryWrapper> all;
    private final Trigger<Void> entriesListRefreshTrigger;
    private final Trigger<Void> entriesListVisibilityTrigger;

    private final ObservableValue<StoreFilter> filter;
    private final Predicate<StoreEntryWrapper> entryFilter;
    private final ObservableValue<StoreCategoryWrapper> category;
    private final ObservableList<StoreEntryWrapper> selected;
    private final IntegerProperty orderUpdateIndex = new SimpleIntegerProperty();
    private final ObservableBooleanValue enabled;
    @Getter
    private final StoreSection rootSection;

    public StoreSectionState( ObservableValue<StoreFilter> filter, Predicate<StoreEntryWrapper> entryFilter,
            ObservableValue<StoreCategoryWrapper> category, ObservableList<StoreEntryWrapper> selected, ObservableBooleanValue enabled) {
        this(StoreViewState.get().getAllEntries().getList(), StoreViewState.get().getEntriesListRefreshTrigger(), StoreViewState.get().getEntriesListVisibilityTrigger(), filter, entryFilter, category, selected, enabled);
    }

    public StoreSectionState(
            ObservableList<StoreEntryWrapper> all, Trigger<Void> entriesListRefreshTrigger,
            Trigger<Void> entriesListVisibilityTrigger, ObservableValue<StoreFilter> filter, Predicate<StoreEntryWrapper> entryFilter,
            ObservableValue<StoreCategoryWrapper> category, ObservableList<StoreEntryWrapper> selected, ObservableBooleanValue enabled) {
        this.all = all;
        this.entriesListRefreshTrigger = entriesListRefreshTrigger;
        this.entriesListVisibilityTrigger = entriesListVisibilityTrigger;
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
        rootSection.refreshAll(all, config, 0, orderUpdateIndex.get());
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

        BindingsHelper.attach(enabled, selected, () -> {
            updateShown();
        });

        BindingsHelper.attach(enabled, filter, () -> {
            updateShown();
        });

        BindingsHelper.attach(enabled, category, () -> {
            updateShown();
        });

        entriesListVisibilityTrigger.onFire(() -> {
            if (!enabled.get()) {
                return;
            }

            updateShown();
        });

        entriesListRefreshTrigger.onFire(() -> {
            if (!enabled.get()) {
                return;
            }

            orderUpdateIndex.setValue(orderUpdateIndex.get() + 1);
            updateAll();
        });
    }
}
