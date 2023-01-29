package io.xpipe.ext.base.apps;

import io.xpipe.app.comp.source.DsStorageTargetComp;
import io.xpipe.app.comp.storage.collection.SourceCollectionViewState;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.storage.DataSourceCollection;
import io.xpipe.app.storage.DataSourceEntry;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.extension.DataSourceTarget;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.impl.VerticalComp;
import io.xpipe.extension.fxcomps.util.SimpleChangeListener;
import io.xpipe.extension.util.SimpleValidator;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import java.util.List;
import java.util.UUID;

public class SaveSourceTarget implements DataSourceTarget {

    private final Property<Boolean> nameValid = new SimpleObjectProperty<>(true);

    @Override
    public String getId() {
        return "base.saveSource";
    }

    @Override
    public InstructionsDisplay createRetrievalInstructions(DataSource<?> source, ObservableValue<DataSourceId> id) {
        var storageGroup = new SimpleObjectProperty<DataSourceCollection>();
        var dataSourceEntry = new SimpleObjectProperty<DataSourceEntry>();
        SimpleChangeListener.apply(id, val -> {
            if (val == null) {
                storageGroup.set(null);
                dataSourceEntry.set(null);
                return;
            }

            storageGroup.set(
                    SourceCollectionViewState.get().getSelectedGroup() != null
                            ? SourceCollectionViewState.get().getSelectedGroup().getCollection()
                            : null);
            dataSourceEntry.set(DataSourceEntry.createNew(UUID.randomUUID(), val.getEntryName(), source));
        });

        var storeSettings = new VerticalComp(List.of(new DsStorageTargetComp(dataSourceEntry, storageGroup, nameValid)))
                .styleClass("store-options");
        storeSettings.apply(r -> AppFont.medium(r.get()));
        var validator = new SimpleValidator();
        return new InstructionsDisplay(
                storeSettings.createRegion(),
                new Runnable() {
                    @Override
                    public void run() {
                        var e = dataSourceEntry.getValue();
                        var c = storageGroup.getValue();
                        if (!c.getEntries().contains(e)) {
                            DataStorage.get().add(e, c);
                        }
                    }
                },
                validator);
    }

    @Override
    public ObservableValue<String> getName() {
        return I18n.observable("base.saveSource");
    }

    @Override
    public Category getCategory() {
        return Category.OTHER;
    }

    @Override
    public AccessType getAccessType() {
        return AccessType.ACTIVE;
    }

    @Override
    public String getSetupGuideURL() {
        return null;
    }

    @Override
    public String getGraphicIcon() {
        return "mdi2c-content-save";
    }
}
