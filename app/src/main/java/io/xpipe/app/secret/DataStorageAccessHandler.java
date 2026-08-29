package io.xpipe.app.secret;

import io.xpipe.app.ext.AuthModuleProvider;
import io.xpipe.app.prefs.DataStorageAccessType;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface DataStorageAccessHandler {

    static DataStorageAccessHandler getInstance() {
        return AuthModuleProvider.get().getStorageAccessHandler();
    }

    boolean init() throws IOException;

    void save();

    void login();

    boolean isAccessRestricted();

    boolean isAccessible();

    DataStorageAccessType getType();

    Optional<EncryptionPrincipal> getEncryptionPrincipal(UUID uuid);

    Optional<EncryptionPrincipal> getEncryptionPrincipal(String name);

    Set<EncryptionPrincipal> getCurrentEncryptionPrincipals();

    Set<EncryptionPrincipal> getAllEncryptionPrincipals();

    EncryptionPrincipal getEncryptAllPrincipal();

    EncryptionPrincipal getFallbackPrincipal();
}
