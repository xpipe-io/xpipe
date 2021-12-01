package io.xpipe.core.store;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Optional;

public class RemoteFileDataInput extends FileDataInput {

    @Override
    public Optional<String> determineDefaultName() {
        return Optional.empty();
    }

    @Override
    public Optional<Instant> getLastModified() {
        return Optional.empty();
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public LocalFileDataInput getLocal() {
        return null;
    }

    @Override
    public RemoteFileDataInput getRemote() {
        return null;
    }

    @Override
    public InputStream openInput() throws Exception {
        return null;
    }

    @Override
    public OutputStream openOutput() throws Exception {
        return null;
    }
}
