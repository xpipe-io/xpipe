package io.xpipe.app.util;

import io.xpipe.core.source.DataSource;
import io.xpipe.core.store.DataStore;

import java.util.Optional;
import java.util.ServiceLoader;

public interface XPipeDaemon {

    static XPipeDaemon getInstance() {
        return ServiceLoader.load(XPipeDaemon.class).findFirst().orElseThrow();
    }

    static Optional<XPipeDaemon> getInstanceIfPresent() {
        return ServiceLoader.load(XPipeDaemon.class).findFirst();
    }

    Optional<DataSource<?>> getSource(String id);

    Optional<String> getStoreName(DataStore store);

    Optional<String> getSourceId(DataSource<?> source);
}
