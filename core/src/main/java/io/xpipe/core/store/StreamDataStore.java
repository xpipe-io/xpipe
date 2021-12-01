package io.xpipe.core.store;

import java.io.InputStream;
import java.io.OutputStream;

public interface StreamDataStore extends DataStore {

    InputStream openInput() throws Exception;

    OutputStream openOutput() throws Exception;
}
