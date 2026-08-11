package io.xpipe.app.hub.section;

import io.xpipe.app.hub.category.StoreCategoryWrapper;
import io.xpipe.app.hub.entry.StoreEntryWrapper;
import io.xpipe.app.hub.list.StoreFilter;
import io.xpipe.app.hub.list.StoreSectionDrag;
import io.xpipe.app.hub.list.StoreViewState;
import io.xpipe.app.platform.DerivedObservableList;
import io.xpipe.app.platform.Listeners;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import lombok.Getter;
import org.int4.fx.values.util.Trigger;

import java.util.HashSet;
import java.util.List;

public class StoreSectionState {

    private final ObservableList<StoreEntryWrapper> all;
    private final Trigger<Void> entriesListRefreshTrigger;
    private final Trigger<Void> entriesListVisibilityTrigger;

    private final ObservableValue<StoreFilter> filter;
    private final StoreSectionSelector selector;
    private final ObservableValue<StoreCategoryWrapper> category;
    private final ObservableList<StoreEntryWrapper> selected;
    private final ObservableValue<StoreSectionSortMode> sortMode;
    private final ObservableValue<StoreSectionDrag> dragOperation;
    private final ObservableList<StoreEntryWrapper> added = FXCollections.observableArrayList();
    private final IntegerProperty orderUpdateIndex = new SimpleIntegerProperty();
    private final ObservableBooleanValue enabled;

    @Getter
    private final StoreSection rootSection;

    public StoreSectionState(
            ObservableValue<StoreFilter> filter,
            StoreSectionSelector selector,
            ObservableValue<StoreCategoryWrapper> category,
            ObservableList<StoreEntryWrapper> selected,
            ObservableBooleanValue enabled) {
        var svs = StoreViewState.get();
        this(
                StoreViewState.get().getAllEntries().getList(),
                StoreViewState.get().getEntriesListRefreshTrigger(),
                StoreViewState.get().getEntriesListVisibilityTrigger(),
                filter,
                selector,
                category,
                selected,
                svs.getSortMode(),
                StoreViewState.get().getSectionDragOperation(),
                enabled);
    }

    public StoreSectionState(
            ObservableList<StoreEntryWrapper> all,
            Trigger<Void> entriesListRefreshTrigger,
            Trigger<Void> entriesListVisibilityTrigger,
            ObservableValue<StoreFilter> filter,
            StoreSectionSelector selector,
            ObservableValue<StoreCategoryWrapper> category,
            ObservableList<StoreEntryWrapper> selected,
            ObservableValue<StoreSectionSortMode> sortMode,
            ObservableValue<StoreSectionDrag> dragOperation,
            ObservableBooleanValue enabled) {
        this.all = all;
        this.entriesListRefreshTrigger = entriesListRefreshTrigger;
        this.entriesListVisibilityTrigger = entriesListVisibilityTrigger;
        this.filter = filter;
        this.selector = selector;
        this.category = category;
        this.selected = selected;
        this.sortMode = sortMode;
        this.dragOperation = dragOperation;
        this.enabled = enabled;
        this.rootSection = new StoreSection(null, 0);

        addListeners();
    }

    private void updateAll() {
        var parents = new HashSet<StoreEntryWrapper>();
        var config = new StoreSectionConfig(
                parents,
                selector,
                filter.getValue(),
                category.getValue(),
                new HashSet<>(selected),
                dragOperation.getValue(),
                new HashSet<>(added));
        rootSection.refreshAll(all, config, 0, orderUpdateIndex.get());
        rootSection.refreshShown(config);
        rootSection.apply(true);
    }

    private void updateShown(boolean alwaysUpdateAll) {
        var parents = new HashSet<StoreEntryWrapper>();
        var config = new StoreSectionConfig(
                parents,
                selector,
                filter.getValue(),
                category.getValue(),
                new HashSet<>(selected),
                dragOperation.getValue(),
                new HashSet<>(added));
        rootSection.refreshShown(config);
        rootSection.apply(alwaysUpdateAll);
    }

    private void addListeners() {
        Listeners.attach(enabled, all, () -> {
            updateAll();
        });

        Listeners.attach(enabled, selected, () -> {
            updateShown(false);
        });

        Listeners.attach(enabled, filter, () -> {
            added.clear();
            updateShown(false);
        });

        Listeners.listen(enabled, StoreViewState.get().getAllEntries().getList(), change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    added.addAll(change.getAddedSubList());
                }
            }
            updateShown(false);
        });

        Listeners.attach(enabled, category, () -> {
            updateShown(true);
        });

        var draggedSelection = DerivedObservableList.<StoreEntryWrapper>arrayList(true);
        Listeners.attach(enabled, dragOperation, () -> {
            draggedSelection.setContent(
                    dragOperation.getValue() != null ? dragOperation.getValue().getSelection() : List.of());
        });
        Listeners.subscribeList(draggedSelection.getList(), () -> {
            updateShown(false);
        });

        Listeners.attach(enabled, sortMode, () -> {
            orderUpdateIndex.setValue(orderUpdateIndex.get() + 1);
            updateAll();
        });

        entriesListVisibilityTrigger.onFire(() -> {
            if (!enabled.get()) {
                return;
            }

            updateShown(true);
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
