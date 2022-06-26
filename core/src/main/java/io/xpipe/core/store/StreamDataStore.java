package io.xpipe.core.store;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A data store that can be accessed using InputStreams and/or OutputStreams.
 * These streams must support mark/reset.
 */
public interface StreamDataStore extends DataStore {

    default boolean isLocalOnly() {
        return true;
    }

    /**
     * Opens an input stream. This input stream does not necessarily have to be a new instance.
     */
    default InputStream openInput() throws Exception {
        throw new UnsupportedOperationException("Can't open store input");
    }

    default InputStream openBufferedInput() throws Exception {
        var in = openInput();
        if (in.markSupported()) {
            return in;
        }

        return new BufferedInputStream(in);
    }

    /**
     * Opens an output stream. This output stream does not necessarily have to be a new instance.
     */
    default OutputStream openOutput() throws Exception {
        throw new UnsupportedOperationException("Can't open store output");
    }

    default boolean canOpen() {
        return true;
    }

    default boolean persistent() {
        return false;
    }
}
