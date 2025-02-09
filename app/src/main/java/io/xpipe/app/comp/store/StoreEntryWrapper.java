package io.xpipe.app.comp.store;

import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataColor;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.SingletonSessionStore;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;

import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

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
    private final Property<DataColor> color = new SimpleObjectProperty<>();
    private final Property<StoreCategoryWrapper> category = new SimpleObjectProperty<>();
    private final Property<String> summary = new SimpleObjectProperty<>();
    private final Property<StoreNotes> notes;
    private final Property<String> customIcon = new SimpleObjectProperty<>();
    private final Property<String> iconFile = new SimpleObjectProperty<>();
    private final BooleanProperty sessionActive = new SimpleBooleanProperty();
    private final Property<DataStore> store = new SimpleObjectProperty<>();
    private final Property<String> information = new SimpleStringProperty();
    private final BooleanProperty perUser = new SimpleBooleanProperty();
    private final ObservableValue<String> shownName;
    private final ObservableValue<String> shownSummary;
    private final ObservableValue<String> shownInformation;
    private final BooleanProperty largeCategoryOptimizations = new SimpleBooleanProperty();

    private boolean effectiveBusyProviderBound = false;
    private final BooleanProperty effectiveBusy = new SimpleBooleanProperty();

    public StoreEntryWrapper(DataStoreEntry entry) {
        this.entry = entry;
        this.name = new SimpleStringProperty(entry.getName());
        this.lastAccess = new SimpleObjectProperty<>(entry.getLastAccess().minus(Duration.ofMillis(500)));
        this.shownName = Bindings.createStringBinding(
                () -> {
                    var n = name.getValue();
                    return n != null && AppPrefs.get().censorMode().get() ? "*".repeat(n.length()) : n;
                },
                AppPrefs.get().censorMode(),
                name);
        this.shownSummary = Bindings.createStringBinding(
                () -> {
                    var n = summary.getValue();
                    return n != null && AppPrefs.get().censorMode().get() ? "*".repeat(n.length()) : n;
                },
                AppPrefs.get().censorMode(),
                summary);
        this.shownInformation = Bindings.createStringBinding(
                () -> {
                    var n = information.getValue();
                    return n != null && AppPrefs.get().censorMode().get() ? "*".repeat(n.length()) : n;
                },
                AppPrefs.get().censorMode(),
                information);
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
            DataStorage.get().moveEntryToCategory(entry, category);
        });
    }

    public void setOrder(DataStoreEntry.Order order) {
        ThreadHelper.runAsync(() -> {
            DataStorage.get().setOrder(getEntry(), order);
        });
    }

    public boolean isInStorage() {
        return DataStorage.get() != null && DataStorage.get().getStoreEntries().contains(entry);
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

    public void stopSession() {
        ThreadHelper.runFailableAsync(() -> {
            if (entry.getStore() instanceof SingletonSessionStore<?> singletonSessionStore) {
                singletonSessionStore.stopSessionIfNeeded();
            }
        });
    }

    public synchronized void update() {
        // We are probably in shutdown then
        if (StoreViewState.get() == null) {
            return;
        }

        // Avoid reupdating name when changed from the name property!
        if (!entry.getName().equals(name.getValue())) {
            name.setValue(entry.getName());
        }

        if (effectiveBusyProviderBound && !getValidity().getValue().isUsable()) {
            this.effectiveBusyProviderBound = false;
            this.effectiveBusy.unbind();
            this.effectiveBusy.bind(busy);
        }

        lastAccess.setValue(entry.getLastAccess());
        disabled.setValue(entry.isDisabled());
        validity.setValue(entry.getValidity());
        expanded.setValue(entry.isExpanded());
        persistentState.setValue(entry.getStorePersistentState());
		
        // The property values are only registered as changed once they are queried
        // If we use information bindings that depend on some of these properties
        // but use the store methods to retrieve data instead of the wrapper properties,
        // the bindings do not get updated as the change events are not fired.
        // We can also fire them manually with this
        persistentState.getValue();

        // Use map copy to recognize update
        // This is a synchronized map, so we synchronize the access
        synchronized (entry.getStoreCache()) {
            if (!entry.getStoreCache().equals(cache.getValue())) {
                cache.setValue(new HashMap<>(entry.getStoreCache()));
                // Same here
                cache.getValue();
            }
        }
        color.setValue(entry.getColor());
        notes.setValue(new StoreNotes(entry.getNotes(), entry.getNotes()));
        customIcon.setValue(entry.getIcon());
        iconFile.setValue(entry.getEffectiveIconFile());
        busy.setValue(entry.getBusyCounter().get() != 0);
        deletable.setValue(entry.getConfiguration().isDeletable());
        sessionActive.setValue(entry.getStore() instanceof SingletonSessionStore<?> ss
                && entry.getStore() instanceof ShellStore
                && ss.isSessionRunning());
        category.setValue(StoreViewState.get().getCategories().getList().stream()
                .filter(storeCategoryWrapper ->
                        storeCategoryWrapper.getCategory().getUuid().equals(entry.getCategoryUuid()))
                .findFirst()
                .orElse(StoreViewState.get().getAllConnectionsCategory()));
        largeCategoryOptimizations.setValue(category.getValue().getLargeCategoryOptimizations().getValue());
        perUser.setValue(
                !category.getValue().getRoot().equals(StoreViewState.get().getAllIdentitiesCategory())
                        && entry.isPerUserStore());

        var storeChanged = store.getValue() != entry.getStore();
        store.setValue(entry.getStore());
        if (storeChanged || !information.isBound()) {
            if (entry.getProvider() != null) {
                var section = StoreViewState.get().getSectionForWrapper(this);
                if (section.isPresent()) {
                    information.unbind();
                    try {
                        var is = entry.getProvider().informationString(section.get());
                        information.bind(is);
                    } catch (Exception e) {
                        ErrorEvent.fromThrowable(e).handle();
                        information.bind(new SimpleStringProperty());
                    }
                }
            }
        }

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

        if (!effectiveBusyProviderBound && getValidity().getValue().isUsable()) {
            this.effectiveBusyProviderBound = true;
            this.effectiveBusy.unbind();
            this.effectiveBusy.bind(busy.or(getEntry().getProvider().busy(this)));
        }

        if (!this.effectiveBusy.isBound() && !getValidity().getValue().isUsable()) {
            this.effectiveBusy.bind(busy);
        }
    }

    public boolean showActionProvider(ActionProvider p) {
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
                && branch.getApplicableClass().isAssignableFrom(entry.getStore().getClass())
                && branch.isApplicable(entry.ref())) {
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
        if (filter == null || name.getValue().toLowerCase().contains(filter.toLowerCase())) {
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
}
