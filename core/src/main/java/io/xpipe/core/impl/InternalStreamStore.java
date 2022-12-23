package io.xpipe.core.impl;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.store.DataFlow;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.core.util.DataStateProvider;
import io.xpipe.core.util.JacksonizedValue;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@JsonTypeName("internalStream")
@SuperBuilder
@Jacksonized
@Getter
public class InternalStreamStore extends JacksonizedValue implements StreamDataStore {

    private final UUID uuid;

    public InternalStreamStore() {
        this.uuid = UUID.randomUUID();
    }

    @Override
    public DataFlow getFlow() {
        return DataFlow.INPUT_OUTPUT;
    }

    @Override
    public Optional<String> determineDefaultName() {
        return Optional.of(uuid.toString());
    }

    private Path getFile() {
        return DataStateProvider.get().getInternalStreamStore(uuid);
    }

    @Override
    public Optional<Instant> determineLastModified() throws IOException {
        return Optional.of(Files.getLastModifiedTime(getFile()).toInstant());
    }

    @Override
    public InputStream openInput() throws Exception {
        return Files.newInputStream(getFile());
    }

    @Override
    public OutputStream openOutput() throws Exception {
        return Files.newOutputStream(getFile());
    }
}
