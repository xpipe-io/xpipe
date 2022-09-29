package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * A store whose contents are stored in memory.
 */
@Value
@JsonTypeName("inMemory")
public class InMemoryStore implements StreamDataStore {

    @NonFinal
    byte[] value;

    public InMemoryStore() {
        this.value = new byte[0];
    }

    @JsonCreator
    public InMemoryStore(byte[] value) {
        this.value = value;
    }

    @Override
    public boolean isContentExclusivelyAccessible() {
        return true;
    }

    @Override
    public InputStream openInput() throws Exception {
        return new ByteArrayInputStream(value);
    }

    @Override
    public OutputStream openOutput() throws Exception {
        return new ByteArrayOutputStream(){
            @Override
            public void close() throws IOException {
                super.close();
                InMemoryStore.this.value = this.toByteArray();
            }
        };
    }

    public String toString() {
        return new String(value, StandardCharsets.UTF_8);
    }
}
