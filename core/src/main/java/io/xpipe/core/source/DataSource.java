package io.xpipe.core.source;

import io.xpipe.core.store.DataStore;

import java.util.Optional;

public interface DataSource<DS extends DataStore> {

    default Optional<String> determineDefaultName(DS store) {
        return Optional.empty();
    }

    DataSourceType getType();
}
