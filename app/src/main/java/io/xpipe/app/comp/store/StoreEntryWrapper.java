package io.xpipe.app.comp.store;

import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreColor;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.ThreadHelper;

import javafx.beans.property.*;
import javafx.collections.FXCollections;

import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Getter
public class StoreEntryWrapper {

    private final Property<String> name;
    private final DataStoreEntry entry;
    private final Property<Instant> lastAccess;
    private final Property<Instant> lastAccessApplied = new SimpleObjectProperty<>();
    private final BooleanProperty disabled = new SimpleBooleanProperty();
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final Property<DataStoreEntry.Validity> validity = new SimpleObjectProperty<>();
    private final ListProperty<ActionProvider> actionProviders =
            new SimpleListProperty<>(FXCollections.observableArrayList());
    private final Property<ActionProvider> defaultActionProvider = new SimpleObjectProperty<>();
    private final BooleanProperty deletable = new SimpleBooleanProperty();
    private final BooleanProperty expanded = new SimpleBooleanProperty();
    private final Property<Object> persistentState = new SimpleObjectProperty<>();
    private final Property<Map<String, Object>> cache = new SimpleObjectProperty<>(Map.of());
    private final Property<DataStoreColor> color = new SimpleObjectProperty<>();
    private final Property<StoreCategoryWrapper> category = new SimpleObjectProperty<>();
    private final Property<String> summary = new SimpleObjectProperty<>();
    private final Property<StoreNotes> notes;

    public StoreEntryWrapper(DataStoreEntry entry) {
        this.entry = entry;
        this.name = new SimpleStringProperty(entry.getName());
        this.lastAccess = new SimpleObjectProperty<>(entry.getLastAccess().minus(Duration.ofMillis(500)));
        ActionProvider.ALL_STANDALONE.stream()
                .filter(dataStoreActionProvider -> {
                    return !entry.isDisabled()
                            && dataStoreActionProvider.getLeafDataStoreCallSite() != null
                            && dataStoreActionProvider
                                    .getLeafDataStoreCallSite()
                                    .getApplicableClass()
                                    .isAssignableFrom(entry.getStore().getClass());
                })
                .sorted(Comparator.comparing(actionProvider ->
                        actionProvider.getLeafDataStoreCallSite().isSystemAction()))
                .forEach(dataStoreActionProvider -> {
                    actionProviders.add(dataStoreActionProvider);
                });
        this.notes = new SimpleObjectProperty<>(new StoreNotes(entry.getNotes(), entry.getNotes()));
        setupListeners();
    }

    public void applyLastAccess() {
        this.lastAccessApplied.setValue(lastAccess.getValue());
    }

    public void moveTo(DataStoreCategory category) {
        ThreadHelper.runAsync(() -> {
            DataStorage.get().updateCategory(entry, category);
        });
    }

    public void setOrder(DataStoreEntry.Order order) {
        ThreadHelper.runAsync(() -> {
            DataStorage.get().setOrder(getEntry(), order);
        });
    }

    public boolean isInStorage() {
        return DataStorage.get().getStoreEntries().contains(entry);
    }

    public void editDialog() {
        StoreCreationComp.showEdit(entry);
    }

    public void delete() {
        ThreadHelper.runAsync(() -> {
            DataStorage.get().deleteChildren(this.entry);
            DataStorage.get().deleteStoreEntry(this.entry);
        });
    }

    private void setupListeners() {
        name.addListener((c, o, n) -> {
            entry.setName(n);
        });

        expanded.addListener((c, o, n) -> {
            entry.setExpanded(n);
        });

        entry.addListener(() -> PlatformThread.runLaterIfNeeded(() -> {
            update();
        }));

        notes.addListener((observable, oldValue, newValue) -> {
            if (newValue.isCommited()) {
                entry.setNotes(newValue.getCurrent());
            }
        });
    }

    public void update() {
        // We are probably in shutdown then
        if (StoreViewState.get() == null) {
            return;
        }

        // Avoid reupdating name when changed from the name property!
        if (!entry.getName().equals(name.getValue())) {
            name.setValue(entry.getName());
        }

        lastAccess.setValue(entry.getLastAccess());
        disabled.setValue(entry.isDisabled());
        validity.setValue(entry.getValidity());
        expanded.setValue(entry.isExpanded());
        persistentState.setValue(entry.getStorePersistentState());
        // Use map copy to recognize update
        // This is a synchronized map, so we synchronize the access
        synchronized (entry.getStoreCache()) {
            cache.setValue(new HashMap<>(entry.getStoreCache()));
        }
        color.setValue(entry.getColor());
        notes.setValue(new StoreNotes(entry.getNotes(), entry.getNotes()));

        busy.setValue(entry.getBusyCounter().get() != 0);
        deletable.setValue(entry.getConfiguration().isDeletable()
                || AppPrefs.get().developerDisableGuiRestrictions().getValue());

        category.setValue(StoreViewState.get()
                .getCategoryWrapper(DataStorage.get()
                        .getStoreCategoryIfPresent(entry.getCategoryUuid())
                        .orElseThrow()));

        if (!entry.getValidity().isUsable()) {
            summary.setValue(null);
        } else {
            try {
                summary.setValue(
                        entry.getProvider() != null ? entry.getProvider().summaryString(this) : null);
            } catch (Exception ex) {
                // Summary creation might fail or have a bug
                ErrorEvent.fromThrowable(ex).handle();
            }
        }

        if (!isInStorage()) {
            actionProviders.clear();
            defaultActionProvider.setValue(null);
        } else {
            try {
                var defaultProvider = ActionProvider.ALL_STANDALONE.stream()
                        .filter(e -> entry.getStore() != null
                                && e.getDefaultDataStoreCallSite() != null
                                && e.getDefaultDataStoreCallSite()
                                        .getApplicableClass()
                                        .isAssignableFrom(entry.getStore().getClass())
                                && e.getDefaultDataStoreCallSite().isApplicable(entry.ref()))
                        .findFirst()
                        .orElse(null);
                this.defaultActionProvider.setValue(defaultProvider);

                var newProviders = ActionProvider.ALL_STANDALONE.stream()
                        .filter(dataStoreActionProvider -> {
                            return showActionProvider(dataStoreActionProvider);
                        })
                        .sorted(Comparator.comparing(actionProvider -> actionProvider.getLeafDataStoreCallSite() != null
                                && actionProvider.getLeafDataStoreCallSite().isSystemAction()))
                        .toList();
                if (!actionProviders.equals(newProviders)) {
                    actionProviders.setAll(newProviders);
                }
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).handle();
            }
        }
    }

    private boolean showActionProvider(ActionProvider p) {
        var leaf = p.getLeafDataStoreCallSite();
        if (leaf != null) {
            return (entry.getValidity().isUsable() || (!leaf.requiresValidStore() && entry.getProvider() != null))
                    && leaf.getApplicableClass()
                            .isAssignableFrom(entry.getStore().getClass())
                    && leaf.isApplicable(entry.ref());
        }

        var branch = p.getBranchDataStoreCallSite();
        if (branch != null
                && entry.getStore() != null
                && branch.getApplicableClass().isAssignableFrom(entry.getStore().getClass())) {
            return branch.getChildren(entry.ref()).stream().anyMatch(child -> {
                return showActionProvider(child);
            });
        }

        return false;
    }

    public void refreshChildren() {
        var hasChildren = DataStorage.get().refreshChildren(entry);
        PlatformThread.runLaterIfNeeded(() -> {
            expanded.set(hasChildren);
        });
    }

    public void executeDefaultAction() throws Exception {
        if (entry.getValidity() == DataStoreEntry.Validity.LOAD_FAILED) {
            return;
        }

        if (getEntry().getValidity() == DataStoreEntry.Validity.INCOMPLETE) {
            editDialog();
            return;
        }

        var found = getDefaultActionProvider().getValue();
        entry.notifyUpdate(true, false);
        if (found != null) {
            var act = found.getDefaultDataStoreCallSite().createAction(entry.ref());
            runAction(act, found.getDefaultDataStoreCallSite().showBusy());
        } else {
            entry.setExpanded(!entry.isExpanded());
        }
    }

    public void runAction(ActionProvider.Action action, boolean showBusy) throws Exception {
        try {
            if (showBusy) {
                getEntry().incrementBusyCounter();
            }
            action.execute();
        } finally {
            if (showBusy) {
                getEntry().decrementBusyCounter();
            }
        }
    }

    public void toggleExpanded() {
        this.expanded.set(!expanded.getValue());
    }

    public boolean matchesFilter(String filter) {
        if (filter == null || nameProperty().getValue().toLowerCase().contains(filter.toLowerCase())) {
            return true;
        }

        if (entry.getValidity().isUsable()
                && entry.getProvider().getSearchableTerms(entry.getStore()).stream()
                        .anyMatch(s -> s.toLowerCase().contains(filter.toLowerCase()))) {
            return true;
        }

        return false;
    }

    public Property<String> nameProperty() {
        return name;
    }

    public BooleanProperty disabledProperty() {
        return disabled;
    }
}
