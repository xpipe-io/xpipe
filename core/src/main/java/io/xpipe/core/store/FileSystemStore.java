package io.xpipe.core.store;

import java.io.InputStream;
import java.io.OutputStream;

public interface FileSystemStore extends DataStore {

    InputStream openInput(String file) throws Exception;

    OutputStream openOutput(String file) throws Exception;

    public boolean exists(String file) throws Exception;

    boolean mkdirs(String file) throws Exception;
}
