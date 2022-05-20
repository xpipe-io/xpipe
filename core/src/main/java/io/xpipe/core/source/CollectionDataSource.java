package io.xpipe.core.source;

import io.xpipe.core.store.DataStore;

import java.util.HashMap;
import java.util.Map;

public abstract class CollectionDataSource<DS extends DataStore> extends DataSource<DS> {

    private final Map<String, String> preferredProviders;

    public CollectionDataSource(DS store) {
        super(store);
        this.preferredProviders = new HashMap<>();
    }

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

    public final CollectionWriteConnection openWriteConnection() throws Exception {
        var con = newWriteConnection();
        con.init();
        return con;
    }

    protected abstract CollectionWriteConnection newWriteConnection();

    protected abstract CollectionReadConnection newReadConnection();
}
