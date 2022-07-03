package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.xpipe.core.source.DataSource;

import java.time.Instant;
import java.util.Optional;

/**
 * A data store represents some form of a location where data is stored, e.g. a file or a database.
 * It does not contain any information on what data is stored,
 * how the data is stored inside, or what part of the data store makes up the actual data source.
 *
 * @see DataSource
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface DataStore {

    default boolean isComplete() {
        return true;
    }

    default void validate() throws Exception {
    }

    default boolean delete() throws Exception {
        return false;
    }

    default String toDisplay() {
        return null;
    }

    /**
     * Casts this instance to the required type without checking whether a cast is possible.
     */
    @SuppressWarnings("unchecked")
    default <DS extends DataStore> DS asNeeded() {
        return (DS) this;
    }

    /**
     * Determines on optional default name for this data store that is
     * used when determining a suitable default name for a data source.
     */
    default Optional<String> determineDefaultName() {
        return Optional.empty();
    }

    /**
     * Determines the last modified of this data store if this data store supports it.
     */
    default Optional<Instant> determineLastModified() {
        return Optional.empty();
    }
}
