package io.xpipe.core.store;

public interface GroupStore extends DataStore {

    DataStore getParent();

    @Override
    default void validate() throws Exception {
        getParent().validate();
    }

    @Override
    default void checkComplete() throws Exception {
        getParent().checkComplete();
    }
}
