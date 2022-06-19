package io.xpipe.core.store;

import java.io.InputStream;
import java.io.OutputStream;

public interface MachineStore extends DataStore {

    static MachineStore local() {
         return new LocalMachineStore();
    }

    InputStream openInput(String file) throws Exception;

    OutputStream openOutput(String file) throws Exception;

    public boolean exists(String file);
}
