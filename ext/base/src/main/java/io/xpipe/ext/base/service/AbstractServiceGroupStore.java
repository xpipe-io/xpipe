package io.xpipe.ext.base.service;

import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.Validators;
import io.xpipe.core.store.DataStore;
import io.xpipe.ext.base.GroupStore;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@SuperBuilder
@EqualsAndHashCode
@ToString
public abstract class AbstractServiceGroupStore<T extends DataStore> implements DataStore, GroupStore<T> {

    DataStoreEntryRef<? extends T> parent;

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(parent);
        // Essentially a null check
        Validators.isType(parent, DataStore.class);
        parent.checkComplete();
    }
}
