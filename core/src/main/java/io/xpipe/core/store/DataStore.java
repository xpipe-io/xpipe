package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.xpipe.core.util.DataStateProvider;

/**
 * A data store represents some form of a location where data is stored, e.g. a file or a database.
 * It does not contain any information on what data is stored,
 * how the data is stored inside, or what part of the data store makes up the actual data source.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface DataStore {

    default boolean isInStorage() {
        return DataStateProvider.get().isInStorage(this);
    }

    default boolean isComplete() {
        try {
            checkComplete();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    default void checkComplete() throws Exception {}

    /**
     * Casts this instance to the required type without checking whether a cast is possible.
     */
    @SuppressWarnings("unchecked")
    default <DS extends DataStore> DS asNeeded() {
        return (DS) this;
    }
}
