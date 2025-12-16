package io.xpipe.app.storage;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.prefs.VaultAuthentication;

import java.io.IOException;
import javax.crypto.SecretKey;

public interface DataStorageUserHandler {

    static DataStorageUserHandler getInstance() {
        return (DataStorageUserHandler) ProcessControlProvider.get().getStorageUserHandler();
    }

    int getUserCount();

    void init() throws IOException;

    void save();

    void login();

    SecretKey getEncryptionKey();

    Comp<?> createOverview();

    String getActiveUser();

    VaultAuthentication getVaultAuthenticationType();
}
