package io.xpipe.app.ext;

import io.xpipe.app.storage.DataStoreEntryRef;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface DataStore {

    List<DataStoreEntryRef<?>> getDependencies();

    default boolean isComplete() {
        try {
            checkComplete();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    default void checkComplete() throws Throwable {}

    /**
     * Casts this instance to the required type without checking whether a cast is possible.
     */
    @SuppressWarnings("unchecked")
    default <DS extends DataStore> DS asNeeded() {
        return (DS) this;
    }
}
