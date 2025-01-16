package io.xpipe.app.storage;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.ext.ProcessControlProvider;

import java.io.IOException;
import javax.crypto.SecretKey;

public interface DataStorageUserHandler {

    static DataStorageUserHandler getInstance() {
        return (DataStorageUserHandler) ProcessControlProvider.get().getStorageUserHandler();
    }

    void init() throws IOException;

    void save();

    void login();

    SecretKey getEncryptionKey();

    Comp<?> createOverview();

    String getActiveUser();
}
