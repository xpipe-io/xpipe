package io.xpipe.core.store;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Optional;

public class RemoteFileDataStore implements StreamDataStore {

    @Override
    public Optional<String> determineDefaultName() {
        return Optional.empty();
    }

    @Override
    public Optional<Instant> determineLastModified() {
        return Optional.empty();
    }

    @Override
    public InputStream openInput() throws Exception {
        return null;
    }

    @Override
    public OutputStream openOutput() throws Exception {
        return null;
    }

    @Override
    public boolean exists() {
        return false;
    }
}
