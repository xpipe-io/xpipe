package io.xpipe.app.ext;

public interface ValidatableStore extends DataStore {

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
    void validate() throws Exception;
}
