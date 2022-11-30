package io.xpipe.core.source;

import io.xpipe.core.store.DataStore;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@SuperBuilder
public abstract class CollectionDataSource<DS extends DataStore> extends DataSource<DS> {

    @Singular
    private final Map<String, String> preferredProviders;

    @Override
    public DataSourceType getType() {
        return DataSourceType.COLLECTION;
    }

    public CollectionDataSource<DS> annotate(String file, String provider) {
        preferredProviders.put(file, provider);
        return this;
    }

    public CollectionDataSource<DS> annotate(Map<String, String> preferredProviders) {
        this.preferredProviders.putAll(preferredProviders);
        return this;
    }

    public final CollectionReadConnection openReadConnection() throws Exception {
        if (!isComplete()) {
            throw new UnsupportedOperationException();
        }

        return newReadConnection();
    }

    public final CollectionWriteConnection openWriteConnection(WriteMode mode) throws Exception {
        var con = newWriteConnection(mode);
        if (con == null) {
            throw new UnsupportedOperationException(mode.getId());
        }

        return con;
    }

    protected abstract CollectionWriteConnection newWriteConnection(WriteMode mode);

    protected abstract CollectionReadConnection newReadConnection();
}
