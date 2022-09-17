package io.xpipe.core.store;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A data store that can be accessed using InputStreams and/or OutputStreams.
 */
public interface StreamDataStore extends DataStore {


    /**
     * Opens an input stream that can be used to read its data.
     */
    default InputStream openInput() throws Exception {
        throw new UnsupportedOperationException("Can't open store input");
    }

    /**
     * Opens an input stream that is guaranteed to be buffered.
     */
    default InputStream openBufferedInput() throws Exception {
        var in = openInput();
        if (in.markSupported()) {
            return in;
        }

        return new BufferedInputStream(in);
    }

    /**
     * Opens an output stream that can be used to write data.
     */
    default OutputStream openOutput() throws Exception {
        throw new UnsupportedOperationException("Can't open store output");
    }
}
