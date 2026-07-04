package io.xpipe.ext.base.identity;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.ext.*;
import io.xpipe.app.hub.comp.StoreSection;
import io.xpipe.app.hub.comp.SystemStateComp;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.DocumentationLink;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import java.util.List;

public abstract class IdentityStoreProvider implements DataStoreProvider {

    @Override
    public DataStoreEntry getDisplayParent(DataStoreEntry store) {
        return MultiIdentityStore.getExclusiveHolder(store.ref()).map(DataStoreEntryRef::get).orElse(null);
    }

    @Override
    public DocumentationLink getHelpLink() {
        return DocumentationLink.IDENTITIES;
    }

    @Override
    public List<String> getSearchableTerms(DataStore store) {
        IdentityStore s = store.asNeeded();
        var name = s.getUsername().getFixedUsername();
        return name.isPresent() ? List.of(name.get()) : List.of();
    }

    @Override
    public BaseRegionBuilder<?, ?> stateDisplay(StoreSection section) {
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
        return Bindings.createStringBinding(
                () -> {
                    var st = (IdentityStore) section.getWrapper().getStore().getValue();
                    return IdentitySummary.createSummary(st);
                },
                section.getWrapper().getPersistentState());
    }
}
