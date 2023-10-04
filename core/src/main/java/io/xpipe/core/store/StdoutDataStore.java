package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.core.util.JacksonizedValue;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.io.FilterOutputStream;
import java.io.OutputStream;

@JsonTypeName("stdout")
@SuperBuilder
@Jacksonized
public class StdoutDataStore extends JacksonizedValue implements StreamDataStore {

    @Override
    public boolean canOpen() {
        return false;
    }

    @Override
    public boolean isContentExclusivelyAccessible() {
        return true;
    }

    @Override
    public OutputStream openOutput() {
        // Create an output stream that will write to standard out but will not close it
        return new FilterOutputStream(System.out) {
            @Override
            public void close() {}
        };
    }
}
