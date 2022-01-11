package io.xpipe.core.source;

import io.xpipe.core.store.DataStore;

import java.util.Optional;

/**
 * Represents a formal description on what exactly makes up the
 * actual data source and how to access/locate it for a given data store.
 *
 * This instance is only valid in combination with its associated data store instance.
 */
public interface DataSourceDescriptor<DS extends DataStore> {

    /**
     * Determines on optional default name for this data store that is
     * used when determining a suitable default name for a data source.
     */
    default Optional<String> determineDefaultName(DS store) {
        return Optional.empty();
    }

    /**
     * Determines the data source info.
     * This is usually called only once on data source
     * creation as this process might be expensive.
     */
    DataSourceInfo determineInfo(DS store) throws Exception;

    /**
     * Returns the general data source type.
     */
    DataSourceType getType();
}
