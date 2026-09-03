package io.xpipe.app.hub.entry;

import io.xpipe.app.action.*;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppSizeBreakpoints;
import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.hub.action.HubBranchProvider;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.HubMenuItemProvider;
import io.xpipe.app.hub.action.impl.EditHubLeafProvider;
import io.xpipe.app.hub.category.StoreCategoryWrapper;
import io.xpipe.app.hub.creation.StoreCreationDialog;
import io.xpipe.app.hub.list.StoreFilter;
import io.xpipe.app.hub.list.StoreViewState;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.DerivedObservableList;
import io.xpipe.app.platform.Listeners;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.DataStorageAccessType;
import io.xpipe.app.secret.DataStorageAccessHandler;
import io.xpipe.app.storage.*;
import io.xpipe.app.store.DataStore;
import io.xpipe.app.store.FixedHierarchyStore;
import io.xpipe.app.store.GroupStore;
import io.xpipe.app.store.LocalStore;
import io.xpipe.app.store.ShellStore;
import io.xpipe.app.store.SingletonSessionStore;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.ThreadHelper;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import lombok.Getter;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class StoreEntryWrapper {

    private final Property<String> name = new SimpleObjectProperty<>();
    private final DataStoreEntry entry;
    private final Property<Instant> lastAccess = new SimpleObjectProperty<>();
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
    private final ObjectProperty<String> notes = new SimpleObjectProperty<>();
    private final Property<String> iconFile = new SimpleObjectProperty<>();
    private final BooleanProperty sessionActive = new SimpleBooleanProperty();
    private final Property<DataStore> store = new SimpleObjectProperty<>();
    private final Property<StoreEntryInformation> information = new SimpleObjectProperty<>();
    private final BooleanProperty accessScopeRestricted = new SimpleBooleanProperty();
    private final Property<String> shownName = new SimpleObjectProperty<>();
    private final Property<String> shownSummary = new SimpleObjectProperty<>();
    private final Property<String> shownDescription = new SimpleObjectProperty<>();
    private final Property<StoreEntryInformation> shownInformation = new SimpleObjectProperty<>();
    private final BooleanProperty template = new SimpleBooleanProperty();
    private final BooleanProperty renaming = new SimpleBooleanProperty();
    private final BooleanProperty pinToTop = new SimpleBooleanProperty();
    private final DoubleProperty orderIndex = new SimpleDoubleProperty();
    private final BooleanProperty effectiveBusy = new SimpleBooleanProperty();
    private final Property<StoreCategoryWrapper> lastInformationCategory = new SimpleObjectProperty<>();
    private final ObservableList<String> tags = FXCollections.observableArrayList();
    private boolean effectiveBusyProviderBound = false;

    public StoreEntryWrapper(DataStoreEntry entry) {
        this.entry = entry;

        setupListeners();
    }

    public void moveTo(DataStoreCategory category) {
        var oldCat = getCategory().getValue();
        var newCat = StoreViewState.get().getCategoryWrapper(category);

        ThreadHelper.runAsync(() -> {
            DataStorage.get().moveEntryToCategory(entry, category);
            Platform.runLater(() -> {
                oldCat.update();
                newCat.update();
            });
        });
    }

    public boolean includeInConnectionCount() {
        return getEntry().getProvider() != null && getEntry().getProvider().includeInConnectionCount();
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

        Listeners.listenWeak(this, AppPrefs.get().censorMode(), (wrapper, v) -> {
            wrapper.update();
        });

        Listeners.listenWeak(this, LicenseProvider.get().licenseTitle(), (wrapper, v) -> {
            wrapper.update();
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
        if (AppOperationMode.isInShutdown() || StoreViewState.get() == null) {
            return;
        }

        // We received a delayed update after removal
        if (!DataStorage.get().getStoreEntries().contains(entry)) {
            return;
        }

        // Avoid reupdating name when changed from the name property!
        if (!entry.getName().equals(name.getValue())) {
            name.setValue(entry.getName());
        }

        shownName.setValue(
                AppPrefs.get().censorMode().get() ? "*".repeat(name.getValue().length()) : name.getValue());

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
        color.setValue(DataStorage.get().getEffectiveColor(entry));
        notes.setValue(entry.getNotes());
        template.setValue(entry.isTemplate());
        iconFile.setValue(entry.getEffectiveIconFile());
        busy.setValue(entry.getBusyCounter().get() != 0);
        deletable.setValue(
                !(entry.getStore() instanceof LocalStore) && !DataStorage.get().getEffectiveReadOnlyState(entry));
        sessionActive.setValue(entry.getStore() instanceof SingletonSessionStore<?> ss
                && entry.getStore() instanceof ShellStore
                && ss.isSessionRunning());
        var newCat = StoreViewState.get().getCategories().getList().stream()
                .filter(storeCategoryWrapper ->
                        storeCategoryWrapper.getCategory().getUuid().equals(entry.getCategoryUuid()))
                .findFirst()
                .orElse(StoreViewState.get().getAllConnectionsCategory());
        category.setValue(newCat);
        accessScopeRestricted.setValue(DataStorageAccessHandler.getInstance().getType() == DataStorageAccessType.ROLE
                && entry.getAccessScope().isAccessSubRestricted());
        pinToTop.setValue(entry.isPinToTop());

        var orderedTags = entry.getTags().stream().sorted().toList();
        DerivedObservableList.wrap(tags, true).setContent(orderedTags);

        store.setValue(entry.getStore());

        var selectedCat = StoreViewState.get().getActiveCategory().getValue();
        lastInformationCategory.setValue(selectedCat);

        if (entry.getValidity().isUsable()
                || (entry.getValidity() != DataStoreEntry.Validity.LOAD_FAILED
                        && entry.getProvider().showIncompleteInfo())) {
            var section = StoreViewState.get().getSectionForWrapper(this);
            if (section.isPresent()) {
                try {
                    var is = entry.getProvider().buildInformation(section.get());
                    if (is != null && !is.isValid()) {
                        is = null;
                    }

                    information.setValue(is);

                    if (is != null && AppPrefs.get().censorMode().get()) {
                        shownInformation.setValue(is.censored());
                    } else {
                        shownInformation.setValue(is);
                    }
                } catch (Exception e) {
                    ErrorEventFactory.fromThrowable(e).omit().handle();
                    information.setValue(null);
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

        shownSummary.setValue(
                summary.getValue() != null && AppPrefs.get().censorMode().get()
                        ? "*".repeat(summary.getValue().length())
                        : summary.getValue());

        if (shownSummary.getValue() != null) {
            shownDescription.setValue(shownSummary.getValue());
        } else {
            var provider = getEntry().getProvider();
            if (provider != null) {
                var providerName = AppI18n.get(provider.getId() + ".displayName");
                shownDescription.setValue(
                        AppPrefs.get().censorMode().get() ? "*".repeat(providerName.length()) : providerName);
            } else {
                shownDescription.setValue(null);
            }
        }

        if (!isInStorage()) {
            minorActionProviders.clear();
            majorActionProviders.clear();
            defaultActionProvider.setValue(null);
        } else {
            try {
                if (!template.get()) {
                    var defaultProvider = ActionProvider.ALL.stream()
                            .filter(e -> entry.getStore() != null
                                    && e instanceof HubLeafProvider<?> def
                                    && (entry.getValidity().isUsable()
                                            || (!def.requiresValidStore() && entry.getProvider() != null))
                                    && def.getApplicableClass()
                                            .isAssignableFrom(entry.getStore().getClass())
                                    && def.isApplicable(entry.ref())
                                    && def.isDefault())
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
                } else {
                    minorActionProviders.clear();
                    majorActionProviders.clear();
                    this.defaultActionProvider.setValue(new EditHubLeafProvider());
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
                    && ((!AppSizeBreakpoints.compactMode().get() && major == leaf.isMajor())
                            || (AppSizeBreakpoints.compactMode().get() && !major));
        }

        if (p instanceof HubBranchProvider<?> branch
                && entry.getStore() != null
                && branch.getApplicableClass().isAssignableFrom(entry.getStore().getClass())
                && branch.isApplicable(entry.ref())
                && ((!AppSizeBreakpoints.compactMode().get() && major == branch.isMajor())
                        || (AppSizeBreakpoints.compactMode().get() && !major))) {
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

    public void toggleTag(String tag) {
        if (tags.contains(tag)) {
            entry.removeTag(tag);
        } else {
            entry.addTag(tag);
        }
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

    public void executeDefaultAction() {
        if (entry.getValidity() == DataStoreEntry.Validity.LOAD_FAILED) {
            return;
        }

        if (getEntry().getValidity() == DataStoreEntry.Validity.INCOMPLETE) {
            if (entry.getProvider().canConfigure()) {
                editDialog();
            }
            return;
        }

        var found = getDefaultActionProvider().getValue();
        if (found != null) {
            if (found instanceof HubLeafProvider<?> def) {
                def.execute(getEntry().ref());
            }
        } else {
            entry.setExpanded(!entry.isExpanded());
        }
    }

    public boolean canDrag() {
        return true;
    }

    public void orderWithIndex(double index) {
        DataStorage.get().setOrderIndex(entry, index);
    }

    public void toggleExpanded() {
        this.expanded.set(!expanded.getValue());
    }

    public boolean matchesFilter(StoreFilter filter) {
        if (filter == null) {
            return true;
        }

        var l = new ArrayList<String>();
        l.add(name.getValue());
        l.add(getEntry().getUuid().toString());
        if (entry.getValidity().isUsable()) {
            l.addAll(entry.getProvider().getSearchableTerms(entry.getStore()));
            l.add(AppI18n.get(entry.getProvider().getId() + ".displayName"));
        }
        l.add(information.getValue() != null ? information.getValue().toJoinedString() : null);
        l.add(summary.getValue());
        l.add(notes.getValue());
        l.addAll(tags);
        return filter.matches(l);
    }

    public Property<String> nameProperty() {
        return name;
    }
}
