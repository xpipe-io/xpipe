package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;

import java.time.Instant;
import java.util.Optional;

@JsonTypeName("named")
public final class NamedStore implements DataStore {

    @Getter
    private final String name;

    @JsonCreator
    public NamedStore(String name) {
        this.name = name;
    }

    @Override
    public void validate() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean delete() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toDisplay() {
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
