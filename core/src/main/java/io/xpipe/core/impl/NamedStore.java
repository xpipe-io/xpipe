package io.xpipe.core.impl;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.store.DataStore;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.Optional;

/**
 * A store that refers to another store in the XPipe storage.
 * The referenced store has to be resolved by the caller manually, as this class does not act as a resolver.
 */
@JsonTypeName("named")
@SuperBuilder
@Jacksonized
public final class NamedStore implements DataStore {

    @Getter
    private final String name;

    @Override
    public void validate() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean delete() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public <DS extends DataStore> DS asNeeded() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<String> determineDefaultName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Instant> determineLastModified() {
        throw new UnsupportedOperationException();
    }
}
