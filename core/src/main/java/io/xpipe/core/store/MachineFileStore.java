package io.xpipe.core.store;

import java.io.InputStream;
import java.io.OutputStream;

public interface MachineFileStore extends DataStore {

    default boolean isLocal(){
        return false;
    }


    InputStream openInput(String file) throws Exception;

    OutputStream openOutput(String file) throws Exception;

    public boolean exists(String file) throws Exception;
}
