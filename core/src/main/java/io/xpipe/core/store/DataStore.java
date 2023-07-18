package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.xpipe.core.impl.StdinDataStore;
import io.xpipe.core.impl.StdoutDataStore;
import io.xpipe.core.source.DataSource;

/**
 * A data store represents some form of a location where data is stored, e.g. a file or a database.
 * It does not contain any information on what data is stored,
 * how the data is stored inside, or what part of the data store makes up the actual data source.
 *
 * @see DataSource
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface DataStore {

    default boolean shouldPersist() {
        return true;
    }

    default boolean shouldSave() {
        return true;
    }

    default boolean isComplete() {
        try {
            checkComplete();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    default DataFlow getFlow() {
        return DataFlow.INPUT_OUTPUT;
    }

    /**
     * Checks whether this store can be opened.
     * This can be not the case for example if the underlying store does not exist.
     */
    default boolean canOpen() throws Exception {
        return true;
    }

    /**
     * Indicates whether this data store can only be accessed by the current running application.
     * One example are standard in and standard out stores.
     *
     * @see StdinDataStore
     * @see StdoutDataStore
     */
    default boolean isContentExclusivelyAccessible() {
        return false;
    }

    /**
     * Performs a validation of this data store.
     * <p>
     * This validation can include one of multiple things:
     * - Sanity checks of individual properties
     * - Existence checks
     * - Connection checks
     * <p>
     * All in all, a successful execution of this method should almost guarantee
     * that the data store can be successfully accessed in the near future.
     * <p>
     * Note that some checks may take a long time, for example if a connection has to be validated.
     * The caller should therefore expect a runtime of multiple seconds when calling this method.
     *
     * @throws Exception if any part of the validation went wrong
     */
    default void validate() throws Exception {}

    default void initializeValidate() throws Exception {}

    default void finalizeValidate() throws Exception {}

    default void checkComplete() throws Exception {}

    default boolean delete() {
        return false;
    }

    /**
     * Casts this instance to the required type without checking whether a cast is possible.
     */
    @SuppressWarnings("unchecked")
    default <DS extends DataStore> DS asNeeded() {
        return (DS) this;
    }
}
