package io.xpipe.app.hub.comp;

import io.xpipe.app.action.*;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.GroupStore;
import io.xpipe.app.ext.LocalStore;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.ext.SingletonSessionStore;
import io.xpipe.app.hub.action.HubBranchProvider;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.HubMenuItemProvider;
import io.xpipe.app.hub.action.impl.EditHubLeafProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreColor;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.FixedHierarchyStore;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.app.util.ThreadHelper;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;

import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class StoreEntryWrapper {

    private final Property<String> name;
    private final DataStoreEntry entry;
    private final Property<Instant> lastAccess;
    private final BooleanProperty disabled = new SimpleBooleanProperty();
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final Property<DataStoreEntry.Validity> validity = new SimpleObjectProperty<>();
    private final ListProperty<HubMenuItemProvider<?>> majorActionProviders =
            new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<HubMenuItemProvider<?>> minorActionProviders =
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
    private final Property<String> customIcon = new SimpleObjectProperty<>();
    private final Property<String> iconFile = new SimpleObjectProperty<>();
    private final BooleanProperty sessionActive = new SimpleBooleanProperty();
    private final Property<DataStore> store = new SimpleObjectProperty<>();
    private final Property<String> information = new SimpleStringProperty();
    private final BooleanProperty perUser = new SimpleBooleanProperty();
    private final ObservableValue<String> shownName;
    private final ObservableValue<String> shownSummary;
    private final Property<String> shownInformation;
    private final BooleanProperty largeCategoryOptimizations = new SimpleBooleanProperty();
    private final BooleanProperty readOnly = new SimpleBooleanProperty();
    private final BooleanProperty renaming = new SimpleBooleanProperty();
    private final BooleanProperty pinToTop = new SimpleBooleanProperty();
    private final IntegerProperty orderIndex = new SimpleIntegerProperty();
    private final BooleanProperty effectiveBusy = new SimpleBooleanProperty();
    private boolean effectiveBusyProviderBound = false;

    public StoreEntryWrapper(DataStoreEntry entry) {
        this.entry = entry;
        this.name = new SimpleStringProperty(entry.getName());
        this.lastAccess = new SimpleObjectProperty<>(entry.getLastAccess().minus(Duration.ofMillis(500)));
        this.shownName = Bindings.createStringBinding(
                () -> {
                    var n = name.getValue();
                    if (n == null) {
                        n = "?";
                    }

                    return AppPrefs.get().censorMode().get() ? "*".repeat(n.length()) : n;
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
        this.shownInformation = new SimpleObjectProperty<>();
        this.notes = new SimpleObjectProperty<>(new StoreNotes(entry.getNotes(), entry.getNotes()));

        setupListeners();
    }

    public void moveTo(DataStoreCategory category) {
        ThreadHelper.runAsync(() -> {
            DataStorage.get().moveEntryToCategory(entry, category);
        });
    }

    public boolean isInStorage() {
        return DataStorage.get() != null && DataStorage.get().getStoreEntries().contains(entry);
    }

    public void editDialog() {
        StoreCreationDialog.showEdit(entry);
    }

    public void delete() {
        ThreadHelper.runAsync(() -> {
            DataStorage.get().deleteWithChildren(this.entry);
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

        // Use map copy to recognize update
        // This is a synchronized map, so we synchronize the access
        synchronized (entry.getStoreCache()) {
            if (!entry.getStoreCache().equals(cache.getValue())) {
                cache.setValue(new HashMap<>(entry.getStoreCache()));
            }
        }
        orderIndex.setValue(entry.getOrderIndex());
        color.setValue(entry.getColor());
        notes.setValue(new StoreNotes(entry.getNotes(), entry.getNotes()));
        customIcon.setValue(entry.getIcon());
        readOnly.setValue(entry.isFreeze());
        iconFile.setValue(entry.getEffectiveIconFile());
        busy.setValue(entry.getBusyCounter().get() != 0);
        deletable.setValue(
                !(entry.getStore() instanceof LocalStore) && !DataStorage.get().getEffectiveReadOnlyState(entry));
        sessionActive.setValue(entry.getStore() instanceof SingletonSessionStore<?> ss
                && entry.getStore() instanceof ShellStore
                && ss.isSessionRunning());
        category.setValue(StoreViewState.get().getCategories().getList().stream()
                .filter(storeCategoryWrapper ->
                        storeCategoryWrapper.getCategory().getUuid().equals(entry.getCategoryUuid()))
                .findFirst()
                .orElse(StoreViewState.get().getAllConnectionsCategory()));
        largeCategoryOptimizations.setValue(
                category.getValue().getLargeCategoryOptimizations().getValue());
        perUser.setValue(
                !category.getValue().getRoot().equals(StoreViewState.get().getAllIdentitiesCategory())
                        && entry.isPerUserStore());
        pinToTop.setValue(entry.isPinToTop());

        var storeChanged = store.getValue() != entry.getStore();
        store.setValue(entry.getStore());
        if (storeChanged || !information.isBound()) {
            information.unbind();
            shownInformation.unbind();
            if (entry.getValidity().isUsable()) {
                var section = StoreViewState.get().getSectionForWrapper(this);
                if (section.isPresent()) {
                    try {
                        var is = entry.getProvider().informationString(section.get());
                        information.bind(is);
                        shownInformation.bind(Bindings.createStringBinding(
                                () -> {
                                    // Might have changed validity meanwhile
                                    if (!entry.getValidity().isUsable()) {
                                        return "";
                                    }

                                    var n = information.getValue();
                                    return n != null
                                                    && AppPrefs.get()
                                                            .censorMode()
                                                            .get()
                                            ? "*".repeat(n.length())
                                            : n;
                                },
                                AppPrefs.get().censorMode(),
                                information));
                    } catch (Exception e) {
                        ErrorEventFactory.fromThrowable(e).omit().handle();
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
                ErrorEventFactory.fromThrowable(ex).omit().handle();
            }
        }

        if (!isInStorage()) {
            majorActionProviders.clear();
            defaultActionProvider.setValue(null);
        } else {
            try {
                var defaultProvider = ActionProvider.ALL.stream()
                        .filter(e -> entry.getStore() != null
                                && e instanceof HubLeafProvider<?> def
                                && (entry.getValidity().isUsable()
                                        || (!def.requiresValidStore() && entry.getProvider() != null))
                                && def.getApplicableClass()
                                        .isAssignableFrom(entry.getStore().getClass())
                                && def.isApplicable(entry.ref())
                                && def.isDefault(entry.ref()))
                        .findFirst()
                        .or(() -> {
                            if (entry.getStore() instanceof GroupStore<?>) {
                                return Optional.empty();
                            } else if (entry.getProvider() != null
                                    && entry.getProvider().canConfigure()) {
                                return Optional.of(new EditHubLeafProvider());
                            } else {
                                return Optional.empty();
                            }
                        })
                        .orElse(null);
                this.defaultActionProvider.setValue(defaultProvider);

                var newMajorProviders = ActionProvider.ALL.stream()
                        .map(actionProvider -> actionProvider instanceof HubMenuItemProvider<?> sa ? sa : null)
                        .filter(Objects::nonNull)
                        .filter(dataStoreActionProvider -> {
                            return showActionProvider(dataStoreActionProvider, true);
                        })
                        .toList();
                if (!majorActionProviders.equals(newMajorProviders)) {
                    majorActionProviders.setAll(newMajorProviders);
                }

                var newMinorProviders = ActionProvider.ALL.stream()
                        .map(actionProvider -> actionProvider instanceof HubMenuItemProvider<?> sa ? sa : null)
                        .filter(Objects::nonNull)
                        .filter(dataStoreActionProvider -> {
                            return showActionProvider(dataStoreActionProvider, false);
                        })
                        .collect(Collectors.toCollection(ArrayList::new));
                newMinorProviders.removeIf(storeActionProvider -> {
                    return newMajorProviders.stream().anyMatch(mj -> {
                        return mj instanceof HubBranchProvider<?> branch
                                && branch.getChildren(entry.ref()).stream()
                                        .anyMatch(c -> c.getClass().equals(storeActionProvider.getClass()));
                    });
                });
                if (!minorActionProviders.equals(newMinorProviders)) {
                    minorActionProviders.setAll(newMinorProviders);
                }
            } catch (Exception ex) {
                ErrorEventFactory.fromThrowable(ex).omit().handle();
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

        // The property values are only registered as changed once they are queried
        // If we use information bindings that depend on some of these properties
        // but use the store methods to retrieve data instead of the wrapper properties,
        // the bindings do not get updated as the change events are not fired.
        // We can also fire them manually with this
        persistentState.getValue();
        store.getValue();
        cache.getValue();
    }

    public boolean showActionProvider(ActionProvider p, boolean major) {
        if (p instanceof HubLeafProvider<?> leaf) {
            return (entry.getValidity().isUsable() || (!leaf.requiresValidStore() && entry.getProvider() != null))
                    && leaf.getApplicableClass()
                            .isAssignableFrom(entry.getStore().getClass())
                    && leaf.isApplicable(entry.ref())
                    && (!major || leaf.isMajor(entry.ref()));
        }

        if (p instanceof HubBranchProvider<?> branch
                && entry.getStore() != null
                && branch.getApplicableClass().isAssignableFrom(entry.getStore().getClass())
                && branch.isApplicable(entry.ref())
                && (!major || branch.isMajor(entry.ref()))) {
            return branch.getChildren(entry.ref()).stream().anyMatch(child -> {
                return showActionProvider(child, false);
            });
        }

        return false;
    }

    public boolean canBreakOutCategory() {
        return (getStore().getValue() instanceof FixedHierarchyStore
                        || getStore().getValue() instanceof GroupStore<?>)
                && StoreViewState.get().getParentSectionForWrapper(this).isPresent();
    }

    public void breakOutCategory() {
        ThreadHelper.runAsync(() -> {
            var cat = DataStorage.get().breakOutCategory(entry);
            if (cat != null) {
                Platform.runLater(() -> {
                    StoreViewState.get()
                            .getActiveCategory()
                            .setValue(StoreViewState.get().getCategoryWrapper(cat));
                });
            }
        });
    }

    public Optional<StoreCategoryWrapper> getBreakoutCategory() {
        if (entry.getBreakOutCategory() == null) {
            return Optional.empty();
        }

        var cat = DataStorage.get().getStoreCategoryIfPresent(entry.getBreakOutCategory());
        if (cat.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(StoreViewState.get().getCategoryWrapper(cat.get()));
    }

    public void mergeBreakOutCategory() {
        ThreadHelper.runAsync(() -> {
            DataStorage.get().mergeBreakOutCategory(entry);
            Platform.runLater(() -> {
                StoreViewState.get()
                        .getActiveCategory()
                        .setValue(StoreViewState.get()
                                .getCategoryWrapper(DataStorage.get().getStoreCategory(entry)));
            });
        });
    }

    public void refreshChildren() {
        var hasChildren = DataStorage.get().refreshChildren(entry);
        PlatformThread.runLaterIfNeeded(() -> {
            expanded.set(hasChildren);
        });
    }

    public void executeDefaultAction() {
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
            if (found instanceof HubLeafProvider<?> def) {
                def.execute(getEntry().ref());
            }
        } else {
            entry.setExpanded(!entry.isExpanded());
        }
    }

    public void orderWithIndex(int index) {
        DataStorage.get().setOrderIndex(entry, index);
    }

    public void orderLast() {
        var section = StoreViewState.get().getParentSectionForWrapper(this);
        if (section.isEmpty()) {
            return;
        }

        var isSingle = section.get().getAllChildren().getList().stream()
                        .filter(sec -> sec.getWrapper().getOrderIndex().get() == orderIndex.getValue())
                        .count()
                == 1;
        var max = section.get().getAllChildren().getList().stream()
                .map(sec -> sec.getWrapper().getOrderIndex().getValue())
                .filter(value -> value != null && value != Integer.MIN_VALUE && value != Integer.MAX_VALUE)
                .mapToInt(value -> value)
                .max()
                .orElse(0);
        if (isSingle && max == orderIndex.getValue()) {
            return;
        }

        orderWithIndex(max + 1);
    }

    public void orderFirst() {
        var section = StoreViewState.get().getParentSectionForWrapper(this);
        if (section.isEmpty()) {
            return;
        }

        var isSingle = section.get().getAllChildren().getList().stream()
                        .filter(sec -> sec.getWrapper().getOrderIndex().get() == orderIndex.getValue())
                        .count()
                == 1;
        var min = section.get().getAllChildren().getList().stream()
                .map(sec -> sec.getWrapper().getOrderIndex().getValue())
                .filter(value -> value != null && value != Integer.MIN_VALUE && value != Integer.MAX_VALUE)
                .mapToInt(value -> value)
                .min()
                .orElse(0);
        if (isSingle && min == orderIndex.getValue()) {
            return;
        }

        orderWithIndex(min - 1);
    }

    public void orderStickFirst() {
        orderWithIndex(Integer.MIN_VALUE);
    }

    public void orderStickLast() {
        orderWithIndex(Integer.MAX_VALUE);
    }

    public void toggleExpanded() {
        this.expanded.set(!expanded.getValue());
    }

    public void togglePinToTop() {
        if (getEntry().isPinToTop()) {
            getEntry().setPinToTop(false);
            StoreViewState.get().triggerStoreListUpdate();
        } else {
            var root = StoreViewState.get().getCurrentTopLevelSection().getAllChildren().getList().stream()
                    .filter(storeSection -> storeSection.anyMatches(storeEntryWrapper -> storeEntryWrapper == this))
                    .findFirst();
            var sortMode = StoreSectionSortMode.DATE_DESC;
            var date = root.isPresent()
                    ? sortMode.date(sortMode.getRepresentative(root.get())).plus(Duration.ofSeconds(1))
                    : Instant.now();
            getEntry().setPinToTop(!getEntry().isPinToTop());
            StoreViewState.get().triggerStoreListUpdate();
            getEntry().setLastUsed(date);
            getEntry().setLastModified(date);
            StoreViewState.get().triggerStoreListUpdate();
        }
    }

    public boolean matchesFilter(String filter) {
        if (filter == null || name.getValue().toLowerCase().contains(filter.toLowerCase())) {
            return true;
        }

        if (getEntry().getUuid().toString().equalsIgnoreCase(filter)) {
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
