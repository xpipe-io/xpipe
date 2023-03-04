package io.xpipe.app.comp.storage.source;

import io.xpipe.app.comp.source.GuiDsCreatorMultiStep;
import io.xpipe.app.comp.storage.StorageFilter;
import io.xpipe.app.comp.storage.collection.SourceCollectionViewState;
import io.xpipe.app.comp.storage.collection.SourceCollectionWrapper;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataSourceEntry;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.StorageElement;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.store.DataFlow;
import javafx.beans.property.*;
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
    Property<DataFlow> dataFlow = new SimpleObjectProperty<>();
    ObjectProperty<DataSourceEntry.State> state = new SimpleObjectProperty<>();
    BooleanProperty loading = new SimpleBooleanProperty();
    List<ActionProvider> actionProviders = new ArrayList<>();

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

        DataStorage.get().deleteSourceEntry(entry);
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
                        : AppI18n.get("failedToLoad"));
        loading.setValue(entry.getState() == null || entry.getState() == DataSourceEntry.State.VALIDATING);

        actionProviders.clear();
        actionProviders.addAll(ActionProvider.ALL.stream()
                .filter(p -> {
                    try {
                        if (!entry.getState().isUsable()) {
                            return false;
                        }

                        var c = p.getDataSourceCallSite();
                        if (c == null) {
                            return false;
                        }

                        return c.getApplicableClass()
                                        .isAssignableFrom(entry.getSource().getClass())
                                && c.isApplicable(entry.getSource().asNeeded());
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
