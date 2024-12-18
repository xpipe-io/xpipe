package io.xpipe.ext.base.service;

import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.Validators;
import io.xpipe.core.store.DataStore;
import io.xpipe.ext.base.GroupStore;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@SuperBuilder
public abstract class AbstractServiceGroupStore<T extends DataStore> implements DataStore, GroupStore<T> {

    DataStoreEntryRef<T> parent;

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(parent);
    }
}
