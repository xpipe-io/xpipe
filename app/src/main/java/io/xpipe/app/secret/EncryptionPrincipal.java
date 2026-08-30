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

            @Override
            public boolean isSubRestricted() {
                return true;
            }
        };
    }

    UUID getUuid();

    String getName();

    boolean isAccessible();

    SecretKey getSecretKey();

    boolean isSubRestricted();
}
