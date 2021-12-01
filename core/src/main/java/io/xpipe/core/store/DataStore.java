package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.Optional;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface DataStore {

    default Optional<String> determineDefaultName() {
        return Optional.empty();
    }

    default Optional<Instant> getLastModified() {
        return Optional.empty();
    }
}
