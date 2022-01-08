package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.Optional;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface DataStore {

    @SuppressWarnings("unchecked")
    default <DS extends DataStore> DS asNeeded() {
        return (DS) this;
    }

    default Optional<String> determineDefaultName() {
        return Optional.empty();
    }

    default Optional<Instant> getLastModified() {
        return Optional.empty();
    }
}
