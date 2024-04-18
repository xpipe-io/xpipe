package io.xpipe.ext.base.desktop;

import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.Validators;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.JacksonizedValue;
import io.xpipe.ext.base.SelfReferentialStore;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Getter
@SuperBuilder
@Jacksonized
@JsonTypeName("desktopCommand")
public class DesktopCommandStore extends JacksonizedValue implements DataStore, SelfReferentialStore {

    private final DataStoreEntryRef<DesktopEnvironmentStore> environment;
    private final String script;

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(environment);
        Validators.isType(environment, DesktopEnvironmentStore.class);
        Validators.nonNull(script);
    }
}
