package io.xpipe.app.comp.storage.source;

import io.xpipe.app.comp.source.GuiDsCreatorMultiStep;
import io.xpipe.app.comp.storage.StorageFilter;
import io.xpipe.app.comp.storage.collection.SourceCollectionViewState;
import io.xpipe.app.comp.storage.collection.SourceCollectionWrapper;
import io.xpipe.app.storage.*;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.store.DataFlow;
import io.xpipe.extension.DataSourceActionProvider;
import io.xpipe.extension.DataStoreProviders;
import io.xpipe.extension.I18n;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.fxcomps.util.PlatformThread;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import lombok.Value;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Value
public class SourceEntryWrapper implements StorageFilter.Filterable {

    DataSourceEntry entry;
    StringProperty name = new SimpleStringProperty();
    BooleanProperty usable = new SimpleBooleanProperty();
    StringProperty information = new SimpleStringProperty();
    StringProperty storeSummary = new SimpleStringProperty();
    Property<Instant> lastUsed = new SimpleObjectProperty<>();
    Property<AccessMode> accessMode = new SimpleObjectProperty<>();
    Property<DataFlow> dataFlow = new SimpleObjectProperty<>();
    ObjectProperty<DataSourceEntry.State> state = new SimpleObjectProperty<>();
    BooleanProperty loading = new SimpleBooleanProperty();

    List<DataSourceActionProvider<?>> actionProviders = new ArrayList<>();
    ListProperty<ApplicationAccess> accesses = new SimpleListProperty<>(FXCollections.observableArrayList());

    public SourceEntryWrapper(DataSourceEntry entry) {
        this.entry = entry;
        entry.addListener(new StorageElement.Listener() {
            @Override
            public void onUpdate() {
                PlatformThread.runLaterIfNeeded(() -> {
                    update();
                });
            }
        });
        update();
        name.addListener((c, o, n) -> {
            if (!entry.getName().equals(n)) {
                entry.setName(n);
            }
        });
    }

    public void moveTo(SourceCollectionWrapper newGroup) {
        var old = SourceCollectionViewState.get().getGroup(this);
        old.getCollection().removeEntry(this.entry);
        newGroup.getCollection().addEntry(this.entry);
    }

    public void editDialog() {
        if (!DataStorage.get().getSourceEntries().contains(entry)) {
            return;
        }

        GuiDsCreatorMultiStep.showEdit(getEntry());
    }

    public void delete() {
        if (!DataStorage.get().getSourceEntries().contains(entry)) {
            return;
        }

        DataStorage.get().deleteEntry(entry);
    }

    private <T extends DataSource<?>> void update() {
        // Avoid reupdating name when changed from the name property!
        if (!entry.getName().equals(name.getValue())) {
            name.set(entry.getName());
        }

        lastUsed.setValue(entry.getLastUsed());
        state.setValue(entry.getState());
        usable.setValue(entry.getState().isUsable());
        dataFlow.setValue(entry.getSource() != null ? entry.getSource().getFlow() : null);
        storeSummary.setValue(
                entry.getState().isUsable()
                        ? DataStoreProviders.byStore(entry.getStore()).toSummaryString(entry.getStore(), 50)
                        : null);
        information.setValue(
                entry.getState() != DataSourceEntry.State.LOAD_FAILED
                        ? entry.getInformation() != null
                                ? entry.getInformation()
                                : entry.getProvider().getDisplayName()
                        : I18n.get("failedToLoad"));
        loading.setValue(entry.getState() == null || entry.getState() == DataSourceEntry.State.VALIDATING);

        actionProviders.clear();
        actionProviders.addAll(DataSourceActionProvider.ALL.stream()
                .filter(p -> {
                    try {
                        if (!entry.getState().isUsable()) {
                            return false;
                        }

                        return p.getApplicableClass()
                                        .isAssignableFrom(entry.getSource().getClass())
                                && p.isApplicable(entry.getSource().asNeeded());
                    } catch (Exception e) {
                        ErrorEvent.fromThrowable(e).handle();
                        return false;
                    }
                })
                .toList());
    }

    @Override
    public boolean shouldShow(String filter) {
        return getName().get().toLowerCase().contains(filter.toLowerCase());
    }
}
