package io.xpipe.core.impl;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.core.util.JacksonizedValue;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

@JsonTypeName("stdout")
@SuperBuilder
@Jacksonized
public class StdoutDataStore extends JacksonizedValue implements StreamDataStore {

    @Override
    public boolean canOpen() throws Exception {
        return false;
    }

    @Override
    public boolean isContentExclusivelyAccessible() {
        return true;
    }

    @Override
    public OutputStream openOutput() throws Exception {
        // Create an output stream that will write to standard out but will not close it
        return new FilterOutputStream(System.out) {
            @Override
            public void close() throws IOException {
            }
        };
    }
}
