package io.xpipe.extension;

import io.xpipe.core.store.DataStore;

import java.util.Optional;
import java.util.ServiceLoader;

public interface XPipeDaemon {

    static XPipeDaemon getInstance() {
        return ServiceLoader.load(XPipeDaemon.class).findFirst().orElseThrow();
    }

    Optional<DataStore> getNamedStore(String name);

    Optional<String> getStoreName(DataStore store);
}
