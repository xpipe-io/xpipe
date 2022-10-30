package io.xpipe.core.source;

import io.xpipe.core.store.DataStore;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@SuperBuilder
public abstract class CollectionDataSource<DS extends DataStore> extends DataSource<DS> {

    @Singular
    private final Map<String, String> preferredProviders;

    public CollectionDataSource<DS> annotate(String file, String provider) {
        preferredProviders.put(file, provider);
        return this;
    }

    public CollectionDataSource<DS> annotate(Map<String, String> preferredProviders) {
        this.preferredProviders.putAll(preferredProviders);
        return this;
    }

    @Override
    public final DataSourceInfo determineInfo() throws Exception {
        try (var con = openReadConnection()) {
            var c = (int) con.listEntries().count();
            return new DataSourceInfo.Collection(c);
        }
    }

    public final CollectionReadConnection openReadConnection() throws Exception {
        var con = newReadConnection();
        con.init();
        return con;
    }

    public final CollectionWriteConnection openWriteConnection(WriteMode mode) throws Exception {
        var con = newWriteConnection(mode);
        if (con == null) {
            throw new UnsupportedOperationException(mode.getId());
        }

        con.init();
        return con;
    }

    protected abstract CollectionWriteConnection newWriteConnection(WriteMode mode);

    protected abstract CollectionReadConnection newReadConnection();
}
