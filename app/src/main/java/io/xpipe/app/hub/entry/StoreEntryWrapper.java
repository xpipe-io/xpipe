package io.xpipe.app.hub.entry;

import io.xpipe.app.action.*;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.HubMenuItemProvider;
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
import io.xpipe.app.util.HostHelper;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.ThreadHelper;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import lombok.Getter;
import org.int4.fx.values.util.Trigger;

import java.net.Inet4Address;
import java.time.Instant;
import java.util.*;

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
    private final Property<DataStoreAccessScope> accessScope = new SimpleObjectProperty<>();
    private final BooleanProperty accessScopeRestricted = new SimpleBooleanProperty();
    private final Property<String> shownName = new SimpleObjectProperty<>();
    private final Property<String> shownSummary = new SimpleObjectProperty<>();
    private final Property<String> shownDescription = new SimpleObjectProperty<>();
    private final Property<StoreEntryInformation> shownInformation = new SimpleObjectProperty<>();
    private final BooleanProperty template = new SimpleBooleanProperty();
    private final BooleanProperty pinToTop = new SimpleBooleanProperty();
    private final DoubleProperty orderIndex = new SimpleDoubleProperty();
    private final BooleanProperty effectiveBusy = new SimpleBooleanProperty();
    private final Property<StoreCategoryWrapper> lastInformationCategory = new SimpleObjectProperty<>();
    private final ObservableList<String> tags = FXCollections.observableArrayList();
    private final Property<Inet4Address> nameIpAddress = new SimpleObjectProperty<>();
    private final Trigger<Void> renameTrigger = Trigger.of();
    private boolean effectiveBusyProviderBound = false;
    private StoreEntryActionProviderSelectionState providerSelectionState;

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
                oldCat.updateHierarchy();
                newCat.updateHierarchy();
            });
        });
    }

    public boolean includeInConnectionCount() {
        return getEntry().getProvider() != null && getEntry().getProvider().includeInConnectionCount();
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
            nameIpAddress.setValue(HostHelper.parseIpv4(n).orElse(null));
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

        var newCat = StoreViewState.get().getCategories().getList().stream()
                .filter(storeCategoryWrapper ->
                        storeCategoryWrapper.getCategory().getUuid().equals(entry.getCategoryUuid()))
                .findFirst();

        // We received an update after moving/deleting a category
        if (newCat.isEmpty()) {
            return;
        }

        // Avoid reupdating name when changed from the name property!
        if (!entry.getName().equals(name.getValue())) {
            name.setValue(entry.getName());
            nameIpAddress.setValue(HostHelper.parseIpv4(entry.getName()).orElse(null));
        }

        shownName.setValue(
                AppPrefs.get().censorMode().get() ? "*".repeat(name.getValue().length()) : name.getValue());

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
        category.setValue(newCat.get());
        accessScope.setValue(entry.getAccessScope());
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

        var actionRelevantState = StoreEntryActionProviderSelectionState.of(entry);
        var actionProvidersNeedUpdate = !actionRelevantState.equals(providerSelectionState);
        if (actionProvidersNeedUpdate) {
            providerSelectionState = actionRelevantState;
            try {
                var selection = StoreEntryActionProviderSelection.of(entry, actionRelevantState);
                this.defaultActionProvider.setValue(selection.getDefaultProvider());
                if (!majorActionProviders.equals(selection.getMajorProviders())) {
                    majorActionProviders.setAll(selection.getMajorProviders());
                }
                if (!minorActionProviders.equals(selection.getMinorProviders())) {
                    minorActionProviders.setAll(selection.getMinorProviders());
                }
            } catch (Exception ex) {
                ErrorEventFactory.fromThrowable(ex).omit().handle();
            }
        }

        if (effectiveBusyProviderBound && !getValidity().getValue().isUsable()) {
            this.effectiveBusyProviderBound = false;
            this.effectiveBusy.unbind();
            this.effectiveBusy.bind(busy);
        } else if (!effectiveBusyProviderBound && getValidity().getValue().isUsable()) {
            this.effectiveBusyProviderBound = true;
            this.effectiveBusy.unbind();
            this.effectiveBusy.bind(busy.or(getEntry().getProvider().busy(this)));
        } else if (!effectiveBusyProviderBound && !effectiveBusy.isBound()) {
            effectiveBusy.bind(busy);
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
        return StoreEntryActionProviderSelection.showActionProvider(
                entry, StoreEntryActionProviderSelectionState.of(entry).isCompact(), p, major);
    }

    public boolean canToggleBreakOutCategory() {
        if (entry.getBreakOutCategory() != null) {
            return true;
        }

        var section = StoreViewState.get().getSectionForWrapper(this);
        var parentSection = StoreViewState.get().getParentSectionForWrapper(this);
        return (getStore().getValue() instanceof FixedHierarchyStore
                        || getStore().getValue() instanceof GroupStore<?>)
                && parentSection.isPresent()
                && section.isPresent() && !section.get().getAllChildren().getList().isEmpty();
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
}
