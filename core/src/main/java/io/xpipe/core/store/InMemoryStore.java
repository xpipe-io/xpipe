package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.util.JacksonizedValue;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.io.*;

/**
 * A store whose contents are stored in memory.
 */
@JsonTypeName("inMemory")
@SuperBuilder
@Jacksonized
@Getter
public class InMemoryStore extends JacksonizedValue implements StreamDataStore {

    private byte[] value;


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
}
