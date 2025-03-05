package io.xpipe.ext.base.service;

import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.Validators;
import io.xpipe.core.process.ShellScript;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.SingletonSessionStore;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@SuperBuilder
@Value
@JsonTypeName("serviceControl")
@Jacksonized
public class ServiceControlStore implements SingletonSessionStore<ServiceControlSession>, DataStore {

    DataStoreEntryRef<ShellStore> host;
    ShellScript startScript;
    ShellScript stopScript;
    ShellScript statusScript;
    boolean elevated;

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(getHost());
        Validators.nonNull(getStartScript());
        Validators.nonNull(getStopScript());
        Validators.nonNull(getStatusScript());
    }

    @Override
    public ServiceControlSession newSession() throws Exception {
        return new ServiceControlSession(running -> {}, this);
    }

    @Override
    public Class<?> getSessionClass() {
        return ServiceControlSession.class;
    }
}
