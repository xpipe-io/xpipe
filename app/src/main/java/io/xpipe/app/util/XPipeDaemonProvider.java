package io.xpipe.app.util;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceReference;
import io.xpipe.core.store.DataStore;

import java.util.Optional;

public class XPipeDaemonProvider implements XPipeDaemon {

    @Override
    public Optional<DataSource<?>> getSource(String id) {
        var sourceId = DataSourceId.fromString(id);
        return DataStorage.get()
                .getDataSource(DataSourceReference.id(sourceId))
                .map(dataSourceEntry -> dataSourceEntry.getSource());
    }

    @Override
    public Optional<String> getStoreName(DataStore store) {
        if (store == null) {
            return Optional.empty();
        }

        return DataStorage.get().getStoreEntries().stream()
                .filter(entry -> !entry.isDisabled() && entry.getStore().equals(store))
                .findFirst()
                .map(entry -> entry.getName());
    }

    @Override
    public Optional<String> getSourceId(DataSource<?> source) {
        var entry = DataStorage.get().getSourceEntry(source);
        return entry.map(
                dataSourceEntry -> DataStorage.get().getId(dataSourceEntry).toString());
    }
}
