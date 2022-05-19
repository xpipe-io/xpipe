package io.xpipe.core.source;

import io.xpipe.core.store.DataStore;

import java.util.Optional;

/**
 * Represents a formal description on what exactly makes up the
 * actual data source and how to access/locate it for a given data store.
 *
 * This instance is only valid in combination with its associated data store instance.
 */
public abstract class DataSourceDescriptor<DS extends DataStore> {

    protected DS store;

    public DataSourceDescriptor(DS store) {
        this.store = store;
    }

    public DataSourceDescriptor<DS> withStore(DS newStore) {
        return null;
    }

    /**
     * Casts this instance to the required type without checking whether a cast is possible.
     */
    @SuppressWarnings("unchecked")
    public final <DSD extends DataSourceDescriptor<?>> DSD asNeeded() {
        return (DSD) this;
    }

    /**
     * Determines on optional default name for this data store that is
     * used when determining a suitable default name for a data source.
     */
    public Optional<String> determineDefaultName() {
        return Optional.empty();
    }

    /**
     * Determines the data source info.
     * This is usually called only once on data source
     * creation as this process might be expensive.
     */
    public abstract DataSourceInfo determineInfo() throws Exception;

    public abstract DataSourceReadConnection openReadConnection() throws Exception;

    public abstract DataSourceConnection openWriteConnection() throws Exception;

    public DS getStore() {
        return store;
    }
}
