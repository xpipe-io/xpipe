package io.xpipe.ext.base.service;

import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.Validators;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.JacksonizedValue;
import io.xpipe.ext.base.GroupStore;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@SuperBuilder
@Jacksonized
@JsonTypeName("serviceGroup")
public class ServiceGroupStore extends JacksonizedValue implements DataStore, GroupStore<DataStore> {

    DataStoreEntryRef<? extends DataStore> parent;

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(parent);
    }
}
