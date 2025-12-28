package io.xpipe.ext.base.identity;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.ext.*;
import io.xpipe.app.hub.comp.StoreEntryWrapper;
import io.xpipe.app.hub.comp.StoreSection;
import io.xpipe.app.hub.comp.SystemStateComp;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import java.util.List;

public abstract class IdentityStoreProvider implements DataStoreProvider {

    @Override
    public List<String> getSearchableTerms(DataStore store) {
        IdentityStore s = store.asNeeded();
        var name = s.getUsername().getFixedUsername();
        return name.isPresent() ? List.of(name.get()) : List.of();
    }

    @Override
    public Comp<?> stateDisplay(StoreEntryWrapper w) {
        return new SystemStateComp(new SimpleObjectProperty<>(SystemStateComp.State.SUCCESS));
    }

    @Override
    public DataStoreCreationCategory getCreationCategory() {
        return DataStoreCreationCategory.IDENTITY;
    }

    @Override
    public DataStoreUsageCategory getUsageCategory() {
        return DataStoreUsageCategory.IDENTITY;
    }

    @Override
    public ObservableValue<String> informationString(StoreSection section) {
        var st = (IdentityStore) section.getWrapper().getStore().getValue();
        return new SimpleStringProperty(IdentitySummary.createSummary(st));
    }
}
