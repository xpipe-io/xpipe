package io.xpipe.app.comp.store;

import io.xpipe.app.comp.store.GuiDsStoreCreator;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreColor;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.ThreadHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class StoreEntryWrapper {

    private final Property<String> name;
    private final DataStoreEntry entry;
    private final Property<Instant> lastAccess;
    private final BooleanProperty disabled = new SimpleBooleanProperty();
    private final BooleanProperty inRefresh = new SimpleBooleanProperty();
    private final BooleanProperty observing = new SimpleBooleanProperty();
    private final Property<DataStoreEntry.Validity> validity = new SimpleObjectProperty<>();
    private final Map<ActionProvider, BooleanProperty> actionProviders;
    private final Property<ActionProvider.DefaultDataStoreCallSite<?>> defaultActionProvider;
    private final BooleanProperty deletable = new SimpleBooleanProperty();
    private final BooleanProperty expanded = new SimpleBooleanProperty();
    private final Property<Object> persistentState = new SimpleObjectProperty<>();
    private final MapProperty<String, Object> cache = new SimpleMapProperty<>(FXCollections.observableHashMap());
    private final Property<DataStoreColor> color = new SimpleObjectProperty<>();

    public StoreEntryWrapper(DataStoreEntry entry) {
        this.entry = entry;
        this.name = new SimpleStringProperty(entry.getName());
        this.lastAccess = new SimpleObjectProperty<>(entry.getLastAccess().minus(Duration.ofMillis(500)));
        this.actionProviders = new LinkedHashMap<>();
        ActionProvider.ALL.stream()
                .filter(dataStoreActionProvider -> {
                    return !entry.isDisabled()
                            && dataStoreActionProvider.getDataStoreCallSite() != null
                            && dataStoreActionProvider
                                    .getDataStoreCallSite()
                                    .getApplicableClass()
                                    .isAssignableFrom(entry.getStore().getClass());
                })
                .sorted(Comparator.comparing(
                        actionProvider -> actionProvider.getDataStoreCallSite().isSystemAction()))
                .forEach(dataStoreActionProvider -> {
                    actionProviders.put(dataStoreActionProvider, new SimpleBooleanProperty(true));
                });
        this.defaultActionProvider = new SimpleObjectProperty<>();
        setupListeners();
        update();
    }

    public void moveTo(DataStoreCategory category) {
        ThreadHelper.runAsync(() -> {
            DataStorage.get().updateCategory(entry, category);
        });
    }

    public boolean isInStorage() {
        return DataStorage.get().getStoreEntries().contains(entry);
    }

    public void editDialog() {
        GuiDsStoreCreator.showEdit(entry);
    }

    public void delete() {
        ThreadHelper.runAsync(() -> {
            DataStorage.get().deleteChildren(this.entry);
            DataStorage.get().deleteStoreEntry(this.entry);
        });
    }

    public ObservableValue<String> summary() {
        return PlatformThread.sync(Bindings.createStringBinding(
                () -> {
                    if (!validity.getValue().isUsable()) {
                        return null;
                    }

                    try {
                        return entry.getProvider().summaryString(this);
                    } catch (Exception ex) {
                        ErrorEvent.fromThrowable(ex).handle();
                        return null;
                    }
                },
                validity,
                persistentState));
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
    }

    public void update() {
        //        var cat = StoreViewState.get().getCategories().stream()
        //                .filter(storeCategoryWrapper ->
        //                        Objects.equals(storeCategoryWrapper.getCategory().getUuid(), entry.getCategoryUuid()))
        //                .findFirst();
        //        category.setValue(cat.orElseThrow());

        // Avoid reupdating name when changed from the name property!
        if (!entry.getName().equals(name.getValue())) {
            name.setValue(entry.getName());
        }

        lastAccess.setValue(entry.getLastAccess());
        disabled.setValue(entry.isDisabled());
        validity.setValue(entry.getValidity());
        expanded.setValue(entry.isExpanded());
        observing.setValue(entry.isObserving());
        persistentState.setValue(entry.getStorePersistentState());
        cache.putAll(entry.getStoreCache());
        color.setValue(entry.getColor());

        inRefresh.setValue(entry.isInRefresh());
        deletable.setValue(entry.getConfiguration().isDeletable()
                || AppPrefs.get().developerDisableGuiRestrictions().getValue());

        actionProviders.keySet().forEach(dataStoreActionProvider -> {
            if (!isInStorage()) {
                actionProviders.get(dataStoreActionProvider).set(false);
                defaultActionProvider.setValue(null);
                return;
            }

            if (!entry.getValidity().isUsable()
                    && !dataStoreActionProvider
                            .getDataStoreCallSite()
                            .activeType()
                            .equals(ActionProvider.DataStoreCallSite.ActiveType.ALWAYS_ENABLE)) {
                actionProviders.get(dataStoreActionProvider).set(false);
                return;
            }

            var defaultProvider = ActionProvider.ALL.stream()
                    .filter(e -> e.getDefaultDataStoreCallSite() != null
                            && e.getDefaultDataStoreCallSite()
                                    .getApplicableClass()
                                    .isAssignableFrom(entry.getStore().getClass())
                            && e.getDefaultDataStoreCallSite()
                                    .isApplicable(entry.ref()))
                    .findFirst()
                    .map(ActionProvider::getDefaultDataStoreCallSite)
                    .orElse(null);
            this.defaultActionProvider.setValue(defaultProvider);

            try {
                actionProviders
                        .get(dataStoreActionProvider)
                        .set(dataStoreActionProvider
                                        .getDataStoreCallSite()
                                        .getApplicableClass()
                                        .isAssignableFrom(entry.getStore().getClass())
                                && dataStoreActionProvider
                                        .getDataStoreCallSite()
                                        .isApplicable(entry.ref()));
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).handle();
                actionProviders.get(dataStoreActionProvider).set(false);
            }
        });
    }

    public void refreshChildren() {
        var hasChildren = DataStorage.get().refreshChildren(entry);
        PlatformThread.runLaterIfNeeded(() -> {
            expanded.set(hasChildren);
        });
    }

    public void executeDefaultAction() throws Exception {
        if (getEntry().getValidity() == DataStoreEntry.Validity.INCOMPLETE) {
            editDialog();
            return;
        }

        var found = getDefaultActionProvider().getValue();
        entry.updateLastUsed();
        if (found != null) {
            found.createAction(entry.ref()).execute();
        } else {
            entry.setExpanded(!entry.isExpanded());
        }
    }

    public void toggleExpanded() {
        this.expanded.set(!expanded.getValue());
    }

    public boolean shouldShow(String filter) {
        return filter == null || nameProperty().getValue().toLowerCase().contains(filter.toLowerCase());
    }

    public Property<String> nameProperty() {
        return name;
    }

    public BooleanProperty disabledProperty() {
        return disabled;
    }
}
