package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Value;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * A store whose contents are stored in memory.
 */
@Value
@JsonTypeName("inMemory")
public class InMemoryStore implements StreamDataStore {

    byte[] value;

    @JsonCreator
    public InMemoryStore(byte[] value) {
        this.value = value;
    }

    @Override
    public boolean isLocalToApplication() {
        return true;
    }

    @Override
    public InputStream openInput() throws Exception {
        return new ByteArrayInputStream(value);
    }

    @Override
    public String toSummaryString() {
        return "inMemory";
    }
}
