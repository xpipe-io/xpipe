package io.xpipe.core.impl;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.store.DataFlow;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.core.util.DataStateProvider;
import io.xpipe.core.util.JacksonizedValue;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private Path getFile() {
        return DataStateProvider.get().getInternalStreamStore(uuid);
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
