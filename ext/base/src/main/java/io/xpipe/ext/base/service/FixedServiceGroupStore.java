package io.xpipe.ext.base.service;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.FixedHierarchyStore;
import io.xpipe.app.util.Validators;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FixedChildStore;
import io.xpipe.core.util.JacksonizedValue;
import io.xpipe.ext.base.GroupStore;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@SuperBuilder
@Jacksonized
@JsonTypeName("fixedServiceGroup")
public class FixedServiceGroupStore extends JacksonizedValue implements DataStore, GroupStore<FixedServiceCreatorStore>, FixedHierarchyStore {

    DataStoreEntryRef<? extends FixedServiceCreatorStore> parent;

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(parent);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<? extends DataStoreEntryRef<? extends FixedChildStore>> listChildren(DataStoreEntry self) throws Exception {
        return (List<? extends DataStoreEntryRef<? extends FixedChildStore>>) parent.getStore().createFixedServices();
    }
}
