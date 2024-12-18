package io.xpipe.ext.base.desktop;

import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.Validators;
import io.xpipe.core.store.DataStore;
import io.xpipe.ext.base.SelfReferentialStore;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Value
@SuperBuilder
@Jacksonized
@JsonTypeName("desktopCommand")
public class DesktopCommandStore implements DataStore, SelfReferentialStore {

    private final DataStoreEntryRef<DesktopEnvironmentStore> environment;
    private final String script;

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(environment);
        Validators.isType(environment, DesktopEnvironmentStore.class);
        environment.checkComplete();
        Validators.nonNull(script);
    }
}
