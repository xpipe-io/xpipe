package io.xpipe.app.secret;

import java.util.UUID;
import javax.crypto.SecretKey;

public interface EncryptionPrincipal {

    static EncryptionPrincipal getTargetPrincipal(EncryptionPrincipal principal) {
        if (!principal.isAccessible()) {
            return principal;
        }

        var handler = DataStorageAccessHandler.getInstance();
        var vaultPrincipal = handler.getFallbackPrincipal();
        var encryptPrincipal = handler.getEncryptAllPrincipal();

        var isVault = vaultPrincipal.equals(principal);
        var valid = handler.getAllEncryptionPrincipals().contains(principal);

        // We have a valid non-vault principal, we can keep that
        if (!isVault && valid) {
            return principal;
        }

        // A used principal got deleted, reencrypt with encryption key
        if (!valid) {
            return encryptPrincipal;
        }

        // We are using a vault key and have a custom encryption key
        // available, so use that one instead
        return encryptPrincipal;
    }

    UUID getUuid();

    String getName();

    boolean isAccessible();

    SecretKey getSecretKey();
}
