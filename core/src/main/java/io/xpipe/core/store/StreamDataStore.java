package io.xpipe.core.store;

import lombok.NonNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * A data store that can be accessed using InputStreams and/or OutputStreams.
 * These streams must support mark/reset.
 */
public interface StreamDataStore extends DataStore {

    static Optional<StreamDataStore> fromString(@NonNull String s) {
        try {
            var path = Path.of(s);
            return Optional.of(new LocalFileDataStore(path));
        } catch (InvalidPathException ignored) {
        }

        try {
            var path = new URL(s);
        } catch (MalformedURLException ignored) {
        }

        return Optional.empty();
    }

    /**
     * Opens an input stream. This input stream does not necessarily have to be a new instance.
     */
    InputStream openInput() throws Exception;

    /**
     * Opens an output stream. This output stream does not necessarily have to be a new instance.
     */
    OutputStream openOutput() throws Exception;

    boolean exists();
}
