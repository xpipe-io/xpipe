package io.xpipe.core.source;

import io.xpipe.core.store.DataStore;

import java.util.HashMap;
import java.util.Map;

public abstract class CollectionDataSourceDescriptor<DS extends DataStore> extends DataSourceDescriptor<DS> {

    private final Map<String, String> preferredProviders;

    public CollectionDataSourceDescriptor(DS store) {
        super(store);
        this.preferredProviders = new HashMap<>();
    }

    public CollectionDataSourceDescriptor<DS> annotate(String file, String provider) {
        preferredProviders.put(file, provider);
        return this;
    }

    public CollectionDataSourceDescriptor<DS> annotate(Map<String, String> preferredProviders) {
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
