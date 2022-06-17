package io.xpipe.core.store;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * A data store that can be accessed using InputStreams and/or OutputStreams.
 * These streams must support mark/reset.
 */
public interface StreamDataStore extends DataStore {

    /**
     * Opens an input stream. This input stream does not necessarily have to be a new instance.
     */
    default InputStream openInput() throws Exception {
        throw new UnsupportedOperationException("Can't open store input");
    }

    /**
     * Opens an output stream. This output stream does not necessarily have to be a new instance.
     */
    default OutputStream openOutput() throws Exception {
        throw new UnsupportedOperationException("Can't open store output");
    }

    default OutputStream openAppendingOutput() throws Exception {
        throw new UnsupportedOperationException("Can't open store output");
    }

    default boolean exists() {
        return false;
    }

    default boolean persistent() {
        return false;
    }
}
