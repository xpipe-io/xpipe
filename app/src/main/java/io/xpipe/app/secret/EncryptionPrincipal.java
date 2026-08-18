package io.xpipe.app.secret;

import java.util.UUID;
import javax.crypto.SecretKey;

public interface EncryptionPrincipal {

    static EncryptionPrincipal inaccessible() {
        var dummyId = UUID.fromString("73e2d533-6fa4-497e-87a8-24da4fdf4d63");
        return new EncryptionPrincipal() {

            @Override
            public UUID getUuid() {
                return dummyId;
            }

            @Override
            public String getName() {
                return "inaccessible";
            }

            @Override
            public boolean isAccessible() {
                return false;
            }

            @Override
            public SecretKey getSecretKey() {
                throw new UnsupportedOperationException();
            }
        };
    }

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
