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
    InputStream openInput() throws Exception;

    /**
     * Opens an output stream. This output stream does not necessarily have to be a new instance.
     */
    OutputStream openOutput() throws Exception;
}
